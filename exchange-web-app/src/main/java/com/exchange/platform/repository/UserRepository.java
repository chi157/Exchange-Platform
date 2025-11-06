package com.exchange.platform.repository;

import com.exchange.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // OAuth2 相關查詢方法
    Optional<User> findByOauth2ProviderAndOauth2Id(String oauth2Provider, String oauth2Id);
}
