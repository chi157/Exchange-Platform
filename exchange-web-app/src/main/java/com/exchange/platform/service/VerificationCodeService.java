package com.exchange.platform.service;

import com.exchange.platform.entity.EmailVerification;
import com.exchange.platform.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificationCodeService {

    private final EmailVerificationRepository verificationRepository;
    private final EmailService emailService;
    
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 10;

    /**
     * 生成並發送驗證碼
     */
    public String generateAndSendCode(String email) {
        // 生成 6 位數驗證碼
        String code = generateCode();
        
        // 儲存驗證碼
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES))
                .verified(false)
                .build();
        
        verificationRepository.save(verification);
        log.info("Generated verification code for email: {}", email);
        
        // 發送 Email
        try {
            String subject = "註冊驗證碼";
            String text = String.format(
                "您的驗證碼是: %s\n\n" +
                "此驗證碼將在 %d 分鐘後失效。\n\n" +
                "如果您沒有要求此驗證碼，請忽略此郵件。",
                code, CODE_EXPIRY_MINUTES
            );
            emailService.sendSimpleEmail(email, subject, text);
            log.info("Verification code email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification code email to: {}", email, e);
            throw new RuntimeException("無法發送驗證碼郵件", e);
        }
        
        return code;
    }

    /**
     * 驗證驗證碼
     */
    public boolean verifyCode(String email, String code) {
        var verificationOpt = verificationRepository
                .findByEmailAndCodeAndVerifiedFalse(email, code);
        
        if (verificationOpt.isEmpty()) {
            log.warn("Invalid verification code for email: {}", email);
            return false;
        }
        
        EmailVerification verification = verificationOpt.get();
        
        if (verification.isExpired()) {
            log.warn("Expired verification code for email: {}", email);
            return false;
        }
        
        // 標記為已驗證
        verification.setVerified(true);
        verificationRepository.save(verification);
        
        log.info("Verification code verified for email: {}", email);
        return true;
    }

    /**
     * 生成隨機驗證碼
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 清理過期的驗證碼（可由排程任務調用）
     */
    public void cleanupExpiredCodes() {
        verificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired verification codes");
    }
}
