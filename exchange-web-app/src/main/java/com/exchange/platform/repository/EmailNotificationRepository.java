package com.exchange.platform.repository;

import com.exchange.platform.entity.EmailNotification;
import com.exchange.platform.entity.EmailNotification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    /**
     * 尋找未發送的通知
     */
    List<EmailNotification> findBySentFalseOrderByCreatedAtAsc();

    /**
     * 根據收件人查詢通知歷史
     */
    Page<EmailNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /**
     * 根據通知類型查詢
     */
    List<EmailNotification> findByNotificationTypeAndSentFalse(NotificationType notificationType);

    /**
     * 根據相關實體查詢通知
     */
    List<EmailNotification> findByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, Long relatedEntityId);

    /**
     * 查詢指定時間範圍內的通知
     */
    @Query("SELECT n FROM EmailNotification n WHERE n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<EmailNotification> findNotificationsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 查詢發送失敗需要重試的通知（24小時內創建但未發送）
     */
    @Query("SELECT n FROM EmailNotification n WHERE n.sent = false AND n.createdAt > :cutoffTime ORDER BY n.createdAt ASC")
    List<EmailNotification> findFailedNotificationsForRetry(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 統計未發送通知數量
     */
    long countBySentFalse();

    /**
     * 根據收件人和通知類型查詢最近的通知（避免重複發送）
     */
    @Query("SELECT n FROM EmailNotification n WHERE n.recipientId = :recipientId AND n.notificationType = :type AND n.relatedEntityId = :entityId ORDER BY n.createdAt DESC")
    List<EmailNotification> findRecentNotificationsByTypeAndEntity(
            @Param("recipientId") Long recipientId,
            @Param("type") NotificationType type,
            @Param("entityId") Long entityId,
            Pageable pageable
    );
}