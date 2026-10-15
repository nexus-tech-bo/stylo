package com.example.styloflow.controller;

import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.dto.user.UserSummaryResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.service.UserService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@SecuredWebMvcTest
class UserControllerTest {
    private static final UserResponse ANA =
            new UserResponse(3L, "Ana Pérez", "ana.perez", Role.STYLIST, "70000000", new BigDecimal("40.00"), true);
    private static final String VALID = """
            {"name":"Ana Pérez","username":"ana.perez","password":"secreto1","role":"STYLIST","phone":"70000000","commissionRate":40.00,"active":true}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @Test
    void findAllUsers_should_return200_when_admin() throws Exception {
        when(userService.findAllUsers()).thenReturn(List.of(ANA));

        mvc.perform(get("/api/users").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("ana.perez"))
                .andExpect(jsonPath("$[0].commissionRate").value(40.00))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void findAllUsers_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void findAllUsers_should_return403_when_cashier() throws Exception {
        mvc.perform(get("/api/users").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void findAllUsers_should_return403_when_platformToken() throws Exception {
        mvc.perform(get("/api/users").with(TestAuth.as(1L, Role.SUPERADMIN))).andExpect(status().isForbidden());
    }

    @Test
    void findActiveStylists_should_return200_when_stylist() throws Exception {
        when(userService.findActiveStylists()).thenReturn(List.of(new UserSummaryResponse(3L, "Ana Pérez", Role.STYLIST)));

        mvc.perform(get("/api/users/stylists").with(TestAuth.as(3L, Role.STYLIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana Pérez"))
                .andExpect(jsonPath("$[0].role").value("STYLIST"));
    }

    @Test
    void findUserById_should_return200_when_admin() throws Exception {
        when(userService.findUserById(3L)).thenReturn(ANA);

        mvc.perform(get("/api/users/3").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void findUserById_should_return404_when_missing() throws Exception {
        when(userService.findUserById(9L)).thenThrow(new NotFoundException("User not found with id: 9"));

        mvc.perform(get("/api/users/9").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    void findUserById_should_return403_when_stylist() throws Exception {
        mvc.perform(get("/api/users/3").with(TestAuth.as(3L, Role.STYLIST))).andExpect(status().isForbidden());
    }

    @Test
    void createUser_should_return201_when_valid() throws Exception {
        when(userService.createUser(any())).thenReturn(ANA);

        mvc.perform(post("/api/users").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void createUser_should_return400_when_fieldsInvalid() throws Exception {
        mvc.perform(post("/api/users").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","username":"a b","password":"123","role":null,"commissionRate":150}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.role").exists())
                .andExpect(jsonPath("$.errors.commissionRate").exists());
        verifyNoInteractions(userService);
    }

    @Test
    void createUser_should_return400_when_roleSuperadmin() throws Exception {
        mvc.perform(post("/api/users").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("STYLIST", "SUPERADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").value("must be ADMIN, CASHIER or STYLIST"));
    }

    @Test
    void createUser_should_return422_when_passwordMissing() throws Exception {
        when(userService.createUser(any())).thenThrow(new BusinessRuleException("Password is required for a new user"));

        mvc.perform(post("/api/users").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("\"password\":\"secreto1\",", "")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Business rule"));
    }

    @Test
    void createUser_should_return409_when_usernameTaken() throws Exception {
        when(userService.createUser(any())).thenThrow(new ConflictException("Username already exists: ana.perez"));

        mvc.perform(post("/api/users").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void createUser_should_return403_when_cashier() throws Exception {
        mvc.perform(post("/api/users").with(TestAuth.as(2L, Role.CASHIER)).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    void updateUserById_should_return200_when_valid() throws Exception {
        when(userService.updateUserById(eq(3L), any())).thenReturn(ANA);

        mvc.perform(put("/api/users/3").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Pérez"));
    }

    @Test
    void updateUserById_should_return422_when_lastAdmin() throws Exception {
        when(userService.updateUserById(eq(1L), any()))
                .thenThrow(new BusinessRuleException("The business must keep at least one active ADMIN"));

        mvc.perform(put("/api/users/1").with(TestAuth.as(1L, Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void changePassword_should_return204_forCallerId() throws Exception {
        mvc.perform(put("/api/users/me/password").with(TestAuth.as(5L, Role.STYLIST)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"nueva123\"}"))
                .andExpect(status().isNoContent());
        verify(userService).changePassword(eq(5L), any());
    }

    @Test
    void changePassword_should_return400_when_tooShort() throws Exception {
        mvc.perform(put("/api/users/me/password").with(TestAuth.as(5L, Role.STYLIST)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void changePassword_should_return401_when_noToken() throws Exception {
        mvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"nueva123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
