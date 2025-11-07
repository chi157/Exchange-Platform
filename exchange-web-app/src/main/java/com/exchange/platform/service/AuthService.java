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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationCodeService verificationCodeService;
    private static final String SESSION_USER_ID = "userId";

    /**
     * 第一步：發送驗證碼
     */
    public AuthResponse sendVerificationCode(String email) {
        log.debug("Sending verification code to email: {}", email);

        // 檢查 Email 是否已被註冊
        if (userRepository.existsByEmail(email)) {
            return AuthResponse.builder()
                    .success(false)
                    .message("此 Email 已被註冊")
                    .build();
        }

        try {
            verificationCodeService.generateAndSendCode(email);
            return AuthResponse.builder()
                    .success(true)
                    .message("驗證碼已發送至您的信箱")
                    .build();
        } catch (Exception e) {
            log.error("Failed to send verification code", e);
            return AuthResponse.builder()
                    .success(false)
                    .message("無法發送驗證碼，請稍後再試")
                    .build();
        }
    }

    /**
     * 第二步：驗證驗證碼並完成註冊
     */
    public AuthResponse registerWithVerification(RegisterRequest request) {
        log.debug("Registering user with verification: {}", request.getEmail());

        // 驗證驗證碼
        if (!verificationCodeService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("驗證碼錯誤或已過期")
                    .build();
        }

        // 再次檢查 Email（避免競爭條件）
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("此 Email 已被註冊")
                    .build();
        }

        // 建立使用者
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword()) // 明碼存放（示範用）
                .displayName(request.getDisplayName())
                .verified(true) // 已驗證
                .roles("USER")
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with email verification: {}", user.getId());

        return AuthResponse.builder()
                .success(true)
                .message("註冊成功")
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    /**
     * 舊的註冊方法（保留相容性）
     */
    @Deprecated
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email already registered")
                    .build();
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword()) // 明碼存放（示範用）
                .displayName(request.getDisplayName())
                .verified(false)
                .roles("USER")
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        return AuthResponse.builder()
                .success(true)
                .message("Registration successful")
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
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

    /**
     * 發送 Email 更改驗證碼
     */
    public AuthResponse sendEmailChangeVerificationCode(Long userId, String newEmail) {
        log.debug("Sending email change verification code to: {}", newEmail);

        // 檢查用戶是否存在
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("用戶不存在")
                    .build();
        }

        // 檢查新 Email 是否已被其他用戶使用
        Optional<User> existingUser = userRepository.findByEmail(newEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            return AuthResponse.builder()
                    .success(false)
                    .message("此 Email 已被其他使用者使用")
                    .build();
        }

        try {
            verificationCodeService.generateAndSendCode(newEmail);
            return AuthResponse.builder()
                    .success(true)
                    .message("驗證碼已發送至新的 Email")
                    .build();
        } catch (Exception e) {
            log.error("Failed to send email change verification code", e);
            return AuthResponse.builder()
                    .success(false)
                    .message("無法發送驗證碼，請稍後再試")
                    .build();
        }
    }

    /**
     * 驗證並更新 Email
     */
    public AuthResponse updateEmailWithVerification(Long userId, String newEmail, String verificationCode) {
        log.debug("Updating email with verification for user: {}", userId);

        // 驗證驗證碼
        if (!verificationCodeService.verifyCode(newEmail, verificationCode)) {
            return AuthResponse.builder()
                    .success(false)
                    .message("驗證碼錯誤或已過期")
                    .build();
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("用戶不存在")
                    .build();
        }

        User user = userOpt.get();

        // 再次檢查新 Email（避免競爭條件）
        Optional<User> existingUser = userRepository.findByEmail(newEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            return AuthResponse.builder()
                    .success(false)
                    .message("此 Email 已被其他使用者使用")
                    .build();
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        log.info("Email updated for user: {}", userId);

        return AuthResponse.builder()
                .success(true)
                .message("Email 更新成功")
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
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

        // Email 更改需要通過驗證碼流程，這裡不處理
        // 只處理密碼和顯示名稱

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
