package com.example.styloflow.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record PlatformLoginRequest(@NotBlank String username, @NotBlank String password) {
}
