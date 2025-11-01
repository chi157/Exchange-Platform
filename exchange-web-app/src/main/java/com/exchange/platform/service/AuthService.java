package com.exchange.platform.service;

import com.exchange.platform.dto.AuthResponse;
import com.exchange.platform.dto.LoginRequest;
import com.exchange.platform.dto.RegisterRequest;
import com.exchange.platform.dto.UserDTO;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.BusinessRuleViolationException;
import com.exchange.platform.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 簡化版認證服務 - 學校專案用，不考慮安全性
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private static final String SESSION_USER_ID = "userId";

    public AuthResponse register(RegisterRequest request) {
        log.debug("Registering new user with email: {}", request.getEmail());
        
        // 檢查 email 是否已存在
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            throw new BusinessRuleViolationException("Email already registered");
        }
        
        // 建立新用戶 - 明碼儲存密碼
        Set<String> defaultRoles = new HashSet<>();
        defaultRoles.add("USER");
        
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .displayName(request.getDisplayName())
                .verified(false)
                .roles(defaultRoles)
                .riskScore(0)
                .isBlacklisted(false)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .displayName(savedUser.getDisplayName())
                .message("Registration successful")
                .success(true)
                .build();
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        log.debug("User login attempt: {}", request.getEmail());
        
        // 查找用戶
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid email or password"));
        
        // 明碼比對密碼
        if (!user.getPasswordHash().equals(request.getPassword())) {
            throw new BusinessRuleViolationException("Invalid email or password");
        }
        
        // 儲存到 Session
        session.setAttribute(SESSION_USER_ID, user.getId());
        
        log.info("User logged in successfully: {}", user.getId());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .message("Login successful")
                .success(true)
                .build();
    }

    public void logout(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId != null) {
            log.info("User logged out: {}", userId);
        }
        session.invalidate();
    }

    public UserDTO getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return null;
        }
        
        User user = userRepository.findById(userId)
                .orElse(null);
        
        if (user == null) {
            return null;
        }
        
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .verified(user.getVerified())
                .build();
    }
}
