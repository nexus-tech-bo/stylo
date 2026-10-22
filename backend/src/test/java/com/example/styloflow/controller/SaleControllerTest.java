package com.example.styloflow.controller;

import com.example.styloflow.dto.sale.SaleItemResponse;
import com.example.styloflow.dto.sale.SaleResponse;
import com.example.styloflow.dto.sale.SaleSummaryResponse;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.Role;
import com.example.styloflow.enums.SaleStatus;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.service.SaleService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaleController.class)
@SecuredWebMvcTest
class SaleControllerTest {
    private static final SaleResponse SALE = new SaleResponse(101L, Instant.parse("2026-10-11T16:20:00Z"), 15L, "Admin",
            12L, "María López", "1234567", new BigDecimal("250.00"), new BigDecimal("5.00"), new BigDecimal("245.00"),
            new BigDecimal("28.19"), PaymentMethod.CASH, new BigDecimal("300.00"), new BigDecimal("55.00"),
            SaleStatus.COMPLETED, null, null, null, "", List.of(new SaleItemResponse(301L, ItemType.SERVICE, 7L, "Corte dama",
            1, new BigDecimal("90.00"), new BigDecimal("10.00"), new BigDecimal("80.00"), 3L, "Ana Pérez")));
    private static final String SALE_BODY = """
            {"customerId":12,
             "items":[{"type":"SERVICE","itemId":7,"quantity":1,"unitPrice":90.00,"discount":10.00,"stylistId":3},
                      {"type":"PRODUCT","itemId":4,"quantity":2}],
             "discount":5.00,"paymentMethod":"CASH","amountReceived":300.00,"notes":""}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SaleService saleService;

    @Test
    void create_should_return201_withCashierAndBusinessFromToken() throws Exception {
        when(saleService.createSale(any(), eq(2L), eq(TestAuth.BUSINESS_ID))).thenReturn(SALE);

        mvc.perform(post("/api/sales").with(TestAuth.as(2L, Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content(SALE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(245.00))
                .andExpect(jsonPath("$.tax").value(28.19))
                .andExpect(jsonPath("$.change").value(55.00))
                .andExpect(jsonPath("$.date").value("2026-10-11T16:20:00Z"))
                .andExpect(jsonPath("$.items[0].stylist").value("Ana Pérez"));
    }

    @Test
    void create_should_return400_when_noItemsAndNoPaymentMethod() throws Exception {
        mvc.perform(post("/api/sales").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.items").exists())
                .andExpect(jsonPath("$.errors.paymentMethod").exists());
        verifyNoInteractions(saleService);
    }

    @Test
    void create_should_return400_when_itemQuantityOutOfRange() throws Exception {
        mvc.perform(post("/api/sales").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"type\":\"PRODUCT\",\"itemId\":4,\"quantity\":0}],\"paymentMethod\":\"QR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    void create_should_return400_when_enumUnknown() throws Exception {
        mvc.perform(post("/api/sales").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"type\":\"PRODUCT\",\"itemId\":4,\"quantity\":1}],\"paymentMethod\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_should_return422_when_businessRuleBroken() throws Exception {
        when(saleService.createSale(any(), any(), any())).thenThrow(new BusinessRuleException("There is no open cash register"));

        mvc.perform(post("/api/sales").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(SALE_BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Business rule"))
                .andExpect(jsonPath("$.detail").value("There is no open cash register"));
    }

    @Test
    void list_should_return200_withFiltersAndNewestFirst() throws Exception {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var summary = new SaleSummaryResponse(101L, Instant.parse("2026-10-11T16:20:00Z"), "Admin", 12L, "María López",
                new BigDecimal("245.00"), PaymentMethod.CASH, SaleStatus.COMPLETED, true);
        when(saleService.findAllSales(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11), 12L, pageable))
                .thenReturn(new PageImpl<>(List.of(summary), pageable, 1));

        mvc.perform(get("/api/sales").param("from", "2026-10-01").param("to", "2026-10-11").param("customerId", "12")
                        .with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cashRegisterOpen").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_should_passNullDates_when_omitted() throws Exception {
        when(saleService.findAllSales(eq(null), eq(null), eq(null), any())).thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/sales").with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isOk());
        verify(saleService).findAllSales(eq(null), eq(null), eq(null), any());
    }

    @Test
    void list_should_return400_when_dateMalformed() throws Exception {
        mvc.perform(get("/api/sales").param("from", "11/10/2026").with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_should_return200() throws Exception {
        when(saleService.findSaleById(101L)).thenReturn(SALE);

        mvc.perform(get("/api/sales/101").with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerTaxId").value("1234567"))
                .andExpect(jsonPath("$.items[0].subtotal").value(80.00));
    }

    @Test
    void get_should_return404_when_missing() throws Exception {
        when(saleService.findSaleById(9L)).thenThrow(new NotFoundException("Sale not found with id: 9"));

        mvc.perform(get("/api/sales/9").with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void void_should_return200_when_admin() throws Exception {
        when(saleService.voidSaleById(eq(101L), any(), eq(1L))).thenReturn(SALE);

        mvc.perform(post("/api/sales/101/void").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Cobro duplicado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101));
    }

    @Test
    void void_should_return400_when_reasonBlank() throws Exception {
        mvc.perform(post("/api/sales/101/void").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
    }

    @Test
    void void_should_return422_when_registerClosed() throws Exception {
        when(saleService.voidSaleById(any(), any(), any())).thenThrow(new BusinessRuleException("The sale's cash register is already closed"));

        mvc.perform(post("/api/sales/101/void").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void void_should_return403_when_cashier() throws Exception {
        mvc.perform(post("/api/sales/101/void").with(TestAuth.as(2L, Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
        verifyNoInteractions(saleService);
    }

    @Test
    void create_should_return403_when_stylist() throws Exception {
        mvc.perform(post("/api/sales").with(TestAuth.as(3L, Role.STYLIST))
                        .contentType(MediaType.APPLICATION_JSON).content(SALE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/sales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}
