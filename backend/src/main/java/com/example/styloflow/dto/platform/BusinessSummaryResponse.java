package com.example.styloflow.dto.platform;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A business with its activity of the last 30 days.
 *
 * @param users    number of users of the business
 * @param sales30d completed sales in the last 30 days
 * @param total30d sum of the totals of those sales
 */
public record BusinessSummaryResponse(
        Long id,
        String code,
        String name,
        boolean active,
        Instant createdAt,
        long users,
        long sales30d,
        BigDecimal total30d
) {
}
