package com.example.styloflow.controller;

import com.example.styloflow.entity.Customer;
import com.example.styloflow.enums.Role;
import com.example.styloflow.service.CustomerService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug found in the live smoke: {@code ?sort=<unknown property>} on any paginated list
 * ({@code /api/customers}, {@code /api/sales}, {@code /api/cash-register/history}) answers 500 "Internal error".
 * It is a client mistake and must be a 400 "Invalid request". The two exceptions below are the ones the real
 * repositories throw (derived/findAll queries vs {@code @Query} JPQL).
 */
@WebMvcTest(CustomerController.class)
@SecuredWebMvcTest
@DisplayName("Invalid ?sort on paginated lists")
class InvalidSortParameterTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService customerService;

    @Test
    @DisplayName("PropertyReferenceException (findAll with unknown sort) -> 400 Invalid request")
    void list_should_return400_when_sortPropertyUnknown_onDerivedQuery() throws Exception {
        when(customerService.findAllCustomers(isNull(), any(Pageable.class)))
                .thenAnswer(_ -> PropertyPath.from("foo", Customer.class));

        mvc.perform(get("/api/customers").param("sort", "foo").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("InvalidDataAccessApiUsageException (@Query with unknown sort) -> 400 Invalid request")
    void list_should_return400_when_sortPropertyUnknown_onJpqlQuery() throws Exception {
        when(customerService.findAllCustomers(isNull(), any(Pageable.class))).thenThrow(new InvalidDataAccessApiUsageException(
                "org.hibernate.query.sqm.UnknownPathException: Could not resolve attribute 'foo' of 'com.example.styloflow.entity.Customer'"));

        mvc.perform(get("/api/customers").param("sort", "foo").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
