package com.example.styloflow.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String businessCode,
        @NotBlank String username,
        @NotBlank String password
) {
}
