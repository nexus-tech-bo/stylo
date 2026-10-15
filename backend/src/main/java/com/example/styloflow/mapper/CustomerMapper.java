package com.example.styloflow.mapper;

import com.example.styloflow.dto.customer.CustomerRequest;
import com.example.styloflow.dto.customer.CustomerResponse;
import com.example.styloflow.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CustomerMapper {
    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void toEntityUpdated(CustomerRequest request, @MappingTarget Customer customer);
}
