package com.exchange.platform.repository;

import com.exchange.platform.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    // 基本查詢
    List<Listing> findByUserId(Long userId);

    List<Listing> findByStatus(Listing.ListingStatus status);

    List<Listing> findByUserIdAndStatus(Long userId, Listing.ListingStatus status);

    List<Listing> findByIdolGroupAndStatus(String idolGroup, Listing.ListingStatus status);

    List<Listing> findByMemberNameAndStatus(String memberName, Listing.ListingStatus status);

    List<Listing> findByLockedByProposalId(Long proposalId);

    // 搜尋查詢（支援分頁）
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.idolGroup) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.memberName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.album) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Listing> searchListings(@Param("keyword") String keyword, Pageable pageable);

    // 可用小卡查詢
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.lockedByProposalId IS NULL")
    List<Listing> findAvailableListings();

    @Query("SELECT l FROM Listing l WHERE l.idolGroup = :group AND l.status = 'ACTIVE'")
    List<Listing> findByIdolGroupAndActive(@Param("group") String idolGroup);

    @Query("SELECT l FROM Listing l WHERE l.idolGroup = :group AND l.memberName = :member AND l.status = 'ACTIVE'")
    List<Listing> findByIdolGroupAndMemberAndActive(@Param("group") String idolGroup, @Param("member") String memberName);
}