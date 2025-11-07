package com.exchange.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 發送簡單的文字郵件
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * 測試郵件發送功能
     */
    public void sendTestEmail(String to) {
        String subject = "Exchange Platform - 測試郵件";
        String text = "這是一封測試郵件。\n\n" +
                     "如果您收到這封郵件，表示 Exchange Platform 的郵件系統運作正常。\n\n" +
                     "此郵件由系統自動發送，請勿回覆。";
        
        sendSimpleEmail(to, subject, text);
    }
}
