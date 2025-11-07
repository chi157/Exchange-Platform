package com.exchange.platform.service;

import com.exchange.platform.dto.AuthResponse;
import com.exchange.platform.dto.LoginRequest;
import com.exchange.platform.dto.RegisterRequest;
import com.exchange.platform.dto.UpdateProfileRequest;
import com.exchange.platform.dto.UserDTO;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private static final String SESSION_USER_ID = "userId";

    public AuthResponse register(RegisterRequest request) {
        log.debug("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email already registered")
                    .build();
        }

        // 生成 6 位數驗證碼
        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword()) // 明碼存放（示範用）
                .displayName(request.getDisplayName())
                .verified(false)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(expiresAt)
                .roles("USER")
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        // 發送驗證碼郵件
        try {
            emailNotificationService.sendVerificationCode(
                    user.getEmail(), 
                    verificationCode, 
                    "REGISTER"
            );
            log.info("Verification code sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification code: {}", e.getMessage());
        }

        return AuthResponse.builder()
                .success(true)
                .message("Registration successful. Please check your email for verification code.")
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    /**
     * 生成 6 位數驗證碼
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 驗證驗證碼
     */
    public AuthResponse verifyEmail(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("User not found")
                    .build();
        }

        User user = userOpt.get();

        // 檢查是否已驗證
        if (user.getVerified()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email already verified")
                    .build();
        }

        // 檢查驗證碼是否正確
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid verification code")
                    .build();
        }

        // 檢查驗證碼是否過期
        if (user.getVerificationCodeExpiresAt() == null || 
            LocalDateTime.now().isAfter(user.getVerificationCodeExpiresAt())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Verification code expired")
                    .build();
        }

        // 驗證成功
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getId());

        return AuthResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    /**
     * 重新發送驗證碼
     */
    public AuthResponse resendVerificationCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("User not found")
                    .build();
        }

        User user = userOpt.get();

        if (user.getVerified()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email already verified")
                    .build();
        }

        // 生成新的驗證碼
        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(expiresAt);
        userRepository.save(user);

        // 發送驗證碼郵件
        try {
            emailNotificationService.sendVerificationCode(
                    user.getEmail(), 
                    verificationCode, 
                    "REGISTER"
            );
            log.info("Verification code resent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to resend verification code: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("Failed to send verification code")
                    .build();
        }

        return AuthResponse.builder()
                .success(true)
                .message("Verification code resent successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        log.debug("User login attempt: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty() || !userOpt.get().getPasswordHash().equals(request.getPassword())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
        }

        User user = userOpt.get();
        
        // 檢查郵箱是否已驗證
        if (!user.getVerified()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Please verify your email before logging in")
                    .build();
        }

        session.setAttribute(SESSION_USER_ID, user.getId());
        log.info("User logged in successfully: {}", user.getId());

        return AuthResponse.builder()
                .success(true)
                .message("Login successful")
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    public void logout(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId != null) {
            log.info("User logged out: {}", userId);
        }
        session.invalidate();
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return null;

        return userRepository.findById(userId)
                .map(u -> UserDTO.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .displayName(u.getDisplayName())
                        .verified(u.getVerified())
                        .roles(u.getRoles())
                        .createdAt(u.getCreatedAt())
                        .updatedAt(u.getUpdatedAt())
                        .build())
                .orElse(null);
    }

    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.debug("Updating profile for user: {}", userId);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("User not found")
                    .build();
        }

        User user = userOpt.get();

        // 如果要更改密碼，需要驗證當前密碼
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getCurrentPassword() == null || 
                !user.getPasswordHash().equals(request.getCurrentPassword())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("當前密碼不正確")
                        .build();
            }
            user.setPasswordHash(request.getNewPassword());
            log.info("Password updated for user: {}", userId);
        }

        // 更新顯示名稱
        if (request.getDisplayName() != null && !request.getDisplayName().isEmpty()) {
            user.setDisplayName(request.getDisplayName());
            log.info("Display name updated for user: {}", userId);
        }

        userRepository.save(user);

        return AuthResponse.builder()
                .success(true)
                .message("個人資料更新成功")
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }
}
