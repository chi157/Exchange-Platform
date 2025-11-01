package com.exchange.platform.service;

import com.exchange.platform.entity.*;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SwapService swapService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewService reviewService;

    private User reviewer;
    private User reviewee;
    private Swap completedSwap;
    private Review testReview;
    private Map<String, Integer> testScores;

    @BeforeEach
    void setUp() {
        reviewer = User.builder()
                .email("reviewer@test.com")
                .displayName("Reviewer")
                .build();
        reviewer.setId(1L);

        reviewee = User.builder()
                .email("reviewee@test.com")
                .displayName("Reviewee")
                .build();
        reviewee.setId(2L);

        completedSwap = Swap.builder()
                .userA(reviewer)
                .userB(reviewee)
                .status(Swap.SwapStatus.COMPLETED)
                .build();
        completedSwap.setId(1L);

        testScores = new HashMap<>();
        testScores.put("packaging", 5);
        testScores.put("communication", 5);
        testScores.put("timeliness", 4);

        testReview = Review.builder()
                .swap(completedSwap)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .scores(testScores)
                .comment("Great exchange!")
                .build();
        testReview.setId(1L);
    }

    @Test
    void createReview_Success() {
        // Arrange
        when(swapService.getSwapById(1L)).thenReturn(completedSwap);
        when(userService.getUserById(1L)).thenReturn(reviewer);
        when(userService.getUserById(2L)).thenReturn(reviewee);
        when(reviewRepository.existsBySwapIdAndReviewerId(1L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        // Act
        Review result = reviewService.createReview(1L, 1L, 2L, testScores, "Great!");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReviewer()).isEqualTo(reviewer);
        assertThat(result.getReviewee()).isEqualTo(reviewee);
        assertThat(result.getScores()).isEqualTo(testScores);
        
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_SwapNotCompleted_ThrowsException() {
        // Arrange
        completedSwap.setStatus(Swap.SwapStatus.SHIPPING);
        when(swapService.getSwapById(1L)).thenReturn(completedSwap);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(1L, 1L, 2L, testScores, "Great!"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void createReview_AlreadyReviewed_ThrowsException() {
        // Arrange
        when(swapService.getSwapById(1L)).thenReturn(completedSwap);
        when(reviewRepository.existsBySwapIdAndReviewerId(1L, 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(1L, 1L, 2L, testScores, "Great!"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Already");
    }

    @Test
    void getUserReviews_Success() {
        // Arrange
        when(reviewRepository.findByRevieweeId(2L)).thenReturn(List.of(testReview));

        // Act
        List<Review> result = reviewService.getUserReviews(2L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReview);
        
        verify(reviewRepository).findByRevieweeId(2L);
    }

    @Test
    void getUserAverageScore_WithReviews_ReturnsAverage() {
        // Arrange
        Map<String, Integer> scores1 = new HashMap<>();
        scores1.put("packaging", 5);
        scores1.put("communication", 5);
        
        Map<String, Integer> scores2 = new HashMap<>();
        scores2.put("packaging", 4);
        scores2.put("communication", 3);
        
        Review review1 = Review.builder().scores(scores1).build();
        Review review2 = Review.builder().scores(scores2).build();
        
        when(reviewRepository.findByRevieweeId(2L)).thenReturn(List.of(review1, review2));

        // Act
        Double result = reviewService.getUserAverageScore(2L);

        // Assert
        assertThat(result).isEqualTo(4.25);
    }

    @Test
    void getUserAverageScore_NoReviews_ReturnsZero() {
        // Arrange
        when(reviewRepository.findByRevieweeId(2L)).thenReturn(List.of());

        // Act
        Double result = reviewService.getUserAverageScore(2L);

        // Assert
        assertThat(result).isEqualTo(0.0);
    }
}
