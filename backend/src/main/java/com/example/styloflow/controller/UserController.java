package com.example.styloflow.controller;

import com.example.styloflow.dto.user.ChangePasswordRequest;
import com.example.styloflow.dto.user.UserRequest;
import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.dto.user.UserSummaryResponse;
import com.example.styloflow.security.AuthenticatedUser;
import com.example.styloflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Users of the business")
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "GET /api/users — list the business users")
    public ResponseEntity<List<UserResponse>> findAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/stylists")
    @Operation(summary = "GET /api/users/stylists — active stylists")
    public ResponseEntity<List<UserSummaryResponse>> findActiveStylists() {
        return ResponseEntity.ok(userService.findActiveStylists());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "GET /api/users/{userId} — get a user")
    public ResponseEntity<UserResponse> findUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.findUserById(userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "POST /api/users — create a user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "PUT /api/users/{userId} — update a user (omitted password = unchanged)")
    public ResponseEntity<UserResponse> updateUserById(@PathVariable Long userId, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateUserById(userId, request));
    }

    @PutMapping("/me/password")
    @Operation(summary = "PUT /api/users/me/password — change the authenticated user's password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        userService.changePassword(user.id(), request);
        return ResponseEntity.noContent().build();
    }
}
