package com.exchange.platform.repository;

import com.exchange.platform.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 查找聊天室的所有消息，按發送時間升序排列
     */
    List<ChatMessage> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
    
    /**
     * 查找聊天室的最後 N 條消息
     */
    List<ChatMessage> findTop50ByChatRoomIdOrderBySentAtDesc(Long chatRoomId);
    
    /**
     * 計算聊天室中未讀消息數 (排除發送者自己的消息)
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoomId = :chatRoomId " +
           "AND m.senderId != :userId AND m.isRead = false")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, 
                            @Param("userId") Long userId);
    
    /**
     * 標記聊天室中某用戶的所有消息為已讀
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
           "WHERE m.chatRoomId = :chatRoomId AND m.senderId != :userId AND m.isRead = false")
    void markAllAsRead(@Param("chatRoomId") Long chatRoomId, 
                      @Param("userId") Long userId);
}
