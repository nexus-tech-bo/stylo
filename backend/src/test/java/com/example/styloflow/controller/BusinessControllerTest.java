package com.example.styloflow.controller;

import com.example.styloflow.dto.business.BusinessRequest;
import com.example.styloflow.dto.business.BusinessResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.service.BusinessService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessController.class)
@SecuredWebMvcTest
class BusinessControllerTest {
    private static final BusinessResponse DEMO = new BusinessResponse("demo", "Peluquería Demo", null, null, null,
            "BOB", "Bs", new BigDecimal("13.00"), "¡Gracias!");
    private static final String VALID = """
            {"name":"Peluquería Demo","currency":"bob","currencySymbol":"Bs","taxRate":13.00,"code":"hacked"}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BusinessService businessService;

    @Test
    void findCurrentBusiness_should_return200_forTokenBusiness() throws Exception {
        when(businessService.findBusinessById(TestAuth.BUSINESS_ID)).thenReturn(DEMO);

        mvc.perform(get("/api/business").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo"))
                .andExpect(jsonPath("$.taxRate").value(13.00));
    }

    @Test
    void findCurrentBusiness_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/business")).andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentBusiness_should_return200AndUppercaseCurrency_when_admin() throws Exception {
        when(businessService.updateBusinessById(eq(TestAuth.BUSINESS_ID), any())).thenReturn(DEMO);

        mvc.perform(put("/api/business").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(BusinessRequest.class);
        verify(businessService).updateBusinessById(eq(TestAuth.BUSINESS_ID), captor.capture());
        assertThat(captor.getValue().currency()).isEqualTo("BOB");
    }

    @Test
    void updateCurrentBusiness_should_return400_when_fieldsInvalid() throws Exception {
        mvc.perform(put("/api/business").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","currency":"BO1","currencySymbol":"","taxRate":101}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.currency").value("must be exactly 3 letters"))
                .andExpect(jsonPath("$.errors.currencySymbol").exists())
                .andExpect(jsonPath("$.errors.taxRate").exists());
    }

    @Test
    void updateCurrentBusiness_should_return403_when_cashier() throws Exception {
        mvc.perform(put("/api/business").with(TestAuth.as(2L, Role.CASHIER)).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
        verifyNoInteractions(businessService);
    }
}
