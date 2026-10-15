package com.example.styloflow.controller;

import com.example.styloflow.dto.catalog.ProductRequest;
import com.example.styloflow.dto.catalog.ProductResponse;
import com.example.styloflow.dto.catalog.StockAdjustmentRequest;
import com.example.styloflow.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
@Tag(name = "Catalog - Products", description = "Products and stock")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    @Operation(summary = "GET /api/catalog/products — list products sorted by name, filter by ?activeOnly={bool}")
    public ResponseEntity<List<ProductResponse>> findAllProducts(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(productService.findAllProducts(activeOnly));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "GET /api/catalog/products/low-stock — active products with stock at or below the minimum")
    public ResponseEntity<List<ProductResponse>> findLowStockProducts() {
        return ResponseEntity.ok(productService.findLowStockProducts());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "POST /api/catalog/products — create a product with an optional image (admin, multipart)")
    public ResponseEntity<ProductResponse> createProduct(@Validated({Default.class, ProductRequest.OnCreate.class})
                                                         @ModelAttribute ProductRequest request,
                                                         @RequestPart(required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request, image));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "PUT /api/catalog/products/{productId} — update a product, image and stock unchanged (admin)")
    public ResponseEntity<ProductResponse> updateProductById(@PathVariable Long productId,
                                                             @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProductById(productId, request));
    }

    @PostMapping("/{productId}/stock-adjustment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "POST /api/catalog/products/{productId}/stock-adjustment — add or remove stock (admin)")
    public ResponseEntity<ProductResponse> adjustStock(@PathVariable Long productId,
                                                       @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(productService.adjustStock(productId, request));
    }
}
