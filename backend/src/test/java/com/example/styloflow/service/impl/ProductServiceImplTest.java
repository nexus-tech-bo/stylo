package com.example.styloflow.service.impl;

import com.example.styloflow.dto.catalog.ProductRequest;
import com.example.styloflow.dto.catalog.StockAdjustmentRequest;
import com.example.styloflow.entity.Product;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.ProductMapper;
import com.example.styloflow.repository.ProductRepository;
import com.example.styloflow.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock
    ProductRepository productRepository;
    @Mock
    ImageStorageService imageStorageService;
    @Spy
    ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
    @InjectMocks
    ProductServiceImpl service;

    private static ProductRequest request(String sku) {
        return new ProductRequest("Shampoo", sku, new BigDecimal("85.00"), 2, null, null);
    }

    private static Product product(int stock, int minStock) {
        return Product.builder().id(4L).name("Shampoo").price(BigDecimal.TEN).stock(stock).minStock(minStock).build();
    }

    @Test
    void productRequest_should_normalizeBlankSkuAndDefaults() {
        var req = request("  ");

        assertThat(req.sku()).isNull();
        assertThat(req.minStock()).isZero();
        assertThat(req.active()).isTrue();
    }

    @Test
    void createProduct_should_throwConflict_and_notUpload_when_skuTaken() {
        when(productRepository.existsBySku("SH-500")).thenReturn(true);

        assertThatThrownBy(() -> service.createProduct(request("SH-500"), null)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(imageStorageService);
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_should_skipSkuCheck_and_computeLowStock_when_skuBlank() {
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createProduct(new ProductRequest("Gel", "", BigDecimal.ONE, 0, 0, true), null);

        verify(productRepository, never()).existsBySku(anyString());
        assertThat(response.sku()).isNull();
        assertThat(response.lowStock()).isTrue();
    }

    @Test
    void updateProductById_should_throwConflict_when_skuTakenByAnother() {
        when(productRepository.findById(4L)).thenReturn(Optional.of(product(5, 1)));
        when(productRepository.existsBySkuAndIdNot("SH-500", 4L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateProductById(4L, request("SH-500"))).isInstanceOf(ConflictException.class);
    }

    @Test
    void updateProductById_should_keepStock_when_requestHasStock() {
        when(productRepository.findById(4L)).thenReturn(Optional.of(product(5, 1)));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateProductById(4L, new ProductRequest("Renamed", null, BigDecimal.ONE, 99, 1, true));

        assertThat(response.name()).isEqualTo("Renamed");
        assertThat(response.stock()).isEqualTo(5);
    }

    @Test
    void updateProductById_should_throwNotFound_when_missing() {
        when(productRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProductById(4L, request(null))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void adjustStock_should_throwBusinessRule_when_quantityZero() {
        assertThatThrownBy(() -> service.adjustStock(4L, new StockAdjustmentRequest(0)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void adjustStock_should_throwBusinessRule_when_resultNegative() {
        when(productRepository.findById(4L)).thenReturn(Optional.of(product(2, 0)));

        assertThatThrownBy(() -> service.adjustStock(4L, new StockAdjustmentRequest(-3)))
                .isInstanceOf(BusinessRuleException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void adjustStock_should_applyDelta_when_valid() {
        when(productRepository.findById(4L)).thenReturn(Optional.of(product(2, 1)));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.adjustStock(4L, new StockAdjustmentRequest(-2));

        assertThat(response.stock()).isZero();
        assertThat(response.lowStock()).isTrue();
    }
}
