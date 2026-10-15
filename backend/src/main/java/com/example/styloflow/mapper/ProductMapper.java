package com.example.styloflow.mapper;

import com.example.styloflow.dto.catalog.ProductRequest;
import com.example.styloflow.dto.catalog.ProductResponse;
import com.example.styloflow.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ProductMapper {
    @Mapping(target = "lowStock", expression = "java(product.getStock() <= product.getMinStock())")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "stock", ignore = true) // only sales and stock-adjustment change it (no lost updates)
    void toEntityUpdated(ProductRequest request, @MappingTarget Product product);
}
