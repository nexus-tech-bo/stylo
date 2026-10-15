package com.example.styloflow.service.impl;

import com.example.styloflow.config.tenant.TenantContext;
import com.example.styloflow.dto.catalog.ProductRequest;
import com.example.styloflow.dto.catalog.ProductResponse;
import com.example.styloflow.dto.catalog.StockAdjustmentRequest;
import com.example.styloflow.entity.Product;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.ProductMapper;
import com.example.styloflow.repository.ProductRepository;
import com.example.styloflow.service.ImageStorageService;
import com.example.styloflow.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private static final String SKU_TAKEN = "A product with this SKU already exists";

    private final ProductRepository productRepository;
    private final ImageStorageService imageStorageService;
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponse> findAllProducts(boolean activeOnly) {
        var products = activeOnly
                ? productRepository.findByActiveTrueOrderByNameAsc()
                : productRepository.findAllByOrderByNameAsc();
        return productMapper.toResponseList(products);
    }

    @Override
    public List<ProductResponse> findLowStockProducts() {
        return productMapper.toResponseList(productRepository.findLowStock());
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, MultipartFile image) {
        if (request.sku() != null && productRepository.existsBySku(request.sku()))
            throw new ConflictException(SKU_TAKEN);
        var product = productMapper.toEntity(request);
        // Upload after the checks so a rejected request leaves no orphan image.
        product.setImageUrl(imageStorageService.upload(image, "styloflow/" + TenantContext.getCurrentTenant() + "/products"));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProductById(Long productId, ProductRequest request) {
        var product = getProductOrThrowById(productId);
        if (request.sku() != null && productRepository.existsBySkuAndIdNot(request.sku(), productId))
            throw new ConflictException(SKU_TAKEN);
        productMapper.toEntityUpdated(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse adjustStock(Long productId, StockAdjustmentRequest request) {
        if (request.quantity() == 0)
            throw new BusinessRuleException("The quantity must not be zero");
        var product = getProductOrThrowById(productId);
        var newStock = product.getStock() + request.quantity();
        if (newStock < 0)
            throw new BusinessRuleException("Insufficient stock, only " + product.getStock() + " units available");
        product.setStock(newStock);
        return productMapper.toResponse(productRepository.save(product));
    }

    private Product getProductOrThrowById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
    }
}
