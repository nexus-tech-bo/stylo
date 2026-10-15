package com.example.styloflow.repository;

import com.example.styloflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByNameAsc();

    List<Product> findByActiveTrueOrderByNameAsc();

    /** Active products with {@code stock <= minStock}. */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock <= p.minStock ORDER BY p.name")
    List<Product> findLowStock();

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);
}
