package com.example.styloflow.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated response of the API contract: {@code {content, page, size, totalElements, totalPages}}.
 *
 * @param <T> item type
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
