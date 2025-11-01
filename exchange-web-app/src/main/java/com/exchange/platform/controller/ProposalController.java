package com.exchange.platform.controller;

import com.exchange.platform.dto.CreateProposalRequest;
import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.service.ProposalService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @PostMapping
    public ResponseEntity<ProposalDTO> create(@Valid @RequestBody CreateProposalRequest request, HttpSession session) {
        ProposalDTO dto = proposalService.create(request, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ProposalDTO> accept(@PathVariable Long id, HttpSession session) {
        return ResponseEntity.ok(proposalService.accept(id, session));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ProposalDTO> reject(@PathVariable Long id, HttpSession session) {
        return ResponseEntity.ok(proposalService.reject(id, session));
    }

    @ExceptionHandler(ProposalService.UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized() { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }

    @ExceptionHandler(ProposalService.ForbiddenException.class)
    public ResponseEntity<Void> handleForbidden() { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

    @ExceptionHandler(ProposalService.NotFoundException.class)
    public ResponseEntity<Void> handleNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
}
