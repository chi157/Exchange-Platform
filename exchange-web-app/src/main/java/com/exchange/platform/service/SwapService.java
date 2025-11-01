package com.exchange.platform.service;

import com.exchange.platform.dto.SwapDTO;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.repository.SwapRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SwapService {

    private final SwapRepository swapRepository;
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

    public SwapDTO toDTO(Swap s) {
        return SwapDTO.builder()
                .id(s.getId())
                .listingId(s.getListingId())
                .proposalId(s.getProposalId())
                .aUserId(s.getAUserId())
                .bUserId(s.getBUserId())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .completedAt(s.getCompletedAt())
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
