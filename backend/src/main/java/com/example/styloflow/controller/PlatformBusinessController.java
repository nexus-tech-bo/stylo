package com.example.styloflow.controller;

import com.example.styloflow.dto.platform.BusinessSummaryResponse;
import com.example.styloflow.dto.platform.CreateBusinessRequest;
import com.example.styloflow.dto.platform.StatusRequest;
import com.example.styloflow.service.PlatformBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/businesses")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
@Tag(name = "Platform", description = "Business management (SUPERADMIN)")
public class PlatformBusinessController {
    private final PlatformBusinessService platformBusinessService;

    @GetMapping
    @Operation(summary = "GET /api/platform/businesses — businesses with 30-day activity")
    public ResponseEntity<List<BusinessSummaryResponse>> findAllBusinesses() {
        return ResponseEntity.ok(platformBusinessService.findAllBusinesses());
    }

    @PostMapping
    @Operation(summary = "POST /api/platform/businesses — create a business and its ADMIN user")
    public ResponseEntity<BusinessSummaryResponse> createBusiness(@Valid @RequestBody CreateBusinessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformBusinessService.createBusiness(request));
    }

    @PutMapping("/{businessId}/status")
    @Operation(summary = "PUT /api/platform/businesses/{businessId}/status — suspend or reactivate a business")
    public ResponseEntity<Void> updateBusinessStatus(@PathVariable Long businessId, @Valid @RequestBody StatusRequest request) {
        platformBusinessService.updateBusinessStatus(businessId, request);
        return ResponseEntity.noContent().build();
    }
}
