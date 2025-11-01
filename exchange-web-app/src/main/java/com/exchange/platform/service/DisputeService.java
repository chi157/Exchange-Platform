package com.exchange.platform.service;

import com.exchange.platform.entity.Dispute;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final SwapService swapService;
    private final UserService userService;

    public Dispute createDispute(Long swapId, Long complainantId, String reason, 
                                String description, List<String> evidenceRefs) {
        Swap swap = swapService.getSwapById(swapId);
        User complainant = userService.getUserById(complainantId);
        
        if (!swap.getUserA().getId().equals(complainantId) && 
            !swap.getUserB().getId().equals(complainantId)) {
            throw new UnauthorizedAccessException("User not part of this swap");
        }
        
        swap.markAsDisputed();
        
        Dispute dispute = Dispute.builder()
                .swap(swap)
                .complainant(complainant)
                .reason(reason)
                .description(description)
                .evidenceRefs(evidenceRefs)
                .status(Dispute.DisputeStatus.OPEN)
                .build();
        
        return disputeRepository.save(dispute);
    }

    @Transactional(readOnly = true)
    public Dispute getDisputeById(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId));
    }

    @Transactional(readOnly = true)
    public List<Dispute> getOpenDisputes() {
        return disputeRepository.findByStatus(Dispute.DisputeStatus.OPEN);
    }

    public Dispute assignToAdmin(Long disputeId, Long adminId) {
        Dispute dispute = getDisputeById(disputeId);
        User admin = userService.getUserById(adminId);
        
        dispute.assignToAdmin(admin);
        return disputeRepository.save(dispute);
    }

    public Dispute resolveDispute(Long disputeId, Dispute.DisputeStatus resolution, 
                                 String resolutionText) {
        Dispute dispute = getDisputeById(disputeId);
        dispute.resolve(resolution, resolutionText);
        return disputeRepository.save(dispute);
    }
}
