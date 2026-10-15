package com.example.styloflow.controller;

import com.example.styloflow.dto.PageResponse;
import com.example.styloflow.dto.sale.SaleRequest;
import com.example.styloflow.dto.sale.SaleResponse;
import com.example.styloflow.dto.sale.SaleSummaryResponse;
import com.example.styloflow.dto.sale.VoidSaleRequest;
import com.example.styloflow.security.AuthenticatedUser;
import com.example.styloflow.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
@Tag(name = "Sales", description = "Point of sale")
public class SaleController {
    private final SaleService saleService;

    @PostMapping
    @Operation(summary = "POST /api/sales — register a sale in the open cash register")
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody SaleRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createSale(request, user.id(), user.businessId()));
    }

    @GetMapping
    @Operation(summary = "GET /api/sales — list sales, filter by ?from={yyyy-MM-dd}&to={yyyy-MM-dd}&customerId={id} (default today)")
    public ResponseEntity<PageResponse<SaleSummaryResponse>> findAllSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(saleService.findAllSales(from, to, customerId, pageable)));
    }

    @GetMapping("/{saleId}")
    @Operation(summary = "GET /api/sales/{saleId} — get a sale with its items")
    public ResponseEntity<SaleResponse> findSaleById(@PathVariable Long saleId) {
        return ResponseEntity.ok(saleService.findSaleById(saleId));
    }

    @PostMapping("/{saleId}/void")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "POST /api/sales/{saleId}/void — void a sale and restore stock (admin only)")
    public ResponseEntity<SaleResponse> voidSaleById(@PathVariable Long saleId, @Valid @RequestBody VoidSaleRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(saleService.voidSaleById(saleId, request, user.id()));
    }
}
