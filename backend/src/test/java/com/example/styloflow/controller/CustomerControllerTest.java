package com.example.styloflow.controller;

import com.example.styloflow.dto.customer.CustomerResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.service.CustomerService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@SecuredWebMvcTest
class CustomerControllerTest {
    private static final CustomerResponse MARIA = new CustomerResponse(12L, "María López", "71234567", "maria@mail.com",
            "1234567", null, Instant.parse("2026-09-01T15:00:00Z"));
    private static final String BODY = """
            {"name":"María López","phone":"71234567","email":"maria@mail.com","taxId":"1234567"}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService customerService;

    @Test
    void list_should_return200PageWithDefaults() throws Exception {
        var pageable = PageRequest.of(0, 20, Sort.by("name"));
        when(customerService.findAllCustomers(eq("mar"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(MARIA), pageable, 1));

        mvc.perform(get("/api/customers").param("q", "mar").with(TestAuth.as(1L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("María López"))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-09-01T15:00:00Z"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(customerService).findAllCustomers("mar", pageable);
    }

    @Test
    void get_should_return200() throws Exception {
        when(customerService.findCustomerById(12L)).thenReturn(MARIA);

        mvc.perform(get("/api/customers/12").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxId").value("1234567"));
    }

    @Test
    void get_should_return404_when_missing() throws Exception {
        when(customerService.findCustomerById(99L)).thenThrow(new NotFoundException("Customer not found with id: 99"));

        mvc.perform(get("/api/customers/99").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    void create_should_return201() throws Exception {
        when(customerService.createCustomer(any())).thenReturn(MARIA);

        mvc.perform(post("/api/customers").with(TestAuth.as(1L, Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void create_should_return400_when_invalid() throws Exception {
        mvc.perform(post("/api/customers").with(TestAuth.as(1L, Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void update_should_return200() throws Exception {
        when(customerService.updateCustomerById(eq(12L), any())).thenReturn(MARIA);

        mvc.perform(put("/api/customers/12").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("María López"));
    }

    @Test
    void list_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_should_return403_when_stylist() throws Exception {
        mvc.perform(get("/api/customers").with(TestAuth.as(3L, Role.STYLIST)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }
}
