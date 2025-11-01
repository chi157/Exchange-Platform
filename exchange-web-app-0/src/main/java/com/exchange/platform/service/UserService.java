package com.exchange.platform.service;

import com.exchange.platform.entity.User;
import com.exchange.platform.exception.ResourceNotFoundException;
import com.exchange.platform.exception.ValidationException;
import com.exchange.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(String email, String password, String displayName) {
        log.debug("Registering new user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            ValidationException ex = new ValidationException("User registration failed");
            ex.addFieldError("email", "Email already exists");
            throw ex;
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName)
                .verified(false)
                .verificationToken(UUID.randomUUID().toString())
                .riskScore(0)
                .isBlacklisted(false)
                .build();

        user.addRole("USER");

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        return savedUser;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<User> getHighRiskUsers(int threshold) {
        return userRepository.findHighRiskUsers(threshold);
    }
}
