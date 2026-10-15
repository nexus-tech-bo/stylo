package com.example.styloflow.mapper;

import com.example.styloflow.dto.catalog.CategoryRequest;
import com.example.styloflow.dto.catalog.CategoryResponse;
import com.example.styloflow.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequest request);

    @Mapping(target = "id", ignore = true)
    void toEntityUpdated(CategoryRequest request, @MappingTarget Category category);
}
