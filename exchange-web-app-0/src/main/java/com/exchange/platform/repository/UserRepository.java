package com.exchange.platform.repository;

import com.exchange.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.verified = true")
    Optional<User> findByEmailAndVerified(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.riskScore >= :threshold")
    java.util.List<User> findHighRiskUsers(@Param("threshold") Integer threshold);

    @Query("SELECT u FROM User u WHERE u.isBlacklisted = true")
    java.util.List<User> findBlacklistedUsers();
}