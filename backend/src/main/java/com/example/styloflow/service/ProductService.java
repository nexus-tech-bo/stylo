package com.example.styloflow.service;

import com.example.styloflow.dto.catalog.ProductRequest;
import com.example.styloflow.dto.catalog.ProductResponse;
import com.example.styloflow.dto.catalog.StockAdjustmentRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    /** Products sorted by name; {@code activeOnly} keeps only active ones. */
    List<ProductResponse> findAllProducts(boolean activeOnly);

    /** Active products with {@code stock <= minStock}, sorted by name. */
    List<ProductResponse> findLowStockProducts();

    /**
     * @param image optional image; {@code null} or empty means no image
     * @throws com.example.styloflow.exception.ConflictException if the SKU is taken
     */
    ProductResponse createProduct(ProductRequest request, MultipartFile image);

    /** Replaces every field except the image. */
    ProductResponse updateProductById(Long productId, ProductRequest request);

    /**
     * Adds {@code quantity} (negative removes units) to the stock.
     *
     * @throws com.example.styloflow.exception.BusinessRuleException if quantity is 0 or the stock would go below 0
     */
    ProductResponse adjustStock(Long productId, StockAdjustmentRequest request);
}
