package com.exchange.platform.service;

import com.exchange.platform.entity.ChatMessage;
import com.exchange.platform.entity.ChatRoom;
import com.exchange.platform.repository.ChatMessageRepository;
import com.exchange.platform.repository.ChatRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 聊天服務
 * 處理聊天室和消息的業務邏輯
 */
@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    /**
     * 創建聊天室（當 Proposal 創建時自動調用）
     */
    @Transactional
    public ChatRoom createChatRoom(Long proposalId, Long userAId, Long userBId) {
        // 檢查是否已存在
        Optional<ChatRoom> existing = chatRoomRepository.findByProposalId(proposalId);
        if (existing.isPresent()) {
            logger.info("Chat room already exists for proposal: {}", proposalId);
            return existing.get();
        }
        
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setProposalId(proposalId);
        chatRoom.setUserAId(userAId);
        chatRoom.setUserBId(userBId);
        chatRoom.setStatus(ChatRoom.ChatRoomStatus.ACTIVE);
        
        ChatRoom saved = chatRoomRepository.save(chatRoom);
        logger.info("Created chat room for proposal: {}, room ID: {}", proposalId, saved.getId());
        
        // 創建系統歡迎消息
        createSystemMessage(saved.getId(), "聊天室已建立，雙方可以開始討論卡片交換細節。");
        
        return saved;
    }
    
    /**
     * 更新聊天室的 Swap ID（當 Proposal 被接受時）
     */
    @Transactional
    public void updateChatRoomSwapId(Long proposalId, Long swapId) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findByProposalId(proposalId);
        if (chatRoom.isPresent()) {
            ChatRoom room = chatRoom.get();
            room.setSwapId(swapId);
            chatRoomRepository.save(room);
            
            // 創建系統通知消息
            createSystemMessage(room.getId(), "提案已被接受！交換已開始，請確認配送方式和地址。");
            
            logger.info("Updated chat room swap ID for proposal: {}, swap ID: {}", proposalId, swapId);
        }
    }
    
    /**
     * 獲取用戶的所有聊天室列表
     */
    public List<ChatRoom> getUserChatRooms(Long userId) {
        return chatRoomRepository.findByUserAIdOrUserBIdOrderByLastMessageAtDesc(userId, userId);
    }
    
    /**
     * 根據 Proposal ID 獲取聊天室
     */
    public Optional<ChatRoom> getChatRoomByProposalId(Long proposalId) {
        return chatRoomRepository.findByProposalId(proposalId);
    }
    
    /**
     * 發送文字消息
     */
    @Transactional
    public ChatMessage sendTextMessage(Long chatRoomId, Long senderId, String content) {
        ChatMessage message = new ChatMessage();
        message.setChatRoomId(chatRoomId);
        message.setSenderId(senderId);
        message.setType(ChatMessage.MessageType.TEXT);
        message.setContent(content);
        message.setIsRead(false);
        
        ChatMessage saved = chatMessageRepository.save(message);
        
        // 更新聊天室的最後消息時間
        updateChatRoomLastMessageTime(chatRoomId);
        
        logger.info("Sent text message in chat room: {}, sender: {}", chatRoomId, senderId);
        return saved;
    }
    
    /**
     * 發送圖片消息
     */
    @Transactional
    public ChatMessage sendImageMessage(Long chatRoomId, Long senderId, String imageUrl) {
        ChatMessage message = new ChatMessage();
        message.setChatRoomId(chatRoomId);
        message.setSenderId(senderId);
        message.setType(ChatMessage.MessageType.IMAGE);
        message.setImageUrl(imageUrl);
        message.setIsRead(false);
        
        ChatMessage saved = chatMessageRepository.save(message);
        
        // 更新聊天室的最後消息時間
        updateChatRoomLastMessageTime(chatRoomId);
        
        logger.info("Sent image message in chat room: {}, sender: {}", chatRoomId, senderId);
        return saved;
    }
    
    /**
     * 創建系統通知消息
     */
    @Transactional
    public ChatMessage createSystemMessage(Long chatRoomId, String content) {
        ChatMessage message = new ChatMessage();
        message.setChatRoomId(chatRoomId);
        message.setSenderId(null);  // 系統消息無發送者
        message.setType(ChatMessage.MessageType.SYSTEM);
        message.setContent(content);
        message.setIsRead(true);  // 系統消息默認已讀
        
        ChatMessage saved = chatMessageRepository.save(message);
        
        // 更新聊天室的最後消息時間
        updateChatRoomLastMessageTime(chatRoomId);
        
        logger.info("Created system message in chat room: {}", chatRoomId);
        return saved;
    }
    
    /**
     * 獲取聊天室的所有消息
     */
    public List<ChatMessage> getChatRoomMessages(Long chatRoomId) {
        return chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);
    }
    
    /**
     * 獲取聊天室的最近 50 條消息
     */
    public List<ChatMessage> getRecentMessages(Long chatRoomId) {
        List<ChatMessage> messages = chatMessageRepository.findTop50ByChatRoomIdOrderBySentAtDesc(chatRoomId);
        // 反轉列表，使其按時間升序
        java.util.Collections.reverse(messages);
        return messages;
    }
    
    /**
     * 標記消息為已讀
     */
    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        chatMessageRepository.markAllAsRead(chatRoomId, userId);
        logger.info("Marked messages as read in chat room: {} for user: {}", chatRoomId, userId);
    }
    
    /**
     * 獲取未讀消息數量
     */
    public long getUnreadMessageCount(Long chatRoomId, Long userId) {
        return chatMessageRepository.countUnreadMessages(chatRoomId, userId);
    }
    
    /**
     * 更新聊天室的最後消息時間
     */
    private void updateChatRoomLastMessageTime(Long chatRoomId) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(chatRoomId);
        if (chatRoom.isPresent()) {
            ChatRoom room = chatRoom.get();
            room.setLastMessageAt(LocalDateTime.now());
            chatRoomRepository.save(room);
        }
    }
    
    /**
     * 驗證用戶是否有權訪問聊天室
     */
    public boolean hasAccessToChatRoom(Long chatRoomId, Long userId) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(chatRoomId);
        if (chatRoom.isEmpty()) {
            return false;
        }
        
        ChatRoom room = chatRoom.get();
        return room.getUserAId().equals(userId) || room.getUserBId().equals(userId);
    }
}
