package com.exchange.platform.repository;

import com.exchange.platform.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findBySwapId(Long swapId);

    List<Review> findByReviewerId(Long reviewerId);

    List<Review> findByRevieweeId(Long revieweeId);

    Optional<Review> findBySwapIdAndReviewerId(Long swapId, Long reviewerId);

    @Query("SELECT AVG(CAST(json_extract(r.scores, '$.overall') AS double)) FROM Review r WHERE r.reviewee.id = :userId")
    Double getAverageScoreByRevieweeId(@Param("userId") Long userId);
}