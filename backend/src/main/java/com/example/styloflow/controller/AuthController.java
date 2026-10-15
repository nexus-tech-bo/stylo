package com.example.styloflow.controller;

import com.example.styloflow.dto.auth.LoginRequest;
import com.example.styloflow.dto.auth.LoginResponse;
import com.example.styloflow.dto.auth.PlatformLoginRequest;
import com.example.styloflow.dto.auth.PlatformLoginResponse;
import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.security.AuthenticatedUser;
import com.example.styloflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Business and platform login")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/login")
    @Operation(summary = "POST /api/auth/login — log in to a business")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/auth/me")
    @Operation(summary = "GET /api/auth/me — the authenticated business user")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(authService.findCurrentUser(user.id()));
    }

    @PostMapping("/platform/auth/login")
    @Operation(summary = "POST /api/platform/auth/login — log in as platform super admin")
    public ResponseEntity<PlatformLoginResponse> platformLogin(@Valid @RequestBody PlatformLoginRequest request) {
        return ResponseEntity.ok(authService.platformLogin(request));
    }
}
