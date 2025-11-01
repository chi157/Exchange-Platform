package com.exchange.platform.service;

import com.exchange.platform.entity.Review;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SwapService swapService;
    private final UserService userService;

    public Review createReview(Long swapId, Long reviewerId, Long revieweeId, 
                              Map<String, Integer> scores, String comment) {
        Swap swap = swapService.getSwapById(swapId);
        
        if (!swap.isCompleted()) {
            throw new BusinessRuleViolationException("Can only review completed swaps");
        }
        
        if (reviewRepository.existsBySwapIdAndReviewerId(swapId, reviewerId)) {
            throw new BusinessRuleViolationException("Already reviewed this swap");
        }
        
        User reviewer = userService.getUserById(reviewerId);
        User reviewee = userService.getUserById(revieweeId);
        
        Review review = Review.builder()
                .swap(swap)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .scores(scores)
                .comment(comment)
                .build();
        
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> getUserReviews(Long userId) {
        return reviewRepository.findByRevieweeId(userId);
    }

    @Transactional(readOnly = true)
    public Double getUserAverageScore(Long userId) {
        List<Review> reviews = reviewRepository.findByRevieweeId(userId);
        if (reviews.isEmpty()) return 0.0;
        
        return reviews.stream()
                .mapToDouble(Review::getAverageScore)
                .average()
                .orElse(0.0);
    }
}
