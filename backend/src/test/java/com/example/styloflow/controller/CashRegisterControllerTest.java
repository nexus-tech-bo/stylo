package com.example.styloflow.controller;

import com.example.styloflow.dto.cashregister.CashRegisterResponse;
import com.example.styloflow.dto.cashregister.PaymentMethodTotalResponse;
import com.example.styloflow.enums.CashRegisterStatus;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.service.CashRegisterService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashRegisterController.class)
@SecuredWebMvcTest
class CashRegisterControllerTest {
    private static final CashRegisterResponse CLOSED = new CashRegisterResponse(15L, CashRegisterStatus.CLOSED, "Admin",
            Instant.parse("2026-10-11T12:00:00Z"), new BigDecimal("200.00"), "Admin", Instant.parse("2026-10-11T23:00:00Z"),
            18, new BigDecimal("1830.00"), new BigDecimal("1450.00"), new BigDecimal("1450.50"), new BigDecimal("0.50"),
            List.of(new PaymentMethodTotalResponse(PaymentMethod.CASH, 12, new BigDecimal("1250.00"))), "Sin novedades");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CashRegisterService cashRegisterService;

    @Test
    void current_should_return200_when_open() throws Exception {
        when(cashRegisterService.findCurrentCashRegister()).thenReturn(Optional.of(CLOSED));

        mvc.perform(get("/api/cash-register/current").with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesCount").value(18))
                .andExpect(jsonPath("$.byPaymentMethod[0].method").value("CASH"));
    }

    @Test
    void current_should_return204_when_noneOpen() throws Exception {
        when(cashRegisterService.findCurrentCashRegister()).thenReturn(Optional.empty());

        mvc.perform(get("/api/cash-register/current").with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void open_should_return200_withPrincipalId() throws Exception {
        when(cashRegisterService.openCashRegister(any(), eq(7L))).thenReturn(CLOSED);

        mvc.perform(post("/api/cash-register/open").with(TestAuth.as(7L, Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"openingAmount\":200.00,\"notes\":\"Fondo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15));
    }

    @Test
    void open_should_return422_when_alreadyOpen() throws Exception {
        when(cashRegisterService.openCashRegister(any(), any())).thenThrow(new BusinessRuleException("A cash register is already open"));

        mvc.perform(post("/api/cash-register/open").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"openingAmount\":0}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Business rule"));
    }

    @Test
    void open_should_return400_when_amountMissingOrNegative() throws Exception {
        mvc.perform(post("/api/cash-register/open").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"openingAmount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.openingAmount").exists());
    }

    @Test
    void close_should_return200() throws Exception {
        when(cashRegisterService.closeCashRegister(any(), eq(1L))).thenReturn(CLOSED);

        mvc.perform(post("/api/cash-register/close").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"countedCash\":1450.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difference").value(0.50))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void close_should_return400_when_countedCashMissing() throws Exception {
        mvc.perform(post("/api/cash-register/close").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.countedCash").exists());
    }

    @Test
    void close_should_return422_when_noneOpen() throws Exception {
        when(cashRegisterService.closeCashRegister(any(), any())).thenThrow(new BusinessRuleException("There is no open cash register"));

        mvc.perform(post("/api/cash-register/close").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"countedCash\":0}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void history_should_return200_mostRecentFirst() throws Exception {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "openedAt"));
        when(cashRegisterService.findCashRegisterHistory(pageable)).thenReturn(new PageImpl<>(List.of(CLOSED), pageable, 1));

        mvc.perform(get("/api/cash-register/history").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(15))
                .andExpect(jsonPath("$.totalPages").value(1));
        verify(cashRegisterService).findCashRegisterHistory(pageable);
    }

    @Test
    void getById_should_return200() throws Exception {
        when(cashRegisterService.findCashRegisterById(15L)).thenReturn(CLOSED);

        mvc.perform(get("/api/cash-register/15").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openedAt").value("2026-10-11T12:00:00Z"));
    }

    @Test
    void getById_should_return404_when_missing() throws Exception {
        when(cashRegisterService.findCashRegisterById(9L)).thenThrow(new NotFoundException("Cash register not found with id: 9"));

        mvc.perform(get("/api/cash-register/9").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void current_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/cash-register/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void open_should_return403_when_stylist() throws Exception {
        mvc.perform(post("/api/cash-register/open").with(TestAuth.as(3L, Role.STYLIST))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"openingAmount\":0}"))
                .andExpect(status().isForbidden());
    }
}
