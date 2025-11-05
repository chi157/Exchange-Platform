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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

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

        // 處理圖片路徑
        System.out.println("DEBUG - create(): 接收到的圖片檔名清單: " + request.getImageFileNames());
        String imagePathsJson = serializeImagePaths(request.getImageFileNames());
        System.out.println("DEBUG - create(): 序列化後的JSON: " + imagePathsJson);
        
        Listing listing = Listing.builder()
                .cardName(request.getCardName())
                .groupName(request.getGroupName())
                .artistName(request.getArtistName())
                .description(request.getDescription())
                .cardSource(request.getCardSource())
                .conditionRating(request.getConditionRating())
                .hasProtection(request.getHasProtection())
                .remarks(request.getRemarks())
                .imagePaths(imagePathsJson)
                .userId(userId)
                .build();

        listing = listingRepository.save(listing);
        return toDTO(listing, userId);
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
            // 搜尋卡片名稱、藝人名稱、團體名稱或描述
            pg = listingRepository.findByCardNameContainingIgnoreCaseOrArtistNameContainingIgnoreCaseOrGroupNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                q, q, q, q, pageable);
        } else {
            pg = listingRepository.findAll(pageable);
        }
        return pg.stream().map(l -> toDTO(l, currentUserId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ListingDTO> listPage(Integer page, Integer size, String q, String sort, Long excludeOwnerId) {
        // 1-based page number from API; convert to 0-based for Spring Data
        int pageIndex = (page == null || page <= 1) ? 0 : page - 1;
        int pageSize = (size == null || size <= 0) ? 5 : Math.min(size, 100);

        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sortSpec);

        Specification<Listing> baseSpec = buildBaseSpec(q, null, excludeOwnerId);
        
        // 使用標準Spring Data分頁，依賴statusRank確保COMPLETED項目在後
        Page<Listing> result = listingRepository.findAll(baseSpec, pageable);

        List<ListingDTO> content = result.getContent().stream()
                .map(this::toDTO)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(content, pageable, result.getTotalElements());
    }
    
    @Transactional(readOnly = true)
    public Page<ListingDTO> myListingsPage(Long ownerId, Integer page, Integer size, String q, String sort) {
        // 1-based page number from API; convert to 0-based for Spring Data
        int pageIndex = (page == null || page <= 1) ? 0 : page - 1;
        int pageSize = (size == null || size <= 0) ? 5 : Math.min(size, 100);

    Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sortSpec);

        Specification<Listing> baseSpec = buildBaseSpec(q, ownerId, null);
        
        // 使用標準Spring Data分頁，依賴statusRank確保COMPLETED項目在後
        Page<Listing> result = listingRepository.findAll(baseSpec, pageable);

        List<ListingDTO> content = result.getContent().stream()
                .map(l -> toDTO(l, ownerId))
                .toList();

        return new org.springframework.data.domain.PageImpl<>(content, pageable, result.getTotalElements());
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

        return Sort.by(new Sort.Order(dir, prop));
    }

    private Specification<Listing> buildBaseSpec(String q, Long ownerId, Long excludeOwnerId) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (ownerId != null) {
                predicates.add(cb.equal(root.get("userId"), ownerId));
            }
            if (excludeOwnerId != null) {
                predicates.add(cb.notEqual(root.get("userId"), excludeOwnerId));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate cardNameLike = cb.like(cb.lower(root.get("cardName")), like);
                jakarta.persistence.criteria.Predicate artistNameLike = cb.like(cb.lower(root.get("artistName")), like);
                jakarta.persistence.criteria.Predicate groupNameLike = cb.like(cb.lower(root.get("groupName")), like);
                jakarta.persistence.criteria.Predicate descLike = cb.like(cb.lower(root.get("description")), like);
                predicates.add(cb.or(cardNameLike, artistNameLike, groupNameLike, descLike));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }



    private ListingDTO toDTO(Listing l) {
        return toDTO(l, null);
    }
    
    private ListingDTO toDTO(Listing l, Long currentUserId) {
        Long userId = l.getUserId();
        
        String ownerDisplayName = userId == null ? "未知使用者" :
            userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("未知使用者");

        boolean isMine = currentUserId != null && userId != null && userId.equals(currentUserId);
        
        // 解析圖片路徑
        List<String> imageUrls = parseImageUrls(l.getImagePaths());
        
        return ListingDTO.builder()
                .id(l.getId())
                .cardName(l.getCardName())
                .groupName(l.getGroupName())
                .artistName(l.getArtistName())
                .description(l.getDescription())
                .cardSource(l.getCardSource())
                .cardSourceDisplay(l.getCardSource() != null ? l.getCardSource().getDisplayName() : null)
                .conditionRating(l.getConditionRating())
                .hasProtection(l.getHasProtection())
                .remarks(l.getRemarks())
                .imageUrls(imageUrls)
                .userId(userId)
                .ownerDisplayName(ownerDisplayName)
                .isMine(isMine)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .status(l.getStatus())
                .statusDisplay(l.getStatus() != null ? l.getStatus().getDisplayName() : null)
                .build();
    }

    public ListingDTO update(Long id, CreateListingRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new UnauthorizedException();
        }

        Listing listing = listingRepository.findById(id).orElseThrow(NotFoundException::new);
        
        // 檢查是否為自己的物品
        if (!listing.getUserId().equals(userId)) {
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

        // 更新卡片資訊
        listing.setCardName(request.getCardName());
        listing.setGroupName(request.getGroupName());
        listing.setArtistName(request.getArtistName());
        listing.setDescription(request.getDescription());
        listing.setCardSource(request.getCardSource());
        listing.setConditionRating(request.getConditionRating());
        listing.setHasProtection(request.getHasProtection());
        listing.setRemarks(request.getRemarks());
        
        // 更新圖片
        if (request.getImageFileNames() != null && !request.getImageFileNames().isEmpty()) {
            String imagePathsJson = serializeImagePaths(request.getImageFileNames());
            listing.setImagePaths(imagePathsJson);
        }
        
        listing = listingRepository.save(listing);
        
        return toDTO(listing, userId);
    }

    // === 圖片處理相關方法 ===
    private String serializeImagePaths(List<String> imageFileNames) {
        System.out.println("DEBUG - serializeImagePaths(): 輸入參數: " + imageFileNames);
        if (imageFileNames == null || imageFileNames.isEmpty()) {
            System.out.println("DEBUG - serializeImagePaths(): 圖片清單為空，返回null");
            return null;
        }
        // 簡單的JSON序列化，實際專案可使用Jackson
        String result = "[\"" + String.join("\",\"", imageFileNames) + "\"]";
        System.out.println("DEBUG - serializeImagePaths(): 序列化結果: " + result);
        return result;
    }
    
    private List<String> parseImageUrls(String imagePaths) {
        if (imagePaths == null || imagePaths.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // 簡單的JSON反序列化，移除方括號和引號
            String cleaned = imagePaths.replaceAll("[\\[\\]\"]", "");
            if (cleaned.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // 將檔案名稱轉換為完整的URL路徑
            return List.of(cleaned.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(fileName -> !fileName.isEmpty())
                    .map(fileName -> "/images/" + fileName)
                    .toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // 測試方法：獨立測試序列化功能
    public String testSerialization(List<String> fileNames) {
        System.out.println("DEBUG - testSerialization(): 測試輸入: " + fileNames);
        return serializeImagePaths(fileNames);
    }

    public static class UnauthorizedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
    public static class ConflictException extends RuntimeException {}
}
