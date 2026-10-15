package com.example.styloflow.repository;

import com.example.styloflow.entity.SalonService;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonServiceRepository extends JpaRepository<SalonService, Long> {
    @EntityGraph(attributePaths = "category")
    List<SalonService> findAllByOrderByNameAsc();

    /** Active services whose category is active too. */
    @EntityGraph(attributePaths = "category")
    List<SalonService> findByActiveTrueAndCategoryActiveTrueOrderByNameAsc();

    boolean existsByCategoryId(Long categoryId);
}
