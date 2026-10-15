package com.example.styloflow.controller;

import com.example.styloflow.dto.catalog.CategoryResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.service.CategoryService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@SecuredWebMvcTest
class CategoryControllerTest {
    private static final CategoryResponse CORTES = new CategoryResponse(1L, "Cortes", true);
    private static final String BODY = "{\"name\":\"Cortes\",\"active\":true}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CategoryService categoryService;

    @Test
    void findAll_should_return200_when_anyBusinessRole() throws Exception {
        when(categoryService.findAllCategories()).thenReturn(List.of(CORTES));

        mvc.perform(get("/api/catalog/categories").with(TestAuth.as(2L, Role.STYLIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cortes"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void findAll_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/catalog/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_should_return201_when_admin() throws Exception {
        when(categoryService.createCategory(any())).thenReturn(CORTES);

        mvc.perform(post("/api/catalog/categories").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_should_return403_when_cashier() throws Exception {
        mvc.perform(post("/api/catalog/categories").with(TestAuth.as(2L, Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void create_should_return400_when_nameBlankOrTooLong() throws Exception {
        mvc.perform(post("/api/catalog/categories").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "x".repeat(81) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void create_should_return409_when_duplicate() throws Exception {
        when(categoryService.createCategory(any())).thenThrow(new ConflictException("dup"));

        mvc.perform(post("/api/catalog/categories").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void update_should_return200_when_admin() throws Exception {
        when(categoryService.updateCategoryById(eq(1L), any())).thenReturn(CORTES);

        mvc.perform(put("/api/catalog/categories/1").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cortes"));
    }

    @Test
    void delete_should_return204_when_admin() throws Exception {
        mvc.perform(delete("/api/catalog/categories/1").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_should_return409_when_categoryHasServices() throws Exception {
        doThrow(new ConflictException("in use")).when(categoryService).deleteCategoryById(1L);

        mvc.perform(delete("/api/catalog/categories/1").with(TestAuth.as(1L, Role.ADMIN)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_should_return403_when_stylist() throws Exception {
        mvc.perform(delete("/api/catalog/categories/1").with(TestAuth.as(2L, Role.STYLIST)))
                .andExpect(status().isForbidden());
    }
}
