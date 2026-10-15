package com.example.styloflow.controller;

import com.example.styloflow.dto.report.DailySalesResponse;
import com.example.styloflow.dto.report.SalesSummaryResponse;
import com.example.styloflow.dto.report.StylistTotalResponse;
import com.example.styloflow.dto.report.TopItemResponse;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.security.AuthenticatedUser;
import com.example.styloflow.service.ReportService;
import com.example.styloflow.util.DateRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Every endpoint takes optional {@code from}/{@code to} days (default today, max one year, 422 otherwise). */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Sales, stylist and product reports")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "GET /api/reports/summary — sales totals, filter by ?from=&to=")
    public ResponseEntity<SalesSummaryResponse> findSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.findSummary(DateRange.resolve(from, to)));
    }

    @GetMapping("/sales-by-day")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "GET /api/reports/sales-by-day — completed sales per day, filter by ?from=&to=")
    public ResponseEntity<List<DailySalesResponse>> findSalesByDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.findSalesByDay(DateRange.resolve(from, to)));
    }

    @GetMapping("/stylists")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "GET /api/reports/stylists — services and commissions per stylist, filter by ?from=&to=")
    public ResponseEntity<List<StylistTotalResponse>> findStylistTotals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.findStylistTotals(DateRange.resolve(from, to)));
    }

    @GetMapping("/my-commissions")
    @Operation(summary = "GET /api/reports/my-commissions — the authenticated user's services and commission, filter by ?from=&to=")
    public ResponseEntity<List<StylistTotalResponse>> findMyCommissions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(reportService.findStylistTotalsByUserId(DateRange.resolve(from, to), user.id()));
    }

    @GetMapping("/top")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "GET /api/reports/top — best-selling services or products, filter by ?type=SERVICE|PRODUCT&limit=10&from=&to=")
    public ResponseEntity<List<TopItemResponse>> findTopItems(
            @RequestParam(defaultValue = "SERVICE") ItemType type,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.findTopItems(DateRange.resolve(from, to), type, limit));
    }

    /** Streams the rows straight from the database to the response (same thread, so the tenant context holds). */
    @GetMapping("/sales.csv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "GET /api/reports/sales.csv — download completed and voided sales as CSV, filter by ?from=&to=")
    public void exportSalesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {
        var range = DateRange.resolve(from, to);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=sales_%s_%s.csv".formatted(range.from(), range.to()));
        reportService.writeSalesCsv(range, response.getWriter());
    }
}
