package com.example.styloflow.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesResponse(LocalDate date, long count, BigDecimal total) {
}
