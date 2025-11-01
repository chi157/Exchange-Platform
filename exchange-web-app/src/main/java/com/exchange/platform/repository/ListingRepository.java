package com.exchange.platform.repository;

import com.exchange.platform.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByUserId(Long userId);

    List<Listing> findByStatus(Listing.ListingStatus status);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.lockedByProposalId IS NULL")
    List<Listing> findAvailableListings();

    @Query("SELECT l FROM Listing l WHERE l.idolGroup = :group AND l.status = 'ACTIVE'")
    List<Listing> findByIdolGroupAndActive(@Param("group") String idolGroup);

    @Query("SELECT l FROM Listing l WHERE l.idolGroup = :group AND l.memberName = :member AND l.status = 'ACTIVE'")
    List<Listing> findByIdolGroupAndMemberAndActive(@Param("group") String idolGroup, @Param("member") String memberName);

    List<Listing> findByLockedByProposalId(Long proposalId);
}