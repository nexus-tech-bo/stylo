package com.example.styloflow.controller;

import com.example.styloflow.dto.PageResponse;
import com.example.styloflow.dto.cashregister.CashRegisterResponse;
import com.example.styloflow.dto.cashregister.CloseCashRegisterRequest;
import com.example.styloflow.dto.cashregister.OpenCashRegisterRequest;
import com.example.styloflow.security.AuthenticatedUser;
import com.example.styloflow.service.CashRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cash-register")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
@Tag(name = "Cash register", description = "Open, close and review cash-register sessions")
public class CashRegisterController {
    private final CashRegisterService cashRegisterService;

    @GetMapping("/current")
    @Operation(summary = "GET /api/cash-register/current — the open register with live totals (204 if none)")
    public ResponseEntity<CashRegisterResponse> findCurrentCashRegister() {
        return cashRegisterService.findCurrentCashRegister()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/open")
    @Operation(summary = "POST /api/cash-register/open — open the cash register")
    public ResponseEntity<CashRegisterResponse> openCashRegister(@Valid @RequestBody OpenCashRegisterRequest request,
                                                                 @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(cashRegisterService.openCashRegister(request, user.id()));
    }

    @PostMapping("/close")
    @Operation(summary = "POST /api/cash-register/close — close the open cash register")
    public ResponseEntity<CashRegisterResponse> closeCashRegister(@Valid @RequestBody CloseCashRegisterRequest request,
                                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(cashRegisterService.closeCashRegister(request, user.id()));
    }

    @GetMapping("/history")
    @Operation(summary = "GET /api/cash-register/history — past and current sessions, most recent first")
    public ResponseEntity<PageResponse<CashRegisterResponse>> findCashRegisterHistory(
            @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(cashRegisterService.findCashRegisterHistory(pageable)));
    }

    @GetMapping("/{cashRegisterId}")
    @Operation(summary = "GET /api/cash-register/{cashRegisterId} — get a cash-register session")
    public ResponseEntity<CashRegisterResponse> findCashRegisterById(@PathVariable Long cashRegisterId) {
        return ResponseEntity.ok(cashRegisterService.findCashRegisterById(cashRegisterId));
    }
}
