package com.exchange.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 聊天室實體
 * 每個 Proposal 創建時自動建立對應的聊天室
 */
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 關聯的提案 ID
     */
    @Column(name = "proposal_id", nullable = false, unique = true)
    private Long proposalId;
    
    /**
     * 關聯的交換 ID (可選，當提案被接受後會有)
     */
    @Column(name = "swap_id")
    private Long swapId;
    
    /**
     * 用戶 A (提案發起者)
     */
    @Column(name = "user_a_id", nullable = false)
    private Long userAId;
    
    /**
     * 用戶 B (收到提案的用戶)
     */
    @Column(name = "user_b_id", nullable = false)
    private Long userBId;
    
    /**
     * 聊天室狀態：ACTIVE(活躍), READ_ONLY(唯讀), ARCHIVED(已歸檔)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;
    
    /**
     * 是否為唯讀模式（Swap 完成後變為唯讀）
     */
    @Column(name = "is_read_only", nullable = false)
    private Boolean isReadOnly = false;
    
    /**
     * 唯讀開始時間（Swap 完成時設定）
     */
    @Column(name = "read_only_since")
    private LocalDateTime readOnlySince;
    
    /**
     * 創建時間
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 最後一條消息時間 (用於排序)
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getProposalId() {
        return proposalId;
    }
    
    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }
    
    public Long getSwapId() {
        return swapId;
    }
    
    public void setSwapId(Long swapId) {
        this.swapId = swapId;
    }
    
    public Long getUserAId() {
        return userAId;
    }
    
    public void setUserAId(Long userAId) {
        this.userAId = userAId;
    }
    
    public Long getUserBId() {
        return userBId;
    }
    
    public void setUserBId(Long userBId) {
        this.userBId = userBId;
    }
    
    public ChatRoomStatus getStatus() {
        return status;
    }
    
    public void setStatus(ChatRoomStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
    
    /**
     * 聊天室狀態枚舉
     */
    public enum ChatRoomStatus {
        ACTIVE,    // 活躍中
        ARCHIVED   // 已歸檔 (交換完成後可歸檔，但聊天記錄永久保留)
    }
}
