package com.exchange.platform.service;

import com.exchange.platform.dto.AuthResponse;
import com.exchange.platform.dto.LoginRequest;
import com.exchange.platform.dto.RegisterRequest;
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
    private static final String SESSION_USER_ID = "userId";

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
}
