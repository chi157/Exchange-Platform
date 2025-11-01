package com.exchange.platform.controller;

import com.exchange.platform.dto.SwapDTO;
import com.exchange.platform.service.SwapService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/swaps")
@RequiredArgsConstructor
public class SwapController {

    private final SwapService swapService;

    @GetMapping("/mine")
    public ResponseEntity<List<SwapDTO>> listMine(@RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size,
                                                  @RequestParam(required = false) String sort,
                                                  HttpSession session) {
        return ResponseEntity.ok(swapService.listMine(session, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SwapDTO> getById(@PathVariable Long id, HttpSession session) {
        return ResponseEntity.ok(swapService.getById(id, session));
    }

    @ExceptionHandler(SwapService.UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized() { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }

    @ExceptionHandler(SwapService.ForbiddenException.class)
    public ResponseEntity<Void> handleForbidden() { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

    @ExceptionHandler(SwapService.NotFoundException.class)
    public ResponseEntity<Void> handleNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
}
