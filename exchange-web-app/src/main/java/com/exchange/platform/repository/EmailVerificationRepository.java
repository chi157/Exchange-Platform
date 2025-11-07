package com.exchange.platform.repository;

import com.exchange.platform.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    
    Optional<EmailVerification> findByEmailAndCodeAndVerifiedFalse(String email, String code);
    
    List<EmailVerification> findByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
