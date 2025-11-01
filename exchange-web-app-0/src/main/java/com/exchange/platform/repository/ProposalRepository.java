package com.exchange.platform.repository;

import com.exchange.platform.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findByProposerId(Long proposerId);

    List<Proposal> findByReceiverId(Long receiverId);

    List<Proposal> findByStatus(Proposal.ProposalStatus status);

    @Query("SELECT p FROM Proposal p WHERE (p.proposer.id = :userId OR p.receiver.id = :userId) AND p.status = :status")
    List<Proposal> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Proposal.ProposalStatus status);

    @Query("SELECT p FROM Proposal p WHERE p.expiresAt < :now AND p.status = 'PENDING'")
    List<Proposal> findExpiredProposals(@Param("now") LocalDateTime now);
    
    // 根據狀態和過期時間查詢提案
    List<Proposal> findByStatusAndExpiresAtBefore(Proposal.ProposalStatus status, LocalDateTime dateTime);
}