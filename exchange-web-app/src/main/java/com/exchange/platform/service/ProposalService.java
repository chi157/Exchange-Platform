package com.exchange.platform.service;

import com.exchange.platform.dto.CreateProposalRequest;
import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.ProposalItem;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.ProposalRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ListingRepository listingRepository;
    private final com.exchange.platform.repository.SwapRepository swapRepository;
    private static final String SESSION_USER_ID = "userId";

    public ProposalDTO create(CreateProposalRequest req, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        // Validate receiver's listing (what proposer wants)
        Listing receiverListing = listingRepository.findById(req.getListingId())
                .orElseThrow(NotFoundException::new);

        // 檢查是否為自己的物品
        if (receiverListing.getOwnerId().equals(userId)) {
            throw new ForbiddenException();
        }

        // Validate proposer's listings (what proposer offers)
        List<Listing> proposerListings = new ArrayList<>();
        for (Long listingId : req.getProposerListingIds()) {
            Listing listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new NotFoundException());
            
            // Verify proposer owns these listings
            if (!listing.getOwnerId().equals(userId)) {
                throw new ForbiddenException();
            }
            
            // Verify listings are available
            if (listing.getStatus() != Listing.Status.AVAILABLE 
                && listing.getStatus() != Listing.Status.ACTIVE) {
                throw new ConflictException();
            }
            
            proposerListings.add(listing);
        }

        // 檢查是否已經對該 listing 提出過 PENDING 提案
        boolean hasPendingProposal = proposalRepository
                .findByProposerIdAndListingIdAndStatus(userId, receiverListing.getId(), Proposal.Status.PENDING)
                .isPresent();
        if (hasPendingProposal) {
            throw new ConflictException();
        }

        // Create proposal
        Proposal p = Proposal.builder()
                .listingId(receiverListing.getId())
                .proposerId(userId)
                .message(req.getMessage())
                .status(Proposal.Status.PENDING)
                .proposalItems(new ArrayList<>())
                .build();
        
        // Set legacy receiver_id as listing owner for compatibility with existing DB
        p.setReceiverIdLegacy(receiverListing.getOwnerId());
        
        // Save proposal first to get ID
        p = proposalRepository.save(p);
        
        // Create ProposalItems for proposer's listings (what proposer offers)
        for (Listing listing : proposerListings) {
            ProposalItem item = ProposalItem.builder()
                    .proposal(p)
                    .listing(listing)
                    .side(ProposalItem.Side.OFFERED)
                    .build();
            p.getProposalItems().add(item);
        }
        
        // Create ProposalItem for receiver's listing (what proposer requests)
        ProposalItem receiverItem = ProposalItem.builder()
                .proposal(p)
                .listing(receiverListing)
                .side(ProposalItem.Side.REQUESTED)
                .build();
        p.getProposalItems().add(receiverItem);
        
        // Save again with items (cascade will save ProposalItems)
        p = proposalRepository.save(p);
        
        return toDTO(p);
    }

    public ProposalDTO accept(Long proposalId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Proposal p = proposalRepository.findById(proposalId).orElseThrow(NotFoundException::new);
        Listing listing = listingRepository.findById(p.getListingId()).orElseThrow(NotFoundException::new);
        if (!listing.getOwnerId().equals(userId)) throw new ForbiddenException();
        
        // Prevent duplicate accepts on locked/completed listings
        if (listing.getStatus() != null
            && listing.getStatus() != com.exchange.platform.entity.Listing.Status.ACTIVE
            && listing.getStatus() != com.exchange.platform.entity.Listing.Status.AVAILABLE) {
            throw new ConflictException();
        }

        // Verify all proposer's listings are still available
        for (ProposalItem item : p.getProposalItems()) {
            if (item.getSide() == ProposalItem.Side.OFFERED) {
                Listing proposerListing = item.getListing();
                if (proposerListing.getStatus() != Listing.Status.AVAILABLE 
                    && proposerListing.getStatus() != Listing.Status.ACTIVE) {
                    throw new ConflictException();
                }
            }
        }

        p.setStatus(Proposal.Status.ACCEPTED);
        proposalRepository.save(p);

        // Create Swap
        com.exchange.platform.entity.Swap swap = com.exchange.platform.entity.Swap.builder()
                .listingId(listing.getId())
                .proposalId(p.getId())
                .aUserId(listing.getOwnerId())
                .bUserId(p.getProposerId())
                .status(com.exchange.platform.entity.Swap.Status.IN_PROGRESS)
                .build();
        swapRepository.save(swap);

        // Lock all involved listings
        listing.setStatus(com.exchange.platform.entity.Listing.Status.LOCKED);
        listingRepository.save(listing);
        
        for (ProposalItem item : p.getProposalItems()) {
            if (item.getSide() == ProposalItem.Side.OFFERED) {
                Listing proposerListing = item.getListing();
                proposerListing.setStatus(Listing.Status.LOCKED);
                listingRepository.save(proposerListing);
            }
        }

        return toDTO(p);
    }

    public ProposalDTO reject(Long proposalId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Proposal p = proposalRepository.findById(proposalId).orElseThrow(NotFoundException::new);
        Listing listing = listingRepository.findById(p.getListingId()).orElseThrow(NotFoundException::new);
        if (!listing.getOwnerId().equals(userId)) throw new ForbiddenException();

        p.setStatus(Proposal.Status.REJECTED);
        return toDTO(proposalRepository.save(p));
    }

    private ProposalDTO toDTO(Proposal p) {
        // Separate items by side: OFFERED = proposer's items, REQUESTED = receiver's items
        List<ProposalDTO.ProposalItemDTO> proposerItems = p.getProposalItems().stream()
                .filter(item -> item.getSide() == ProposalItem.Side.OFFERED)
                .map(item -> ProposalDTO.ProposalItemDTO.builder()
                        .itemId(item.getId())
                        .listingId(item.getListing().getId())
                        .listingTitle(item.getListing().getTitle())
                        .side("OFFERED")
                        .build())
                .collect(Collectors.toList());
        
        List<ProposalDTO.ProposalItemDTO> receiverItems = p.getProposalItems().stream()
                .filter(item -> item.getSide() == ProposalItem.Side.REQUESTED)
                .map(item -> ProposalDTO.ProposalItemDTO.builder()
                        .itemId(item.getId())
                        .listingId(item.getListing().getId())
                        .listingTitle(item.getListing().getTitle())
                        .side("REQUESTED")
                        .build())
                .collect(Collectors.toList());
        
        return ProposalDTO.builder()
                .id(p.getId())
                .listingId(p.getListingId())
                .proposerId(p.getProposerId())
                .message(p.getMessage())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .proposerItems(proposerItems)
                .receiverItems(receiverItems)
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.List<ProposalDTO> listMine(HttpSession session, Integer page, Integer size, String sort) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();
        Pageable pageable = PageRequest.of(toPageIndex(page), toPageSize(size), parseSort(sort));
        Page<Proposal> pg = proposalRepository.findByProposerId(userId, pageable);
        return pg.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ProposalDTO> listReceived(HttpSession session, Integer page, Integer size, String sort) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();
        Pageable pageable = PageRequest.of(toPageIndex(page), toPageSize(size), parseSort(sort));
        Page<Proposal> pg = proposalRepository.findByReceiverIdLegacy(userId, pageable);
        return pg.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ProposalDTO> listByListing(Long listingId, Integer page, Integer size, String sort) {
        Pageable pageable = PageRequest.of(toPageIndex(page), toPageSize(size), parseSort(sort));
        Page<Proposal> pg = proposalRepository.findByListingId(listingId, pageable);
        return pg.stream().map(this::toDTO).toList();
    }

    private int toPageIndex(Integer page) {
        // 1-based -> 0-based
        return (page == null || page <= 1) ? 0 : page - 1;
    }

    private int toPageSize(Integer size) {
        return (size == null || size <= 0) ? 10 : Math.min(size, 100);
    }

    private Sort parseSort(String sort) {
        String prop = "createdAt";
        Sort.Direction dir = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && !parts[0].isBlank()) prop = parts[0].trim();
            if (parts.length >= 2) {
                String d = parts[1].trim().toUpperCase();
                if ("ASC".equals(d)) dir = Sort.Direction.ASC; else if ("DESC".equals(d)) dir = Sort.Direction.DESC;
            }
        }
        if (!prop.equals("createdAt") && !prop.equals("updatedAt") && !prop.equals("id")) prop = "createdAt";
        return Sort.by(dir, prop);
    }

    public static class UnauthorizedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
    public static class ConflictException extends RuntimeException {}
}
