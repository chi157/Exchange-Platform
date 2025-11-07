package com.exchange.platform.controller;

import com.exchange.platform.dto.AuthResponse;
import com.exchange.platform.dto.LoginRequest;
import com.exchange.platform.dto.RegisterRequest;
import com.exchange.platform.dto.UpdateProfileRequest;
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

    /**
     * 發送驗證碼
     */
    @PostMapping("/send-verification-code")
    public ResponseEntity<AuthResponse> sendVerificationCode(@RequestBody RegisterRequest request) {
        log.debug("POST /api/auth/send-verification-code - email: {}", request.getEmail());
        AuthResponse response = authService.sendVerificationCode(request.getEmail());
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 驗證並註冊
     */
    @PostMapping("/register-with-verification")
    public ResponseEntity<AuthResponse> registerWithVerification(@Valid @RequestBody RegisterRequest request) {
        log.debug("POST /api/auth/register-with-verification - email: {}", request.getEmail());
        AuthResponse response = authService.registerWithVerification(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 舊的註冊端點（保留相容性）
     */
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

    /**
     * 發送 Email 更改驗證碼
     */
    @PostMapping("/send-email-change-code")
    public ResponseEntity<AuthResponse> sendEmailChangeCode(@RequestBody UpdateProfileRequest request, HttpSession session) {
        log.debug("POST /api/auth/send-email-change-code");
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder().success(false).message("未登入").build());
        }
        AuthResponse response = authService.sendEmailChangeVerificationCode(userId, request.getEmail());
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 驗證並更新 Email
     */
    @PostMapping("/update-email-with-verification")
    public ResponseEntity<AuthResponse> updateEmailWithVerification(@RequestBody UpdateProfileRequest request, HttpSession session) {
        log.debug("POST /api/auth/update-email-with-verification");
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder().success(false).message("未登入").build());
        }
        AuthResponse response = authService.updateEmailWithVerification(userId, request.getEmail(), request.getVerificationCode());
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
