package com.exchange.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 聊天消息實體
 * 支援文字、圖片、系統通知消息
 * 永久保留，不允許刪除
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 所屬聊天室 ID
     */
    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;
    
    /**
     * 發送者 ID (系統消息時為 null)
     */
    @Column(name = "sender_id")
    private Long senderId;
    
    /**
     * 消息類型：TEXT(文字), IMAGE(圖片), SYSTEM(系統通知)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType type;
    
    /**
     * 消息內容 (文字消息或系統通知文字)
     */
    @Column(columnDefinition = "TEXT")
    private String content;
    
    /**
     * 圖片 URL (圖片消息時使用)
     */
    @Column(name = "image_url")
    private String imageUrl;
    
    /**
     * 是否已讀
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    /**
     * 發送時間
     */
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;
    
    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getChatRoomId() {
        return chatRoomId;
    }
    
    public void setChatRoomId(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    /**
     * 消息類型枚舉
     */
    public enum MessageType {
        TEXT,      // 文字消息
        IMAGE,     // 圖片消息
        SYSTEM     // 系統通知 (如：交換狀態更新)
    }
}
