package com.example.styloflow.service.impl;

import com.example.styloflow.dto.catalog.CategoryRequest;
import com.example.styloflow.dto.catalog.CategoryResponse;
import com.example.styloflow.entity.Category;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.CategoryMapper;
import com.example.styloflow.repository.CategoryRepository;
import com.example.styloflow.repository.SalonServiceRepository;
import com.example.styloflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private static final String NAME_TAKEN = "A category with this name already exists";

    private final CategoryRepository categoryRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> findAllCategories() {
        return categoryMapper.toResponseList(categoryRepository.findAllByOrderByNameAsc());
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name()))
            throw new ConflictException(NAME_TAKEN);
        return categoryMapper.toResponse(categoryRepository.save(categoryMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategoryById(Long categoryId, CategoryRequest request) {
        var category = getCategoryOrThrowById(categoryId);
        if (categoryRepository.existsByNameAndIdNot(request.name(), categoryId))
            throw new ConflictException(NAME_TAKEN);
        categoryMapper.toEntityUpdated(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategoryById(Long categoryId) {
        var category = getCategoryOrThrowById(categoryId);
        if (salonServiceRepository.existsByCategoryId(categoryId))
            throw new ConflictException("The category has services, deactivate it instead");
        categoryRepository.delete(category);
    }

    private Category getCategoryOrThrowById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));
    }
}
