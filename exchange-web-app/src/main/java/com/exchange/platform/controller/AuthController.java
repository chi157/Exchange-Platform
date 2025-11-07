package com.exchange.platform.controller;

import com.exchange.platform.dto.AuthResponse;
import com.exchange.platform.dto.LoginRequest;
import com.exchange.platform.dto.RegisterRequest;
import com.exchange.platform.dto.UserDTO;
import com.exchange.platform.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("POST /api/auth/register - email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        log.debug("POST /api/auth/login - email: {}", request.getEmail());
        AuthResponse response = authService.login(request, session);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        log.debug("POST /api/auth/logout");
        authService.logout(session);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(HttpSession session) {
        log.debug("GET /api/auth/me");
        UserDTO user = authService.getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @RequestParam String email,
            @RequestParam String code) {
        log.debug("POST /api/auth/verify-email - email: {}", email);
        AuthResponse response = authService.verifyEmail(email, code);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerificationCode(@RequestParam String email) {
        log.debug("POST /api/auth/resend-verification - email: {}", email);
        AuthResponse response = authService.resendVerificationCode(email);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
