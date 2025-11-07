package com.exchange.platform.service;

import com.exchange.platform.dto.ChatRoomListDTO;
import com.exchange.platform.entity.ChatMessage;
import com.exchange.platform.entity.ChatRoom;
import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.ChatMessageRepository;
import com.exchange.platform.repository.ChatRoomRepository;
import com.exchange.platform.repository.ProposalRepository;
import com.exchange.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ProposalRepository proposalRepository;
    
    @Autowired
    private UserRepository userRepository;
    
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
        createSystemMessage(saved.getId(), "💬 提案聊天室已建立！請雙方討論交換細節，接受提案後即可開始交換流程。");
        
        return saved;
    }
    
    /**
     * 更新聊天室的 Swap ID（當 Proposal 被接受時）
     * 注意：保持 @Transactional，因為這是從 Controller 直接調用的獨立事務
     */
    @Transactional
    public void updateChatRoomSwapId(Long proposalId, Long swapId) {
        try {
            Optional<ChatRoom> chatRoom = chatRoomRepository.findByProposalId(proposalId);
            if (chatRoom.isPresent()) {
                ChatRoom room = chatRoom.get();
                room.setSwapId(swapId);
                room.setStatus(ChatRoom.ChatRoomStatus.ACTIVE); // 確保狀態為活躍
                chatRoomRepository.save(room);
                
                // 創建系統通知消息
                createSystemMessage(room.getId(), "✅ 提案已被接受！交換已開始，請確認配送方式和地址。");
                
                logger.info("Updated chat room swap ID for proposal: {}, swap ID: {}", proposalId, swapId);
            } else {
                logger.warn("No chat room found for proposal: {}, cannot update swap ID", proposalId);
            }
        } catch (Exception e) {
            logger.error("Error updating chat room swap ID for proposal: {}", proposalId, e);
            throw e; // 這裡可以拋出，因為是獨立事務
        }
    }
    
    /**
     * 將聊天室設為唯讀（當 Swap 完成時調用）
     * N 天後可以通過定時任務將唯讀聊天室歸檔
     * 注意：不使用 @Transactional，因為這是從其他 @Transactional 方法調用的
     */
    public void setReadOnly(Long swapId) {
        try {
            Optional<ChatRoom> chatRoom = chatRoomRepository.findBySwapId(swapId);
            if (chatRoom.isPresent()) {
                ChatRoom room = chatRoom.get();
                room.setIsReadOnly(true);
                room.setReadOnlySince(LocalDateTime.now());
                room.setStatus(ChatRoom.ChatRoomStatus.READ_ONLY);
                chatRoomRepository.save(room);
                
                // 創建系統通知消息
                createSystemMessage(room.getId(), "🔒 交換已完成！聊天室已設為唯讀模式，可查看歷史記錄但無法發送新消息。");
                
                logger.info("Set chat room to read-only for swap: {}, room ID: {}", swapId, room.getId());
            } else {
                logger.warn("No chat room found for swap: {}, cannot set read-only", swapId);
            }
        } catch (Exception e) {
            // 不拋出異常，避免影響父事務
            logger.error("Error setting chat room to read-only for swap: {}", swapId, e);
        }
    }
    
    /**
     * 發送面交相關的系統消息到聊天室
     * 注意：不使用 @Transactional，因為這是從其他 @Transactional 方法調用的
     * 如果找不到 chat room 不應該影響父事務
     */
    public void sendMeetupSystemMessage(Long swapId, String message) {
        try {
            Optional<ChatRoom> chatRoom = chatRoomRepository.findBySwapId(swapId);
            if (chatRoom.isPresent()) {
                createSystemMessage(chatRoom.get().getId(), message);
                logger.info("Sent meetup system message to chat room for swap: {}", swapId);
            } else {
                logger.warn("No chat room found for swap: {}, cannot send meetup system message", swapId);
            }
        } catch (Exception e) {
            // 不拋出異常，避免影響父事務
            logger.error("Error sending meetup system message for swap: {}", swapId, e);
        }
    }
    
    /**
     * 檢查聊天室是否可以發送消息
     */
    public boolean canSendMessage(Long chatRoomId) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(chatRoomId);
        if (chatRoom.isEmpty()) {
            return false;
        }
        
        ChatRoom room = chatRoom.get();
        // 只有 ACTIVE 狀態且非唯讀才能發送消息
        return room.getStatus() == ChatRoom.ChatRoomStatus.ACTIVE && !room.getIsReadOnly();
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
        // 檢查是否可以發送消息
        if (!canSendMessage(chatRoomId)) {
            throw new IllegalStateException("此聊天室已設為唯讀，無法發送新消息");
        }
        
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
        // 檢查是否可以發送消息
        if (!canSendMessage(chatRoomId)) {
            throw new IllegalStateException("此聊天室已設為唯讀，無法發送新消息");
        }
        
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
        
        // 通過 WebSocket 廣播系統消息
        try {
            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId, 
                saved
            );
            logger.info("Broadcasted system message to chat room: {}", chatRoomId);
        } catch (Exception e) {
            logger.error("Failed to broadcast system message via WebSocket", e);
        }
        
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
    
    /**
     * 獲取用戶的聊天室列表（豐富版本，包含對方用戶名、物品資訊和未讀數量）
     */
    public List<ChatRoomListDTO> getEnrichedChatRooms(Long userId) {
        List<ChatRoom> chatRooms = getUserChatRooms(userId);
        if (chatRooms.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批次獲取所有需要的資料
        List<Long> proposalIds = chatRooms.stream()
                .map(ChatRoom::getProposalId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        List<Long> userIds = chatRooms.stream()
                .flatMap(room -> List.of(room.getUserAId(), room.getUserBId()).stream())
                .distinct()
                .collect(Collectors.toList());
        
        // 批次查詢所有用戶
        List<User> users = userIds.isEmpty() ? new ArrayList<>() : userRepository.findAllById(userIds);
        java.util.Map<Long, String> userNameMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));
        
        // 批次查詢所有提案（帶 JOIN FETCH）
        java.util.Map<Long, Proposal> proposalMap = new java.util.HashMap<>();
        for (Long proposalId : proposalIds) {
            proposalRepository.findByIdWithItems(proposalId).ifPresent(p -> proposalMap.put(proposalId, p));
        }
        
        // 批次查詢所有聊天室的未讀數量
        java.util.Map<Long, Long> unreadCountMap = new java.util.HashMap<>();
        for (ChatRoom room : chatRooms) {
            long count = getUnreadMessageCount(room.getId(), userId);
            unreadCountMap.put(room.getId(), count);
        }
        
        // 組裝 DTO
        List<ChatRoomListDTO> enrichedRooms = new ArrayList<>();
        for (ChatRoom room : chatRooms) {
            try {
                // 確定對方用戶ID
                Long otherUserId = room.getUserAId().equals(userId) ? room.getUserBId() : room.getUserAId();
                String otherUserName = userNameMap.getOrDefault(otherUserId, "未知使用者");
                
                // 獲取物品資訊摘要
                String itemsSummary = "";
                if (room.getProposalId() != null && proposalMap.containsKey(room.getProposalId())) {
                    Proposal proposal = proposalMap.get(room.getProposalId());
                    
                    // 獲取提案者和接收者的物品列表
                    List<String> proposerItems = proposal.getProposalItems().stream()
                            .filter(item -> item.getSide() == com.exchange.platform.entity.ProposalItem.Side.OFFERED)
                            .map(item -> item.getListing().getCardName())
                            .collect(Collectors.toList());
                    
                    List<String> receiverItems = proposal.getProposalItems().stream()
                            .filter(item -> item.getSide() == com.exchange.platform.entity.ProposalItem.Side.REQUESTED)
                            .map(item -> item.getListing().getCardName())
                            .collect(Collectors.toList());
                    
                    // 判斷當前用戶是提案者還是接收者
                    if (proposal.getProposerId().equals(userId)) {
                        // 當前用戶是提案者
                        itemsSummary = String.format("你的: %s ⇄ 對方的: %s",
                                proposerItems.isEmpty() ? "無" : String.join(", ", proposerItems),
                                receiverItems.isEmpty() ? "無" : String.join(", ", receiverItems));
                    } else {
                        // 當前用戶是接收者
                        itemsSummary = String.format("你的: %s ⇄ 對方的: %s",
                                receiverItems.isEmpty() ? "無" : String.join(", ", receiverItems),
                                proposerItems.isEmpty() ? "無" : String.join(", ", proposerItems));
                    }
                }
                
                // 創建DTO
                ChatRoomListDTO dto = new ChatRoomListDTO(
                        room.getId(),
                        room.getProposalId(),
                        room.getSwapId(),
                        otherUserName,
                        itemsSummary,
                        unreadCountMap.getOrDefault(room.getId(), 0L),
                        room.getLastMessageAt(),
                        room.getStatus().name()
                );
                
                enrichedRooms.add(dto);
            } catch (Exception e) {
                logger.error("Error enriching chat room: {}", room.getId(), e);
                // 如果出錯，仍然添加基本信息
                enrichedRooms.add(new ChatRoomListDTO(
                        room.getId(),
                        room.getProposalId(),
                        room.getSwapId(),
                        "未知使用者",
                        "無法載入物品資訊",
                        0L,
                        room.getLastMessageAt(),
                        room.getStatus().name()
                ));
            }
        }
        
        return enrichedRooms;
    }
}
