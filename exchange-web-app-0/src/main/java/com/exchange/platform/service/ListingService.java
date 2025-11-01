package com.exchange.platform.service;

import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.ResourceNotFoundException;
import com.exchange.platform.exception.UnauthorizedAccessException;
import com.exchange.platform.exception.ValidationException;
import com.exchange.platform.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 卡片刊登服務
 * 處理卡片的新增、查詢、更新、刪除等操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserService userService;

    /**
     * 建立新的卡片刊登
     */
    public Listing createListing(Long userId, String title, String description,
                                 String idolGroup, String memberName, String album,
                                 String era, String version, String cardCode,
                                 Boolean isOfficial, Listing.CardCondition condition,
                                 List<String> photos) {
        log.debug("建立新刊登，使用者ID: {}", userId);
        
        User user = userService.getUserById(userId);
        
        Listing listing = Listing.builder()
                .user(user)
                .title(title)
                .description(description)
                .idolGroup(idolGroup)
                .memberName(memberName)
                .album(album)
                .era(era)
                .version(version)
                .cardCode(cardCode)
                .isOfficial(isOfficial != null ? isOfficial : true)
                .condition(condition)
                .photos(photos != null ? photos : List.of())
                .status(Listing.ListingStatus.ACTIVE)
                .build();
        
        Listing savedListing = listingRepository.save(listing);
        log.info("成功建立刊登，ID: {}", savedListing.getId());
        return savedListing;
    }

    /**
     * 根據ID查詢刊登
     */
    @Transactional(readOnly = true)
    public Listing getListingById(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));
    }

    /**
     * 搜尋刊登（支援關鍵字搜尋）
     */
    @Transactional(readOnly = true)
    public Page<Listing> searchListings(String keyword, Pageable pageable) {
        log.debug("執行搜尋，關鍵字: {}", keyword);
        return listingRepository.searchListings(keyword != null ? keyword : "", pageable);
    }

    /**
     * 根據偶像團體搜尋
     */
    @Transactional(readOnly = true)
    public List<Listing> searchByIdolGroup(String idolGroup) {
        return listingRepository.findByIdolGroupAndStatus(idolGroup, Listing.ListingStatus.ACTIVE);
    }

    /**
     * 根據成員名稱搜尋
     */
    @Transactional(readOnly = true)
    public List<Listing> searchByMemberName(String memberName) {
        return listingRepository.findByMemberNameAndStatus(memberName, Listing.ListingStatus.ACTIVE);
    }

    /**
     * 查詢使用者的所有刊登
     */
    @Transactional(readOnly = true)
    public List<Listing> getUserListings(Long userId) {
        return listingRepository.findByUserId(userId);
    }

    /**
     * 查詢使用者的可用刊登（ACTIVE狀態）
     */
    @Transactional(readOnly = true)
    public List<Listing> getUserAvailableListings(Long userId) {
        return listingRepository.findByUserIdAndStatus(userId, Listing.ListingStatus.ACTIVE);
    }

    /**
     * 更新刊登資訊
     */
    public Listing updateListing(Long listingId, Long userId, String title, String description,
                                String album, String era, String version,
                                Listing.CardCondition condition) {
        log.debug("更新刊登 {} by 使用者 {}", listingId, userId);
        
        Listing listing = getListingById(listingId);
        
        // 驗證擁有權
        if (!listing.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("只有擁有者可以更新刊登");
        }
        
        // 只有ACTIVE狀態才能更新
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            ValidationException ex = new ValidationException("只有ACTIVE狀態的刊登可以更新");
            ex.addFieldError("status", "當前狀態: " + listing.getStatus());
            throw ex;
        }
        
        if (title != null) listing.setTitle(title);
        if (description != null) listing.setDescription(description);
        if (album != null) listing.setAlbum(album);
        if (era != null) listing.setEra(era);
        if (version != null) listing.setVersion(version);
        if (condition != null) listing.setCondition(condition);
        
        Listing updatedListing = listingRepository.save(listing);
        log.info("成功更新刊登: {}", listingId);
        return updatedListing;
    }

    /**
     * 新增照片
     */
    public Listing addPhoto(Long listingId, Long userId, String photoUrl) {
        Listing listing = getListingById(listingId);
        
        if (!listing.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("只有擁有者可以新增照片");
        }
        
        listing.addPhoto(photoUrl);
        return listingRepository.save(listing);
    }

    /**
     * 移除照片
     */
    public Listing removePhoto(Long listingId, Long userId, String photoUrl) {
        Listing listing = getListingById(listingId);
        
        if (!listing.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("只有擁有者可以移除照片");
        }
        
        listing.removePhoto(photoUrl);
        return listingRepository.save(listing);
    }

    /**
     * 鎖定刊登（用於提案）
     */
    public void lockListing(Long listingId, Long proposalId) {
        Listing listing = getListingById(listingId);
        listing.lock(proposalId);
        listingRepository.save(listing);
        log.info("鎖定刊登成功: {}, 提案ID: {}", listingId, proposalId);
    }

    /**
     * 解鎖刊登（取消提案時使用）
     */
    public void unlockListing(Long listingId) {
        Listing listing = getListingById(listingId);
        listing.unlock();
        listingRepository.save(listing);
        log.info("解鎖刊登成功: {}", listingId);
    }

    /**
     * 標記為已交易
     */
    public void markAsTraded(Long listingId) {
        Listing listing = getListingById(listingId);
        listing.markAsTraded();
        listingRepository.save(listing);
        log.info("標記為已交易成功: {}", listingId);
    }

    /**
     * 刪除刊登
     */
    public void deleteListing(Long listingId, Long userId) {
        log.debug("刪除刊登 {} by 使用者 {}", listingId, userId);
        
        Listing listing = getListingById(listingId);
        
        if (!listing.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("只有擁有者可以刪除刊登");
        }
        
        listing.markAsDeleted();
        listingRepository.save(listing);
        log.info("刪除刊登成功: {}", listingId);
    }
}
