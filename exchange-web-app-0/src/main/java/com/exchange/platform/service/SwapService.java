package com.exchange.platform.service;

import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.SwapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SwapService {

    private final SwapRepository swapRepository;
    private final ProposalService proposalService;
    private final ListingService listingService;

    public Swap createSwap(Long proposalId) {
        Proposal proposal = proposalService.getProposalById(proposalId);
        
        if (proposal.getStatus() != Proposal.ProposalStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Proposal must be accepted first");
        }
        
        Swap swap = Swap.builder()
                .proposal(proposal)
                .userA(proposal.getProposer())
                .userB(proposal.getReceiver())
                .status(Swap.SwapStatus.SHIPPING)
                .receivedAConfirmed(false)
                .receivedBConfirmed(false)
                .build();
        
        Swap savedSwap = swapRepository.save(swap);
        
        for (var item : proposal.getProposalItems()) {
            listingService.markAsTraded(item.getListing().getId());
        }
        
        return savedSwap;
    }

    @Transactional(readOnly = true)
    public Swap getSwapById(Long swapId) {
        return swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap", swapId));
    }

    @Transactional(readOnly = true)
    public List<Swap> getUserSwaps(Long userId) {
        return swapRepository.findByUserAIdOrUserBId(userId, userId);
    }

    public Swap confirmReceived(Long swapId, Long userId) {
        Swap swap = getSwapById(swapId);
        
        if (swap.getUserA().getId().equals(userId)) {
            swap.confirmReceivedByUserA();
        } else if (swap.getUserB().getId().equals(userId)) {
            swap.confirmReceivedByUserB();
        } else {
            throw new UnauthorizedAccessException("User not part of this swap");
        }
        
        return swapRepository.save(swap);
    }
}
