package com.example.styloflow.controller;

import com.example.styloflow.dto.auth.LoginResponse;
import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.BusinessSuspendedException;
import com.example.styloflow.service.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@SecuredWebMvcTest
class AuthControllerTest {
    private static final UserResponse ADMIN =
            new UserResponse(1L, "Admin", "admin", Role.ADMIN, null, new BigDecimal("0.00"), true);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;

    @Test
    void login_should_return200_when_credentialsValid() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("tok", Instant.parse("2026-10-12T02:30:00Z"), ADMIN,
                new LoginResponse.BusinessInfo("demo", "Peluquería Demo")));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessCode":"demo","username":"admin","password":"admin123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-12T02:30:00Z"))
                .andExpect(jsonPath("$.business.code").value("demo"));
    }

    @Test
    void login_should_return400InvalidData_when_fieldsBlank() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"businessCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.businessCode").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_should_return400InvalidRequest_when_jsonMalformed() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void login_should_return403_when_businessSuspended() throws Exception {
        when(authService.login(any())).thenThrow(new BusinessSuspendedException("This business is suspended"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessCode\":\"demo\",\"username\":\"a\",\"password\":\"b\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Business suspended"));
    }

    @Test
    void me_should_return200_when_businessUser() throws Exception {
        when(authService.findCurrentUser(1L)).thenReturn(ADMIN);

        mvc.perform(get("/api/auth/me").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.commissionRate").value(0.00));
    }

    @Test
    void me_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void me_should_return403_when_platformToken() throws Exception {
        mvc.perform(get("/api/auth/me").with(TestAuth.as(1L, Role.SUPERADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }
}
