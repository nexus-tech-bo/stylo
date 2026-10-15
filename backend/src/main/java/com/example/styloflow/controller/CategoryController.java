package com.example.styloflow.controller;

import com.example.styloflow.dto.catalog.CategoryRequest;
import com.example.styloflow.dto.catalog.CategoryResponse;
import com.example.styloflow.service.CategoryService;
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
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
@Tag(name = "Catalog - Categories", description = "Service categories")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "GET /api/catalog/categories — list categories sorted by name")
    public ResponseEntity<List<CategoryResponse>> findAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategories());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "POST /api/catalog/categories — create a category (admin)")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "PUT /api/catalog/categories/{categoryId} — update a category (admin)")
    public ResponseEntity<CategoryResponse> updateCategoryById(@PathVariable Long categoryId,
                                                               @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategoryById(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "DELETE /api/catalog/categories/{categoryId} — delete a category without services (admin)")
    public ResponseEntity<Void> deleteCategoryById(@PathVariable Long categoryId) {
        categoryService.deleteCategoryById(categoryId);
        return ResponseEntity.noContent().build();
    }
}
