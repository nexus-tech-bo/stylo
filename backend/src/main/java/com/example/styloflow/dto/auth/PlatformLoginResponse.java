package com.example.styloflow.dto.auth;

import java.time.Instant;

public record PlatformLoginResponse(String token, Instant expiresAt, String name) {
}
