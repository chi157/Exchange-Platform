package com.exchange.platform.controller;

import com.exchange.platform.entity.ChatMessage;
import com.exchange.platform.entity.ChatRoom;
import com.exchange.platform.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 聊天控制器
 * 提供 REST API 和 WebSocket 消息處理
 */
@Controller
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * REST API: 獲取用戶的聊天室列表
     */
    @GetMapping("/api/chat/rooms")
    @ResponseBody
    public ResponseEntity<?> getChatRooms(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        List<ChatRoom> chatRooms = chatService.getUserChatRooms(userId);
        return ResponseEntity.ok(chatRooms);
    }
    
    /**
     * REST API: 根據 Proposal ID 獲取聊天室
     */
    @GetMapping("/api/chat/room/proposal/{proposalId}")
    @ResponseBody
    public ResponseEntity<?> getChatRoomByProposal(@PathVariable Long proposalId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        Optional<ChatRoom> chatRoom = chatService.getChatRoomByProposalId(proposalId);
        if (chatRoom.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "聊天室不存在"));
        }
        
        // 驗證權限
        if (!chatService.hasAccessToChatRoom(chatRoom.get().getId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "無權訪問此聊天室"));
        }
        
        return ResponseEntity.ok(chatRoom.get());
    }
    
    /**
     * REST API: 獲取聊天室的消息歷史
     */
    @GetMapping("/api/chat/room/{chatRoomId}/messages")
    @ResponseBody
    public ResponseEntity<?> getChatMessages(@PathVariable Long chatRoomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        // 驗證權限
        if (!chatService.hasAccessToChatRoom(chatRoomId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "無權訪問此聊天室"));
        }
        
        List<ChatMessage> messages = chatService.getChatRoomMessages(chatRoomId);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * REST API: 標記消息為已讀
     */
    @PostMapping("/api/chat/room/{chatRoomId}/read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Long chatRoomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        // 驗證權限
        if (!chatService.hasAccessToChatRoom(chatRoomId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "無權訪問此聊天室"));
        }
        
        chatService.markMessagesAsRead(chatRoomId, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * REST API: 獲取未讀消息數量
     */
    @GetMapping("/api/chat/room/{chatRoomId}/unread")
    @ResponseBody
    public ResponseEntity<?> getUnreadCount(@PathVariable Long chatRoomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        // 驗證權限
        if (!chatService.hasAccessToChatRoom(chatRoomId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "無權訪問此聊天室"));
        }
        
        long count = chatService.getUnreadMessageCount(chatRoomId, userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
    
    /**
     * WebSocket: 處理發送的文字消息
     * 客戶端發送到: /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload) {
        try {
            Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            String content = payload.get("content").toString();
            
            // 保存消息
            ChatMessage message = chatService.sendTextMessage(chatRoomId, senderId, content);
            
            // 廣播消息到聊天室
            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId, 
                message
            );
            
            logger.info("Message sent via WebSocket: chatRoom={}, sender={}", chatRoomId, senderId);
            
        } catch (IllegalStateException e) {
            // 聊天室為唯讀狀態
            logger.warn("Cannot send message to read-only chat room: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                payload.get("senderId").toString(),
                "/queue/errors",
                Map.of("error", "此聊天室已設為唯讀，無法發送消息")
            );
        } catch (Exception e) {
            logger.error("Error sending message via WebSocket", e);
            messagingTemplate.convertAndSendToUser(
                payload.get("senderId").toString(),
                "/queue/errors",
                Map.of("error", "發送消息失敗")
            );
        }
    }
    
    /**
     * WebSocket: 處理發送的圖片消息
     * 客戶端發送到: /app/chat.sendImage
     */
    @MessageMapping("/chat.sendImage")
    public void sendImage(@Payload Map<String, Object> payload) {
        try {
            Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            String imageUrl = payload.get("imageUrl").toString();
            
            // 保存消息
            ChatMessage message = chatService.sendImageMessage(chatRoomId, senderId, imageUrl);
            
            // 廣播消息到聊天室
            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId, 
                message
            );
            
            logger.info("Image sent via WebSocket: chatRoom={}, sender={}", chatRoomId, senderId);
            
        } catch (IllegalStateException e) {
            // 聊天室為唯讀狀態
            logger.warn("Cannot send image to read-only chat room: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                payload.get("senderId").toString(),
                "/queue/errors",
                Map.of("error", "此聊天室已設為唯讀，無法發送圖片")
            );
        } catch (Exception e) {
            logger.error("Error sending image via WebSocket", e);
            messagingTemplate.convertAndSendToUser(
                payload.get("senderId").toString(),
                "/queue/errors",
                Map.of("error", "發送圖片失敗")
            );
        }
    }
    
    /**
     * WebSocket: 用戶進入聊天室
     * 客戶端發送到: /app/chat.join
     */
    @MessageMapping("/chat.join")
    public void joinChatRoom(@Payload Map<String, Object> payload) {
        try {
            Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            
            // 標記消息為已讀
            chatService.markMessagesAsRead(chatRoomId, userId);
            
            logger.info("User joined chat room: user={}, chatRoom={}", userId, chatRoomId);
            
        } catch (Exception e) {
            logger.error("Error joining chat room via WebSocket", e);
        }
    }
}
