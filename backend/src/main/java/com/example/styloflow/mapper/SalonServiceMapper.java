package com.example.styloflow.mapper;

import com.example.styloflow.dto.catalog.SalonServiceRequest;
import com.example.styloflow.dto.catalog.SalonServiceResponse;
import com.example.styloflow.entity.SalonService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface SalonServiceMapper {
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "category")
    SalonServiceResponse toResponse(SalonService service);

    List<SalonServiceResponse> toResponseList(List<SalonService> services);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SalonService toEntity(SalonServiceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void toEntityUpdated(SalonServiceRequest request, @MappingTarget SalonService service);
}
