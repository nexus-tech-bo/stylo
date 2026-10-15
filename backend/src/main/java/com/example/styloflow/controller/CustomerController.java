package com.example.styloflow.controller;

import com.example.styloflow.dto.PageResponse;
import com.example.styloflow.dto.customer.CustomerRequest;
import com.example.styloflow.dto.customer.CustomerResponse;
import com.example.styloflow.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
@Tag(name = "Customers", description = "Salon customers")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "GET /api/customers — list customers, filter by ?q={name, phone or tax id}")
    public ResponseEntity<PageResponse<CustomerResponse>> findAllCustomers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(customerService.findAllCustomers(q, pageable)));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "GET /api/customers/{customerId} — get a customer")
    public ResponseEntity<CustomerResponse> findCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.findCustomerById(customerId));
    }

    @PostMapping
    @Operation(summary = "POST /api/customers — create a customer")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "PUT /api/customers/{customerId} — update a customer")
    public ResponseEntity<CustomerResponse> updateCustomerById(@PathVariable Long customerId,
                                                               @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomerById(customerId, request));
    }
}
