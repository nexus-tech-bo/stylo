package com.example.styloflow.controller;

import com.example.styloflow.dto.business.BusinessRequest;
import com.example.styloflow.dto.business.BusinessResponse;
import com.example.styloflow.security.AuthenticatedUser;
import com.example.styloflow.service.BusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@Tag(name = "Business", description = "Settings of the business of the token")
public class BusinessController {
    private final BusinessService businessService;

    @GetMapping
    @Operation(summary = "GET /api/business — the business of the token")
    public ResponseEntity<BusinessResponse> findCurrentBusiness(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(businessService.findBusinessById(user.businessId()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "PUT /api/business — update the business settings")
    public ResponseEntity<BusinessResponse> updateCurrentBusiness(@Valid @RequestBody BusinessRequest request,
                                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(businessService.updateBusinessById(user.businessId(), request));
    }
}
