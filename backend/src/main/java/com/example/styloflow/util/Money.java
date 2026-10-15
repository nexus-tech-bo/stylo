package com.example.styloflow.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money and percentages in the API always have 2 decimals (API.md). */
public final class Money {
    private Money() {}

    /**
     * Scales {@code value} to 2 decimals, HALF_UP. MapStruct applies it to every {@code BigDecimal -> BigDecimal}
     * property of the mappers using {@code MapStructConfig}.
     *
     * @param value amount or percentage, may be {@code null}
     * @return the scaled value, or {@code null}
     */
    public static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
