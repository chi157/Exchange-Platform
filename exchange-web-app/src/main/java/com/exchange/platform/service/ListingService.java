package com.exchange.platform.service;

import com.exchange.platform.dto.CreateListingRequest;
import com.exchange.platform.dto.ListingDTO;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.repository.ListingRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
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
    public List<ListingDTO> list(Integer limit, Integer offset) {
        int lim = (limit == null || limit <= 0) ? 10 : Math.min(limit, 100);
        int off = (offset == null || offset < 0) ? 0 : offset;
        return listingRepository.findAll(PageRequest.of(off / lim, lim))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ListingDTO toDTO(Listing l) {
        return ListingDTO.builder()
                .id(l.getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .ownerId(l.getOwnerId())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    public static class UnauthorizedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
