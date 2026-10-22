package com.example.styloflow.service.impl;

import com.example.styloflow.dto.report.DailySalesResponse;
import com.example.styloflow.dto.report.SalesSummaryResponse;
import com.example.styloflow.dto.report.StylistTotalResponse;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.SaleStatus;
import com.example.styloflow.repository.ReportRepository;
import com.example.styloflow.util.DateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    private static final SpelAwareProxyProjectionFactory PROJECTIONS = new SpelAwareProxyProjectionFactory();
    private static final DateRange RANGE = DateRange.resolve(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

    @Mock
    ReportRepository reportRepository;

    @InjectMocks
    ReportServiceImpl reportService;

    @Test
    void findSummary_should_computeAverageAndAddLineDiscounts() {
        when(reportRepository.findSalesTotals(RANGE.start(), RANGE.end())).thenReturn(projection(ReportRepository.SalesTotals.class,
                Map.of("salesCount", 3L, "salesTotal", new BigDecimal("100"), "discounts", new BigDecimal("5"),
                        "tax", new BigDecimal("11.5"), "voidedSales", 2L)));
        when(reportRepository.sumLineDiscounts(RANGE.start(), RANGE.end())).thenReturn(new BigDecimal("2.50"));
        when(reportRepository.findTotalsByPaymentMethod(RANGE.start(), RANGE.end())).thenReturn(List.of(
                projection(ReportRepository.PaymentMethodTotals.class,
                        Map.of("method", PaymentMethod.CASH, "count", 3L, "total", new BigDecimal("100")))));

        var summary = reportService.findSummary(RANGE);

        assertThat(summary.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(summary.to()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(summary.salesTotal()).isEqualTo("100.00");
        assertThat(summary.averageTicket()).isEqualTo("33.33");
        assertThat(summary.totalDiscounts()).isEqualTo("7.50");
        assertThat(summary.totalTax()).isEqualTo("11.50");
        assertThat(summary.voidedSales()).isEqualTo(2);
        assertThat(summary.byPaymentMethod()).containsExactly(
                new SalesSummaryResponse.PaymentMethodTotal(PaymentMethod.CASH, 3, new BigDecimal("100.00")));
    }

    @Test
    void findSummary_should_returnZeros_when_noSales() {
        var nulls = new HashMap<String, Object>();
        nulls.put("salesCount", 0L);
        nulls.put("voidedSales", 0L);
        when(reportRepository.findSalesTotals(RANGE.start(), RANGE.end()))
                .thenReturn(projection(ReportRepository.SalesTotals.class, nulls));

        var summary = reportService.findSummary(RANGE);

        assertThat(summary.salesCount()).isZero();
        assertThat(summary.salesTotal()).isEqualTo("0.00");
        assertThat(summary.averageTicket()).isEqualTo("0.00");
        assertThat(summary.totalDiscounts()).isEqualTo("0.00");
        assertThat(summary.totalTax()).isEqualTo("0.00");
        assertThat(summary.byPaymentMethod()).isEmpty();
    }

    @Test
    void findSalesByDay_should_fillDaysWithoutSales() {
        when(reportRepository.findDailyTotals("America/La_Paz", RANGE.start(), RANGE.end())).thenReturn(List.of(
                projection(ReportRepository.DailyTotals.class,
                        Map.of("day", "2026-10-02", "salesCount", 4L, "total", new BigDecimal("80.5")))));

        assertThat(reportService.findSalesByDay(RANGE)).containsExactly(
                new DailySalesResponse(LocalDate.of(2026, 10, 1), 0, new BigDecimal("0.00")),
                new DailySalesResponse(LocalDate.of(2026, 10, 2), 4, new BigDecimal("80.50")),
                new DailySalesResponse(LocalDate.of(2026, 10, 3), 0, new BigDecimal("0.00")));
    }

    @Test
    void findStylistTotals_should_roundMoney_and_queryAllStylists() {
        when(reportRepository.findStylistTotals(any(), any(), isNull())).thenReturn(List.of(
                projection(ReportRepository.StylistTotals.class, Map.of("stylistId", 3L, "stylist", "Ana",
                        "services", 2L, "total", new BigDecimal("100"), "commissionRate", new BigDecimal("40"),
                        "commission", new BigDecimal("33.3333333")))));

        assertThat(reportService.findStylistTotals(RANGE)).containsExactly(new StylistTotalResponse(3L, "Ana", 2,
                new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("33.33")));
    }

    @Test
    void findStylistTotalsByUserId_should_filterByUser() {
        when(reportRepository.findStylistTotals(RANGE.start(), RANGE.end(), 7L)).thenReturn(List.of());

        assertThat(reportService.findStylistTotalsByUserId(RANGE, 7L)).isEmpty();
        verify(reportRepository).findStylistTotals(RANGE.start(), RANGE.end(), 7L);
    }

    @Test
    void findTopItems_should_passTypeAndLimit() {
        when(reportRepository.findTopItems(ItemType.PRODUCT, RANGE.start(), RANGE.end(), Limit.of(5))).thenReturn(List.of(
                projection(ReportRepository.TopItem.class,
                        Map.of("id", 7L, "name", "Shampoo", "quantity", 9L, "total", new BigDecimal("90")))));

        var top = reportService.findTopItems(RANGE, ItemType.PRODUCT, 5);

        assertThat(top).singleElement().satisfies(t -> {
            assertThat(t.name()).isEqualTo("Shampoo");
            assertThat(t.quantity()).isEqualTo(9);
            assertThat(t.total()).isEqualTo("90.00");
        });
    }

    @Test
    void writeSalesCsv_should_writeHeaderRowsInLocalTime_and_closeStream() {
        var closed = new AtomicBoolean();
        var row = csvRow(15L, "Caja \"1\"", "Pérez; Juan\nhijo", SaleStatus.VOIDED);
        when(reportRepository.streamSalesForCsv(RANGE.start(), RANGE.end()))
                .thenReturn(Stream.of(row).onClose(() -> closed.set(true)));
        var out = new StringWriter();

        reportService.writeSalesCsv(RANGE, new PrintWriter(out));

        assertThat(out.toString()).isEqualTo(
                "id;date;cashier;customer;subtotal;discount;total;tax;payment_method;status\r\n" +
                "15;2026-10-01 10:30:00;\"Caja \"\"1\"\"\";\"Pérez; Juan\nhijo\";110.00;10.00;100.00;11.50;QR;VOIDED\r\n");
        assertThat(closed).isTrue();
    }

    @Test
    void writeSalesCsv_should_leaveCustomerEmpty_when_none() {
        when(reportRepository.streamSalesForCsv(RANGE.start(), RANGE.end()))
                .thenReturn(Stream.of(csvRow(1L, "Ana", null, SaleStatus.COMPLETED)));
        var out = new StringWriter();

        reportService.writeSalesCsv(RANGE, new PrintWriter(out));

        assertThat(out.toString()).endsWith("\r\n1;2026-10-01 10:30:00;Ana;;110.00;10.00;100.00;11.50;QR;COMPLETED\r\n");
    }

    @Test
    void csvText_should_escapeSeparatorsQuotesAndFormulas() {
        assertThat(ReportServiceImpl.csvText("Ana")).isEqualTo("Ana");
        assertThat(ReportServiceImpl.csvText("a;b")).isEqualTo("\"a;b\"");
        assertThat(ReportServiceImpl.csvText("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(ReportServiceImpl.csvText("a\r\nb")).isEqualTo("\"a\r\nb\"");
        assertThat(ReportServiceImpl.csvText("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(ReportServiceImpl.csvText(null)).isEmpty();
    }

    private static ReportRepository.SaleCsvRow csvRow(Long id, String cashier, String customer, SaleStatus status) {
        var values = new HashMap<String, Object>();
        values.put("id", id);
        values.put("createdAt", Instant.parse("2026-10-01T14:30:00Z")); // 10:30 in La Paz
        values.put("cashier", cashier);
        values.put("customer", customer);
        values.put("subtotal", new BigDecimal("110"));
        values.put("discount", new BigDecimal("10"));
        values.put("total", new BigDecimal("100"));
        values.put("tax", new BigDecimal("11.5"));
        values.put("paymentMethod", PaymentMethod.QR);
        values.put("status", status);
        return projection(ReportRepository.SaleCsvRow.class, values);
    }

    private static <T> T projection(Class<T> type, Map<String, Object> values) {
        return PROJECTIONS.createProjection(type, values);
    }
}
