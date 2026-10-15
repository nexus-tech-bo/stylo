package com.example.styloflow.mapper;

import com.example.styloflow.dto.business.BusinessRequest;
import com.example.styloflow.dto.business.BusinessResponse;
import com.example.styloflow.entity.Business;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface BusinessMapper {
    BusinessResponse toResponse(Business business);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    void toEntityUpdated(BusinessRequest request, @MappingTarget Business business);
}
