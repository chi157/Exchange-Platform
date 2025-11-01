package com.exchange.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * User Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    private String email;
    private String displayName;
    private Boolean verified;
    private Set<String> roles;
    private Integer riskScore;
    private Boolean isBlacklisted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}