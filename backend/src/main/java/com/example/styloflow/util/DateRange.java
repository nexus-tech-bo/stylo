package com.example.styloflow.util;

import com.example.styloflow.exception.BusinessRuleException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Inclusive day range in the business zone, plus its instant bounds for queries:
 * {@code createdAt >= start AND createdAt < end}.
 *
 * @param from  first day (inclusive)
 * @param to    last day (inclusive)
 * @param start start of {@code from} in {@link #ZONE}
 * @param end   start of the day after {@code to} in {@link #ZONE} (exclusive)
 */
public record DateRange(LocalDate from, LocalDate to, Instant start, Instant end) {
    /** Time zone of every business. */
    public static final ZoneId ZONE = ZoneId.of("America/La_Paz");

    /**
     * Applies the API defaults: no {@code from} = today, no {@code to} = {@code from}.
     *
     * @param from optional first day
     * @param to   optional last day
     * @return the resolved range
     * @throws BusinessRuleException if {@code to} is before {@code from} or the range exceeds one year
     */
    public static DateRange resolve(LocalDate from, LocalDate to) {
        var start = from != null ? from : LocalDate.now(ZONE);
        var end = to != null ? to : start;
        if (end.isBefore(start))
            throw new BusinessRuleException("'to' must not be before 'from'");
        if (end.isAfter(start.plusYears(1)))
            throw new BusinessRuleException("The date range cannot exceed one year");
        return new DateRange(start, end,
                start.atStartOfDay(ZONE).toInstant(),
                end.plusDays(1).atStartOfDay(ZONE).toInstant());
    }
}
