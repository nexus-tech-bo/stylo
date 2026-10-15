package com.example.styloflow.util;

import com.example.styloflow.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void resolve_should_defaultToToday_when_noDates() {
        var range = DateRange.resolve(null, null);
        var today = LocalDate.now(DateRange.ZONE);
        assertThat(range.from()).isEqualTo(today);
        assertThat(range.to()).isEqualTo(today);
    }

    @Test
    void resolve_should_useLaPazBoundsWithExclusiveEnd() {
        var range = DateRange.resolve(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11));
        // La Paz is UTC-4 all year
        assertThat(range.start()).isEqualTo(Instant.parse("2026-10-01T04:00:00Z"));
        assertThat(range.end()).isEqualTo(Instant.parse("2026-10-12T04:00:00Z"));
    }

    @Test
    void resolve_should_default_to_equalFrom() {
        var range = DateRange.resolve(LocalDate.of(2026, 1, 5), null);
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 1, 5));
    }

    @Test
    void resolve_should_throw_when_toBeforeFrom() {
        assertThatThrownBy(() -> DateRange.resolve(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolve_should_allowExactlyOneYear_and_rejectMore() {
        var from = LocalDate.of(2025, 10, 11);
        assertThat(DateRange.resolve(from, from.plusYears(1)).to()).isEqualTo(from.plusYears(1));
        assertThatThrownBy(() -> DateRange.resolve(from, from.plusYears(1).plusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
