package com.exchange.platform.service;

import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.dto.SwapDTO;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.ProposalItem;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.ProposalRepository;
import com.exchange.platform.repository.SwapRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SwapService {

    private final SwapRepository swapRepository;
    private final ProposalRepository proposalRepository;
    private final ListingRepository listingRepository;
    private final com.exchange.platform.repository.UserRepository userRepository;
    private final ChatService chatService;
    private static final String SESSION_USER_ID = "userId";

    @Transactional(readOnly = true)
    public java.util.List<SwapDTO> listMine(HttpSession session, Integer page, Integer size, String sort) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();
        Pageable pageable = PageRequest.of(toPageIndex(page), toPageSize(size), parseSort(sort));
        Page<Swap> pg = swapRepository.findByAUserIdOrBUserId(userId, userId, pageable);
        return pg.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public SwapDTO getById(Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();
        Swap swap = swapRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) throw new ForbiddenException();
        return toDTO(swap);
    }

    public SwapDTO confirmReceived(Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) throw new ForbiddenException();

        // Idempotent: if already completed, just return current state
        boolean isA = swap.getAUserId().equals(userId);
        if (isA) {
            if (swap.getAConfirmedAt() == null) swap.setAConfirmedAt(java.time.LocalDateTime.now());
        } else {
            if (swap.getBConfirmedAt() == null) swap.setBConfirmedAt(java.time.LocalDateTime.now());
        }

        // If both confirmed, mark completed
        if (swap.getAConfirmedAt() != null && swap.getBConfirmedAt() != null) {
            if (swap.getStatus() != Swap.Status.COMPLETED) {
                swap.setStatus(Swap.Status.COMPLETED);
                if (swap.getCompletedAt() == null) swap.setCompletedAt(java.time.LocalDateTime.now());
                // Set chat room to read-only
                try {
                    chatService.setReadOnly(swap.getId());
                } catch (Exception e) {
                    // Log but don't fail the swap completion
                    System.err.println("Failed to set chat room read-only for swap " + swap.getId() + ": " + e.getMessage());
                }
            }
        }

        swap = swapRepository.save(swap);
        if (swap.getStatus() == Swap.Status.COMPLETED) {
            finalizeListingsForCompletedSwap(swap);
        }
        return toDTO(swap);
    }

    public SwapDTO toDTO(Swap s) {
        // Fetch proposal with items
        final List<ProposalDTO.ProposalItemDTO>[] proposerItemsArray = new List[]{Collections.emptyList()};
        final List<ProposalDTO.ProposalItemDTO>[] receiverItemsArray = new List[]{Collections.emptyList()};
        final Long[] proposerIdArray = new Long[]{null};
        final Long[] receiverIdArray = new Long[]{null};
        
        if (s.getProposalId() != null) {
            proposalRepository.findById(s.getProposalId()).ifPresent(proposal -> {
                proposerIdArray[0] = proposal.getProposerId();
                receiverIdArray[0] = proposal.getReceiverId();
                
                proposerItemsArray[0] = proposal.getProposalItems().stream()
                        .filter(item -> item.getSide() == ProposalItem.Side.OFFERED)
                        .map(item -> {
                            Listing listing = item.getListing();
                            String display = listing.getCardName() + " - " + listing.getArtistName();
                            String imageUrl = getFirstImageUrl(listing.getImagePaths());
                            return ProposalDTO.ProposalItemDTO.builder()
                                    .itemId(item.getId())
                                    .listingId(listing.getId())
                                    .listingDisplay(display)
                                    .imageUrl(imageUrl)
                                    .side("OFFERED")
                                    .build();
                        })
                        .collect(Collectors.toList());
                
                receiverItemsArray[0] = proposal.getProposalItems().stream()
                        .filter(item -> item.getSide() == ProposalItem.Side.REQUESTED)
                        .map(item -> {
                            Listing listing = item.getListing();
                            String display = listing.getCardName() + " - " + listing.getArtistName();
                            String imageUrl = getFirstImageUrl(listing.getImagePaths());
                            return ProposalDTO.ProposalItemDTO.builder()
                                    .itemId(item.getId())
                                    .listingId(listing.getId())
                                    .listingDisplay(display)
                                    .imageUrl(imageUrl)
                                    .side("REQUESTED")
                                    .build();
                        })
                        .collect(Collectors.toList());
            });
        }
        
        // Get user display names
        String aUserDisplayName = userRepository.findById(s.getAUserId())
                .map(user -> user.getDisplayName())
                .orElse("未知使用者");
        
        String bUserDisplayName = userRepository.findById(s.getBUserId())
                .map(user -> user.getDisplayName())
                .orElse("未知使用者");
        
        String proposerDisplayName = proposerIdArray[0] != null 
                ? userRepository.findById(proposerIdArray[0])
                    .map(user -> user.getDisplayName())
                    .orElse("未知使用者")
                : null;
        
        String receiverDisplayName = receiverIdArray[0] != null
                ? userRepository.findById(receiverIdArray[0])
                    .map(user -> user.getDisplayName())
                    .orElse("未知使用者")
                : null;
        
        return SwapDTO.builder()
                .id(s.getId())
                .listingId(s.getListingId())
                .proposalId(s.getProposalId())
                .aUserId(s.getAUserId())
                .aUserDisplayName(aUserDisplayName)
                .bUserId(s.getBUserId())
                .bUserDisplayName(bUserDisplayName)
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .completedAt(s.getCompletedAt())
                .aConfirmedAt(s.getAConfirmedAt())
                .bConfirmedAt(s.getBConfirmedAt())
                .proposerItems(proposerItemsArray[0])
                .receiverItems(receiverItemsArray[0])
                .proposerId(proposerIdArray[0])
                .proposerDisplayName(proposerDisplayName)
                .receiverId(receiverIdArray[0])
                .receiverDisplayName(receiverDisplayName)
                .build();
    }

            private void finalizeListingsForCompletedSwap(Swap swap) {
                java.util.Set<Long> listingIdsToComplete = new java.util.HashSet<>();
                if (swap.getListingId() != null) {
                    listingIdsToComplete.add(swap.getListingId());
                }

                if (swap.getProposalId() != null) {
                    proposalRepository.findById(swap.getProposalId()).ifPresent(proposal -> {
                        proposal.getProposalItems().stream()
                            .filter(item -> item.getSide() == ProposalItem.Side.OFFERED)
                            .map(ProposalItem::getListing)
                            .filter(java.util.Objects::nonNull)
                            .map(Listing::getId)
                            .forEach(listingIdsToComplete::add);
                    });
                }

                if (listingIdsToComplete.isEmpty()) {
                    return;
                }

                java.util.List<Listing> listings = listingRepository.findAllById(listingIdsToComplete);
                for (Listing listing : listings) {
                    if (listing.getStatus() != Listing.Status.COMPLETED) {
                        listing.setStatus(Listing.Status.COMPLETED);
                    }
                }
                listingRepository.saveAll(listings);
            }

    private int toPageIndex(Integer page) { return (page == null || page <= 1) ? 0 : page - 1; }
    private int toPageSize(Integer size) { return (size == null || size <= 0) ? 10 : Math.min(size, 100); }
    private Sort parseSort(String sort) {
        String prop = "createdAt";
        Sort.Direction dir = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && !parts[0].isBlank()) prop = parts[0].trim();
            if (parts.length >= 2) {
                String d = parts[1].trim().toUpperCase();
                if ("ASC".equals(d)) dir = Sort.Direction.ASC; else if ("DESC".equals(d)) dir = Sort.Direction.DESC;
            }
        }
        if (!prop.equals("createdAt") && !prop.equals("updatedAt") && !prop.equals("id")) prop = "createdAt";
        return Sort.by(dir, prop);
    }

    /**
     * 從 imagePaths JSON 字串解析出第一張圖片的 URL
     */
    private String getFirstImageUrl(String imagePaths) {
        if (imagePaths == null || imagePaths.trim().isEmpty()) {
            return null;
        }
        try {
            // 簡單的JSON反序列化，移除方括號和引號
            String cleaned = imagePaths.replaceAll("[\\[\\]\"]", "");
            if (cleaned.trim().isEmpty()) {
                return null;
            }
            
            // 取得第一個檔案名稱
            String[] fileNames = cleaned.split(",");
            if (fileNames.length > 0) {
                String fileName = fileNames[0].trim();
                return "/images/" + fileName;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static class UnauthorizedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
}
