package com.exchange.platform.repository;

import com.exchange.platform.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    /**
     * 根據提案 ID 查找聊天室
     */
    Optional<ChatRoom> findByProposalId(Long proposalId);
    
    /**
     * 根據交換 ID 查找聊天室
     */
    Optional<ChatRoom> findBySwapId(Long swapId);
    
    /**
     * 查找用戶的所有聊天室 (作為 userA 或 userB)
     * 按最後消息時間降序排列
     */
    List<ChatRoom> findByUserAIdOrUserBIdOrderByLastMessageAtDesc(Long userAId, Long userBId);
}
