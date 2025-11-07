package com.exchange.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "sent", nullable = false)
    private Boolean sent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum NotificationType {
        // 提案相關
        PROPOSAL_RECEIVED("收到新提案"),
        PROPOSAL_ACCEPTED("提案被接受"),
        PROPOSAL_REJECTED("提案被拒絕"),
        PROPOSAL_WITHDRAWN("提案被撤回"),
        
        // 交換流程相關
        SWAP_CONFIRMED("交換確認"),
        DELIVERY_METHOD_PROPOSED("運送方式提案"),
        DELIVERY_METHOD_ACCEPTED("運送方式確認"),
        DELIVERY_METHOD_REJECTED("運送方式被拒絕"),
        MEETUP_SCHEDULED("面交時間確認"),
        
        // 物流相關
        SHIPMENT_SENT("包裹已寄出"),
        SHIPMENT_RECEIVED("包裹已送達"),
        TRACKING_UPDATE("物流狀態更新"),
        
        // 完成相關
        EXCHANGE_COMPLETED("交換完成"),
        REVIEW_REMINDER("評價提醒"),
        
        // 系統相關
        DISPUTE_CREATED("爭議處理開始"),
        ACCOUNT_VERIFICATION("帳號驗證"),
        SECURITY_ALERT("安全提醒");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}