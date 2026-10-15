package com.example.styloflow.service.impl;

import com.example.styloflow.dto.catalog.CategoryRequest;
import com.example.styloflow.entity.Category;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.CategoryMapper;
import com.example.styloflow.repository.CategoryRepository;
import com.example.styloflow.repository.SalonServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    SalonServiceRepository salonServiceRepository;
    @Spy
    CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
    @InjectMocks
    CategoryServiceImpl service;

    @Test
    void createCategory_should_defaultActiveToTrue_when_omitted() {
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createCategory(new CategoryRequest("Cortes", null));

        assertThat(response.active()).isTrue();
        assertThat(response.name()).isEqualTo("Cortes");
    }

    @Test
    void createCategory_should_throwConflict_when_nameTaken() {
        when(categoryRepository.existsByName("Cortes")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CategoryRequest("Cortes", true)))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategoryById_should_throwConflict_when_nameTakenByAnother() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).name("A").build()));
        when(categoryRepository.existsByNameAndIdNot("Cortes", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateCategoryById(1L, new CategoryRequest("Cortes", true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCategoryById_should_throwNotFound_when_missing() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategoryById(9L, new CategoryRequest("X", true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteCategoryById_should_throwConflict_when_categoryHasServices() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).name("A").build()));
        when(salonServiceRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCategoryById(1L)).isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategoryById_should_delete_when_noServices() {
        var category = Category.builder().id(1L).name("A").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        service.deleteCategoryById(1L);

        verify(categoryRepository).delete(category);
    }
}
