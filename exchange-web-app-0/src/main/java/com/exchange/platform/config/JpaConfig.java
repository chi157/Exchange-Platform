package com.exchange.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA 設定
 * 啟用 JPA Auditing 功能以自動記錄 createdAt, updatedAt, createdBy, updatedBy
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {
    
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }
            // 假設 UserDetails 的 username 是 userId
            try {
                return Optional.of(Long.parseLong(authentication.getName()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        };
    }
}
