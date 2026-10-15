package com.example.styloflow.dto.report;

import java.math.BigDecimal;

public record TopItemResponse(Long id, String name, long quantity, BigDecimal total) {
}
