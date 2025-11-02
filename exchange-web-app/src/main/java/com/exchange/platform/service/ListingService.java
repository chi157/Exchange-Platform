package com.exchange.platform.service;

import com.exchange.platform.dto.CreateListingRequest;
import com.exchange.platform.dto.ListingDTO;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.repository.ListingRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
    private final com.exchange.platform.repository.ProposalRepository proposalRepository;
    private final com.exchange.platform.repository.UserRepository userRepository;
    private static final String SESSION_USER_ID = "userId"; // 與 AuthService 相同 key

    public ListingDTO create(CreateListingRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new UnauthorizedException();
        }

        Listing listing = Listing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .ownerId(userId)
                .build();

        listing = listingRepository.save(listing);
        return toDTO(listing);
    }

    @Transactional(readOnly = true)
    public ListingDTO getById(Long id) {
        Listing l = listingRepository.findById(id).orElseThrow(NotFoundException::new);
        return toDTO(l);
    }

    @Transactional(readOnly = true)
    public List<ListingDTO> list(Integer page, Integer size, String q, String sort, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute(SESSION_USER_ID);
        
        // 1-based page number from API; convert to 0-based for Spring Data
        int pageIndex = (page == null || page <= 1) ? 0 : page - 1;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 100);

        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sortSpec);

        Page<Listing> pg;
        if (q != null && !q.isBlank()) {
            pg = listingRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(q, q, pageable);
        } else {
            pg = listingRepository.findAll(pageable);
        }
        return pg.stream().map(l -> toDTO(l, currentUserId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ListingDTO> listPage(Integer page, Integer size, String q, String sort) {
        // 1-based page number from API; convert to 0-based for Spring Data
        int pageIndex = (page == null || page <= 1) ? 0 : page - 1;
        int pageSize = (size == null || size <= 0) ? 5 : Math.min(size, 100);

        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sortSpec);

        Page<Listing> pg;
        if (q != null && !q.isBlank()) {
            pg = listingRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(q, q, pageable);
        } else {
            pg = listingRepository.findAll(pageable);
        }
        return pg.map(this::toDTO);
    }

    private Sort parseSort(String sort) {
        // 支援格式: "createdAt,desc" 或 "createdAt,asc"；預設 createdAt desc
        String prop = "createdAt";
        Sort.Direction dir = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && !parts[0].isBlank()) {
                prop = parts[0].trim();
            }
            if (parts.length >= 2) {
                String d = parts[1].trim().toUpperCase();
                if ("ASC".equals(d)) dir = Sort.Direction.ASC;
                else if ("DESC".equals(d)) dir = Sort.Direction.DESC;
            }
        }
        // 白名單屬性，避免任意欄位注入
        if (!prop.equals("createdAt") && !prop.equals("updatedAt") && !prop.equals("id")) {
            prop = "createdAt";
        }
        return Sort.by(dir, prop);
    }

    private ListingDTO toDTO(Listing l) {
        return toDTO(l, null);
    }
    
    private ListingDTO toDTO(Listing l, Long currentUserId) {
        String ownerDisplayName = userRepository.findById(l.getOwnerId())
                .map(user -> user.getDisplayName())
                .orElse("未知使用者");
        
        return ListingDTO.builder()
                .id(l.getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .ownerId(l.getOwnerId())
                .ownerDisplayName(ownerDisplayName)
                .status(l.getStatus())
                .isMine(currentUserId != null && l.getOwnerId().equals(currentUserId))
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    public ListingDTO update(Long id, CreateListingRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new UnauthorizedException();
        }

        Listing listing = listingRepository.findById(id).orElseThrow(NotFoundException::new);
        
        // 檢查是否為自己的物品
        if (!listing.getOwnerId().equals(userId)) {
            throw new ForbiddenException();
        }

        // 檢查是否有 pending proposal
        boolean hasPendingProposal = proposalRepository
                .findByListingId(id, PageRequest.of(0, 1))
                .stream()
                .anyMatch(p -> p.getStatus() == com.exchange.platform.entity.Proposal.Status.PENDING);
        
        if (hasPendingProposal) {
            throw new ConflictException();
        }

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing = listingRepository.save(listing);
        
        return toDTO(listing);
    }

    public static class UnauthorizedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
    public static class ConflictException extends RuntimeException {}
}
