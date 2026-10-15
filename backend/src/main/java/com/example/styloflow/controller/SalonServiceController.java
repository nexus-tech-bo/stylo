package com.example.styloflow.controller;

import com.example.styloflow.dto.catalog.SalonServiceRequest;
import com.example.styloflow.dto.catalog.SalonServiceResponse;
import com.example.styloflow.service.SalonServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/services")
@RequiredArgsConstructor
@Tag(name = "Catalog - Services", description = "Salon services")
public class SalonServiceController {
    private final SalonServiceService salonServiceService;

    @GetMapping
    @Operation(summary = "GET /api/catalog/services — list services sorted by name, filter by ?activeOnly={bool}")
    public ResponseEntity<List<SalonServiceResponse>> findAllServices(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(salonServiceService.findAllServices(activeOnly));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "POST /api/catalog/services — create a service with an optional image (admin, multipart)")
    public ResponseEntity<SalonServiceResponse> createService(@Valid @ModelAttribute SalonServiceRequest request,
                                                              @RequestPart(required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salonServiceService.createService(request, image));
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "PUT /api/catalog/services/{serviceId} — update a service, image unchanged (admin)")
    public ResponseEntity<SalonServiceResponse> updateServiceById(@PathVariable Long serviceId,
                                                                  @Valid @RequestBody SalonServiceRequest request) {
        return ResponseEntity.ok(salonServiceService.updateServiceById(serviceId, request));
    }
}
