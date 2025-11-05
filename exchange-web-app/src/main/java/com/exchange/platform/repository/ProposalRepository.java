package com.exchange.platform.repository;

import com.exchange.platform.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
	@Query("SELECT DISTINCT p FROM Proposal p LEFT JOIN FETCH p.proposalItems pi LEFT JOIN FETCH pi.listing WHERE p.proposerId = :proposerId")
	Page<Proposal> findByProposerIdWithItems(@Param("proposerId") Long proposerId, Pageable pageable);
	
	@Query("SELECT DISTINCT p FROM Proposal p LEFT JOIN FETCH p.proposalItems pi LEFT JOIN FETCH pi.listing WHERE p.receiverId = :receiverId")
	Page<Proposal> findByReceiverIdWithItems(@Param("receiverId") Long receiverId, Pageable pageable);
	
	Page<Proposal> findByProposerId(Long proposerId, Pageable pageable);
	Page<Proposal> findByReceiverId(Long receiverId, Pageable pageable);
	Page<Proposal> findByListingId(Long listingId, Pageable pageable);
	Optional<Proposal> findByProposerIdAndListingIdAndStatus(Long proposerId, Long listingId, Proposal.Status status);
}
