package com.exchange.platform.repository;

import com.exchange.platform.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
}
