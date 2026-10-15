package com.example.styloflow.controller;

import com.example.styloflow.dto.platform.BusinessSummaryResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.service.PlatformBusinessService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlatformBusinessController.class)
@SecuredWebMvcTest
class PlatformBusinessControllerTest {
    private static final BusinessSummaryResponse BELLA = new BusinessSummaryResponse(2L, "salon-bella", "Salón Bella", true,
            Instant.parse("2026-10-11T15:00:00Z"), 1, 0, new BigDecimal("0.00"));
    private static final String VALID = """
            {"name":"Salón Bella","code":"salon-bella","adminName":"Laura Ruiz","adminUsername":"laura","adminPassword":"clave123","baseCatalog":true}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlatformBusinessService platformBusinessService;

    @Test
    void findAllBusinesses_should_return200_when_superadmin() throws Exception {
        when(platformBusinessService.findAllBusinesses()).thenReturn(List.of(BELLA));

        mvc.perform(get("/api/platform/businesses").with(TestAuth.as(1L, Role.SUPERADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("salon-bella"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-11T15:00:00Z"))
                .andExpect(jsonPath("$[0].users").value(1))
                .andExpect(jsonPath("$[0].sales30d").value(0))
                .andExpect(jsonPath("$[0].total30d").value(0.00));
    }

    @Test
    void findAllBusinesses_should_return403_when_businessAdmin() throws Exception {
        mvc.perform(get("/api/platform/businesses").with(TestAuth.as(1L, Role.ADMIN))).andExpect(status().isForbidden());
        verifyNoInteractions(platformBusinessService);
    }

    @Test
    void findAllBusinesses_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/platform/businesses")).andExpect(status().isUnauthorized());
    }

    @Test
    void createBusiness_should_return201_when_valid() throws Exception {
        when(platformBusinessService.createBusiness(any())).thenReturn(BELLA);

        mvc.perform(post("/api/platform/businesses").with(TestAuth.as(1L, Role.SUPERADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void createBusiness_should_return400_when_fieldsInvalid() throws Exception {
        mvc.perform(post("/api/platform/businesses").with(TestAuth.as(1L, Role.SUPERADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","code":"Salon Bella","adminName":"","adminUsername":"l","adminPassword":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.adminName").exists())
                .andExpect(jsonPath("$.errors.adminUsername").exists())
                .andExpect(jsonPath("$.errors.adminPassword").exists());
        verifyNoInteractions(platformBusinessService);
    }

    @Test
    void createBusiness_should_return409_when_codeTaken() throws Exception {
        when(platformBusinessService.createBusiness(any())).thenThrow(new ConflictException("Business code already exists: salon-bella"));

        mvc.perform(post("/api/platform/businesses").with(TestAuth.as(1L, Role.SUPERADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict());
    }

    @Test
    void updateBusinessStatus_should_return204_when_valid() throws Exception {
        mvc.perform(put("/api/platform/businesses/2/status").with(TestAuth.as(1L, Role.SUPERADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNoContent());
        verify(platformBusinessService).updateBusinessStatus(eq(2L), any());
    }

    @Test
    void updateBusinessStatus_should_return400_when_activeMissing() throws Exception {
        mvc.perform(put("/api/platform/businesses/2/status").with(TestAuth.as(1L, Role.SUPERADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.active").exists());
    }

    @Test
    void updateBusinessStatus_should_return404_when_missing() throws Exception {
        doThrow(new NotFoundException("Business not found with id: 9")).when(platformBusinessService).updateBusinessStatus(eq(9L), any());

        mvc.perform(put("/api/platform/businesses/9/status").with(TestAuth.as(1L, Role.SUPERADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBusinessStatus_should_return403_when_businessAdmin() throws Exception {
        mvc.perform(put("/api/platform/businesses/2/status").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }
}
