package com.example.styloflow.dto.auth;

import com.example.styloflow.dto.user.UserResponse;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserResponse user, BusinessInfo business) {

    public record BusinessInfo(String code, String name) {
    }
}
