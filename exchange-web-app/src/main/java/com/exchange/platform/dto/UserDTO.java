package com.exchange.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String displayName;
    private Boolean verified;
    private String roles; // comma-separated
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
