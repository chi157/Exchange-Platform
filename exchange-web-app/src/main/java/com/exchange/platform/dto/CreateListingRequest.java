package com.exchange.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateListingRequest {
    @NotBlank
    private String title;
    private String description;
}
