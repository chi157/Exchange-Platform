package com.exchange.platform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String email;
    private String displayName;
}
