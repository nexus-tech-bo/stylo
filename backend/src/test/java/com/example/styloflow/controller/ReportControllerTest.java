package com.example.styloflow.controller;

import com.example.styloflow.dto.report.DailySalesResponse;
import com.example.styloflow.dto.report.SalesSummaryResponse;
import com.example.styloflow.dto.report.StylistTotalResponse;
import com.example.styloflow.dto.report.TopItemResponse;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.Role;
import com.example.styloflow.service.ReportService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import com.example.styloflow.util.DateRange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@SecuredWebMvcTest
class ReportControllerTest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 11);
    private static final DateRange RANGE = DateRange.resolve(FROM, TO);
    private static final String TOO_LONG = "?from=2025-01-01&to=2026-06-01";
    private static final StylistTotalResponse ANA = new StylistTotalResponse(3L, "Ana Pérez", 25,
            new BigDecimal("2100.00"), new BigDecimal("40.00"), new BigDecimal("840.00"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReportService reportService;

    // --- summary --------------------------------------------------------------

    @Test
    void summary_should_return200_when_cashier() throws Exception {
        when(reportService.findSummary(RANGE)).thenReturn(new SalesSummaryResponse(FROM, TO, 210,
                new BigDecimal("21850.00"), new BigDecimal("104.05"), new BigDecimal("430.00"), new BigDecimal("2513.72"), 3,
                List.of(new SalesSummaryResponse.PaymentMethodTotal(PaymentMethod.CASH, 140, new BigDecimal("14200.00")))));

        mvc.perform(get("/api/reports/summary?from=2026-10-01&to=2026-10-11").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-01"))
                .andExpect(jsonPath("$.to").value("2026-10-11"))
                .andExpect(jsonPath("$.salesCount").value(210))
                .andExpect(jsonPath("$.averageTicket").value(104.05))
                .andExpect(jsonPath("$.voidedSales").value(3))
                .andExpect(jsonPath("$.byPaymentMethod[0].method").value("CASH"))
                .andExpect(jsonPath("$.byPaymentMethod[0].total").value(14200.00));
    }

    @Test
    void summary_should_defaultToToday_when_noDates() throws Exception {
        mvc.perform(get("/api/reports/summary").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk());

        verify(reportService).findSummary(DateRange.resolve(null, null));
    }

    @Test
    void summary_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/reports/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void summary_should_return403_when_stylist() throws Exception {
        mvc.perform(get("/api/reports/summary").with(TestAuth.as(3L, Role.STYLIST)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void summary_should_return422_when_rangeOverOneYear() throws Exception {
        mvc.perform(get("/api/reports/summary" + TOO_LONG).with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Business rule"));
        verifyNoInteractions(reportService);
    }

    @Test
    void summary_should_return400_when_dateMalformed() throws Exception {
        mvc.perform(get("/api/reports/summary?from=11-10-2026").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    // --- sales-by-day ---------------------------------------------------------

    @Test
    void salesByDay_should_return200_when_admin() throws Exception {
        when(reportService.findSalesByDay(RANGE)).thenReturn(List.of(new DailySalesResponse(TO, 18, new BigDecimal("1830.00"))));

        mvc.perform(get("/api/reports/sales-by-day?from=2026-10-01&to=2026-10-11").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-10-11"))
                .andExpect(jsonPath("$[0].count").value(18))
                .andExpect(jsonPath("$[0].total").value(1830.00));
    }

    @Test
    void salesByDay_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/reports/sales-by-day")).andExpect(status().isUnauthorized());
    }

    @Test
    void salesByDay_should_return403_when_cashier() throws Exception {
        mvc.perform(get("/api/reports/sales-by-day").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesByDay_should_return422_when_rangeOverOneYear() throws Exception {
        mvc.perform(get("/api/reports/sales-by-day" + TOO_LONG).with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent());
    }

    // --- stylists -------------------------------------------------------------

    @Test
    void stylists_should_return200_when_admin() throws Exception {
        when(reportService.findStylistTotals(RANGE)).thenReturn(List.of(ANA));

        mvc.perform(get("/api/reports/stylists?from=2026-10-01&to=2026-10-11").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stylistId").value(3))
                .andExpect(jsonPath("$[0].stylist").value("Ana Pérez"))
                .andExpect(jsonPath("$[0].services").value(25))
                .andExpect(jsonPath("$[0].commissionRate").value(40.00))
                .andExpect(jsonPath("$[0].commission").value(840.00));
    }

    @Test
    void stylists_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/reports/stylists")).andExpect(status().isUnauthorized());
    }

    @Test
    void stylists_should_return403_when_cashier() throws Exception {
        mvc.perform(get("/api/reports/stylists").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void stylists_should_return422_when_rangeOverOneYear() throws Exception {
        mvc.perform(get("/api/reports/stylists" + TOO_LONG).with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent());
    }

    // --- my-commissions -------------------------------------------------------

    @Test
    void myCommissions_should_useAuthenticatedUserId() throws Exception {
        when(reportService.findStylistTotalsByUserId(RANGE, 3L)).thenReturn(List.of(ANA));

        mvc.perform(get("/api/reports/my-commissions?from=2026-10-01&to=2026-10-11").with(TestAuth.as(3L, Role.STYLIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stylistId").value(3))
                .andExpect(jsonPath("$[0].commission").value(840.00));
    }

    @Test
    void myCommissions_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/reports/my-commissions")).andExpect(status().isUnauthorized());
    }

    @Test
    void myCommissions_should_return403_when_platformToken() throws Exception {
        mvc.perform(get("/api/reports/my-commissions").with(TestAuth.as(1L, Role.SUPERADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void myCommissions_should_return422_when_rangeOverOneYear() throws Exception {
        mvc.perform(get("/api/reports/my-commissions" + TOO_LONG).with(TestAuth.as(3L, Role.STYLIST)))
                .andExpect(status().isUnprocessableContent());
    }

    // --- top ------------------------------------------------------------------

    @Test
    void top_should_return200_with_typeAndLimit() throws Exception {
        when(reportService.findTopItems(RANGE, ItemType.PRODUCT, 5))
                .thenReturn(List.of(new TopItemResponse(7L, "Shampoo", 54, new BigDecimal("4320.00"))));

        mvc.perform(get("/api/reports/top?type=PRODUCT&limit=5&from=2026-10-01&to=2026-10-11").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Shampoo"))
                .andExpect(jsonPath("$[0].quantity").value(54))
                .andExpect(jsonPath("$[0].total").value(4320.00));
    }

    @Test
    void top_should_defaultToServiceAndTen() throws Exception {
        mvc.perform(get("/api/reports/top").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk());

        verify(reportService).findTopItems(DateRange.resolve(null, null), ItemType.SERVICE, 10);
    }

    @Test
    void top_should_return400_when_limitOutOfRange() throws Exception {
        mvc.perform(get("/api/reports/top?limit=0").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.limit").exists());
        mvc.perform(get("/api/reports/top?limit=101").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reportService);
    }

    @Test
    void top_should_return400_when_typeUnknown() throws Exception {
        mvc.perform(get("/api/reports/top?type=FOO").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void top_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/reports/top")).andExpect(status().isUnauthorized());
    }

    @Test
    void top_should_return403_when_cashier() throws Exception {
        mvc.perform(get("/api/reports/top").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void top_should_return422_when_rangeOverOneYear() throws Exception {
        mvc.perform(get("/api/reports/top" + TOO_LONG).with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent());
    }

    // --- sales.csv ------------------------------------------------------------

    @Test
    void salesCsv_should_downloadUtf8Attachment() throws Exception {
        doAnswer(inv -> {
            inv.getArgument(1, PrintWriter.class).print("id;date\r\n1;Pérez\r\n");
            return null;
        }).when(reportService).writeSalesCsv(eq(RANGE), any());

        mvc.perform(get("/api/reports/sales.csv?from=2026-10-01&to=2026-10-11").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=sales_2026-10-01_2026-10-11.csv"))
                .andExpect(content().string("id;date\r\n1;Pérez\r\n"));
    }

    @Test
    void salesCsv_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/reports/sales.csv")).andExpect(status().isUnauthorized());
    }

    @Test
    void salesCsv_should_return403_when_cashier() throws Exception {
        mvc.perform(get("/api/reports/sales.csv").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reportService);
    }

    @Test
    void salesCsv_should_return422_when_rangeOverOneYear() throws Exception {
        mvc.perform(get("/api/reports/sales.csv" + TOO_LONG).with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Business rule"));
    }
}
