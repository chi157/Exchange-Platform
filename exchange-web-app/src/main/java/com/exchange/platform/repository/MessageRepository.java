package com.exchange.platform.repository;

import com.exchange.platform.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByProposalId(Long proposalId);

    List<Message> findBySwapId(Long swapId);

    @Query("SELECT m FROM Message m WHERE m.proposal.id = :proposalId ORDER BY m.createdAt ASC")
    List<Message> findByProposalIdOrderByCreatedAt(@Param("proposalId") Long proposalId);

    @Query("SELECT m FROM Message m WHERE m.swap.id = :swapId ORDER BY m.createdAt ASC")
    List<Message> findBySwapIdOrderByCreatedAt(@Param("swapId") Long swapId);

    @Query("SELECT COUNT(m) FROM Message m WHERE (m.proposal.id = :proposalId OR m.swap.id = :swapId) AND m.isRead = false AND m.sender.id != :userId")
    Long countUnreadMessages(@Param("proposalId") Long proposalId, @Param("swapId") Long swapId, @Param("userId") Long userId);
}