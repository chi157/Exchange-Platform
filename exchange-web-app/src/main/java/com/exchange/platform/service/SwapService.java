package com.exchange.platform.service;

import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.dto.SwapDTO;
import com.exchange.platform.entity.ProposalItem;
import com.exchange.platform.entity.Swap;
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
    private final com.exchange.platform.repository.UserRepository userRepository;
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
            }
        }

        swap = swapRepository.save(swap);
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
                receiverIdArray[0] = proposal.getReceiverIdLegacy();
                
                proposerItemsArray[0] = proposal.getProposalItems().stream()
                        .filter(item -> item.getSide() == ProposalItem.Side.OFFERED)
                        .map(item -> ProposalDTO.ProposalItemDTO.builder()
                                .itemId(item.getId())
                                .listingId(item.getListing().getId())
                                .listingTitle(item.getListing().getTitle())
                                .side("OFFERED")
                                .build())
                        .collect(Collectors.toList());
                
                receiverItemsArray[0] = proposal.getProposalItems().stream()
                        .filter(item -> item.getSide() == ProposalItem.Side.REQUESTED)
                        .map(item -> ProposalDTO.ProposalItemDTO.builder()
                                .itemId(item.getId())
                                .listingId(item.getListing().getId())
                                .listingTitle(item.getListing().getTitle())
                                .side("REQUESTED")
                                .build())
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

    public static class UnauthorizedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
}
