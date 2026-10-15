package com.example.styloflow.service;

import com.example.styloflow.dto.catalog.CategoryRequest;
import com.example.styloflow.dto.catalog.CategoryResponse;

import java.util.List;

public interface CategoryService {
    /** All categories sorted by name. */
    List<CategoryResponse> findAllCategories();

    /** @throws com.example.styloflow.exception.ConflictException if the name is taken */
    CategoryResponse createCategory(CategoryRequest request);

    /** @throws com.example.styloflow.exception.ConflictException if another category has the name */
    CategoryResponse updateCategoryById(Long categoryId, CategoryRequest request);

    /** @throws com.example.styloflow.exception.ConflictException if the category still has services */
    void deleteCategoryById(Long categoryId);
}
