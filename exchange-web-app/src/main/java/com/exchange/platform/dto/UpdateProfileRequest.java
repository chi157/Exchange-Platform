package com.exchange.platform.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String email;
    private String displayName;
    private String currentPassword;
    private String newPassword;
    private String verificationCode; // Email 更改驗證碼
}
