package com.exchange.platform.controller;

import com.exchange.platform.dto.CreateListingRequest;
import com.exchange.platform.dto.ListingDTO;
import com.exchange.platform.service.ListingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final com.exchange.platform.service.ProposalService proposalService;

    @PostMapping
    public ResponseEntity<ListingDTO> create(@Valid @RequestBody CreateListingRequest request, HttpSession session) {
        ListingDTO dto = listingService.create(request, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDTO> getById(@PathVariable Long id) {
        ListingDTO dto = listingService.getById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<ListingDTO>> list(@RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(listingService.list(page, size, q, sort));
    }

    @GetMapping("/{id}/proposals")
    public ResponseEntity<java.util.List<com.exchange.platform.dto.ProposalDTO>> listProposalsByListing(@PathVariable Long id,
                                                                                                         @RequestParam(required = false) Integer page,
                                                                                                         @RequestParam(required = false) Integer size,
                                                                                                         @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(this.proposalService.listByListing(id, page, size, sort));
    }

    @ExceptionHandler(ListingService.UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(ListingService.NotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
