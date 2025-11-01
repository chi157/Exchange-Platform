package com.exchange.platform.repository;

import com.exchange.platform.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
	Page<Proposal> findByProposerId(Long proposerId, Pageable pageable);
	Page<Proposal> findByReceiverIdLegacy(Long receiverId, Pageable pageable);
	Page<Proposal> findByListingId(Long listingId, Pageable pageable);
	Optional<Proposal> findByProposerIdAndListingIdAndStatus(Long proposerId, Long listingId, Proposal.Status status);
}
