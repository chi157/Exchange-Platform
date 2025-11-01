package com.exchange.platform.service;

import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.ProposalItem;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final UserService userService;
    private final ListingService listingService;

    public Proposal createProposal(Long proposerId, List<Long> proposerListingIds,
                                   List<Long> receiverListingIds, String message) {
        User proposer = userService.getUserById(proposerId);
        
        if (receiverListingIds == null || receiverListingIds.isEmpty()) {
            throw new ValidationException("Must select at least one card");
        }
        
        Listing firstReceiverListing = listingService.getListingById(receiverListingIds.get(0));
        User receiver = firstReceiverListing.getUser();
        
        if (proposer.getId().equals(receiver.getId())) {
            throw new BusinessRuleViolationException("Cannot exchange with yourself");
        }
        
        Proposal proposal = Proposal.builder()
                .proposer(proposer)
                .receiver(receiver)
                .status(Proposal.ProposalStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .message(message)
                .build();
        
        if (proposerListingIds != null) {
            for (Long listingId : proposerListingIds) {
                Listing listing = listingService.getListingById(listingId);
                
                // 檢查並鎖定提案者的卡片
                if (!listing.isAvailable()) {
                    throw new BusinessRuleViolationException(
                        "Listing ID " + listingId + " is not available for proposal");
                }
                
                ProposalItem item = ProposalItem.builder()
                        .proposal(proposal)
                        .listing(listing)
                        .side(ProposalItem.Side.PROPOSER)
                        .build();
                proposal.addProposalItem(item);
            }
        }
        
        for (Long listingId : receiverListingIds) {
            Listing listing = listingService.getListingById(listingId);
            
            // 檢查接收者的卡片是否可用
            if (!listing.isAvailable()) {
                throw new BusinessRuleViolationException(
                    "Listing ID " + listingId + " is not available for proposal");
            }
            
            ProposalItem item = ProposalItem.builder()
                    .proposal(proposal)
                    .listing(listing)
                    .side(ProposalItem.Side.RECEIVER)
                    .build();
            proposal.addProposalItem(item);
        }
        
        Proposal savedProposal = proposalRepository.save(proposal);
        
        // 鎖定所有提案者選擇的卡片
        if (proposerListingIds != null) {
            for (Long listingId : proposerListingIds) {
                listingService.lockListing(listingId, savedProposal.getId());
            }
        }
        
        return savedProposal;
    }

    @Transactional(readOnly = true)
    public Proposal getProposalById(Long proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));
    }

    @Transactional(readOnly = true)
    public List<Proposal> getReceivedProposals(Long userId) {
        return proposalRepository.findByReceiverId(userId);
    }

    @Transactional(readOnly = true)
    public List<Proposal> getSentProposals(Long userId) {
        return proposalRepository.findByProposerId(userId);
    }

    public Proposal acceptProposal(Long proposalId, Long userId) {
        Proposal proposal = getProposalById(proposalId);
        
        if (!proposal.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Only receiver can accept");
        }
        
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING) {
            throw new InvalidStateTransitionException("Proposal", proposal.getStatus().toString());
        }
        
        proposal.accept();
        return proposalRepository.save(proposal);
    }

    public Proposal rejectProposal(Long proposalId, Long userId) {
        Proposal proposal = getProposalById(proposalId);
        
        if (!proposal.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Only receiver can reject");
        }
        
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING) {
            throw new InvalidStateTransitionException("Proposal", proposal.getStatus().toString());
        }
        
        proposal.reject();
        Proposal savedProposal = proposalRepository.save(proposal);
        
        // 解鎖所有提案者的卡片
        unlockProposalListings(proposal);
        
        return savedProposal;
    }

    public void cancelProposal(Long proposalId, Long userId) {
        Proposal proposal = getProposalById(proposalId);
        
        if (!proposal.getProposer().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Only proposer can cancel");
        }
        
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING) {
            throw new InvalidStateTransitionException("Proposal", proposal.getStatus().toString());
        }
        
        proposal.cancel();
        proposalRepository.save(proposal);
        
        // 解鎖所有提案者的卡片
        unlockProposalListings(proposal);
    }
    
    // 解鎖提案相關的所有卡片
    private void unlockProposalListings(Proposal proposal) {
        for (ProposalItem item : proposal.getProposalItems()) {
            if (item.getSide() == ProposalItem.Side.PROPOSER) {
                listingService.unlockListing(item.getListing().getId());
            }
        }
    }
    
    /**
     * 定時任務：每小時檢查並處理過期的提案
     * 將過期的 PENDING 提案標記為 EXPIRED 並解鎖相關卡片
     */
    @Scheduled(fixedRate = 3600000) // 每小時執行一次 (3600000 ms = 1 hour)
    public void expireOverdueProposals() {
        LocalDateTime now = LocalDateTime.now();
        List<Proposal> expiredProposals = proposalRepository
            .findByStatusAndExpiresAtBefore(Proposal.ProposalStatus.PENDING, now);
        
        if (!expiredProposals.isEmpty()) {
            log.info("發現 {} 個過期提案，開始處理", expiredProposals.size());
            
            for (Proposal proposal : expiredProposals) {
                try {
                    // 標記為過期
                    proposal.cancel(); // 使用 cancel() 來標記過期狀態
                    proposalRepository.save(proposal);
                    
                    // 解鎖所有相關卡片
                    unlockProposalListings(proposal);
                    
                    log.info("提案 ID {} 已過期並解鎖相關卡片", proposal.getId());
                } catch (Exception e) {
                    log.error("處理過期提案 ID {} 時發生錯誤: {}", proposal.getId(), e.getMessage(), e);
                }
            }
            
            log.info("過期提案處理完成，共處理 {} 個", expiredProposals.size());
        }
    }
}

