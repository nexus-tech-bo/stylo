package com.example.styloflow.controller;

import com.example.styloflow.dto.catalog.SalonServiceRequest;
import com.example.styloflow.dto.catalog.SalonServiceResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.service.SalonServiceService;
import com.example.styloflow.support.SecuredWebMvcTest;
import com.example.styloflow.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalonServiceController.class)
@SecuredWebMvcTest
class SalonServiceControllerTest {
    private static final SalonServiceResponse CORTE = new SalonServiceResponse(7L, 1L, "Cortes", "Corte dama",
            "Lavado incluido", 45, new BigDecimal("80.00"), true, "https://img/x.jpg");
    private static final String BODY = """
            {"categoryId":1,"name":"Corte dama","durationMinutes":45,"price":80.00}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SalonServiceService salonServiceService;

    @Test
    void findAll_should_passActiveOnly_and_return200() throws Exception {
        when(salonServiceService.findAllServices(true)).thenReturn(List.of(CORTE));

        mvc.perform(get("/api/catalog/services").param("activeOnly", "true").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Cortes"))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].price").value(80.00))
                .andExpect(jsonPath("$[0].imageUrl").value("https://img/x.jpg"));
    }

    @Test
    void findAll_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/catalog/services")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_should_return201_and_bindFormFieldsAndImage_when_multipart() throws Exception {
        when(salonServiceService.createService(any(), any())).thenReturn(CORTE);
        var image = new MockMultipartFile("image", "corte.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/catalog/services").file(image).with(TestAuth.as(1L, Role.ADMIN))
                        .param("categoryId", "1").param("name", "Corte dama").param("description", "Lavado incluido")
                        .param("durationMinutes", "45").param("price", "80.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));

        var request = forClass(SalonServiceRequest.class);
        var file = forClass(MultipartFile.class);
        verify(salonServiceService).createService(request.capture(), file.capture());
        assertThat(request.getValue().name()).isEqualTo("Corte dama");
        assertThat(request.getValue().price()).isEqualByComparingTo("80.00");
        assertThat(request.getValue().active()).isTrue();
        assertThat(file.getValue().getOriginalFilename()).isEqualTo("corte.jpg");
    }

    @Test
    void create_should_return400_when_multipartFieldsInvalid() throws Exception {
        mvc.perform(multipart("/api/catalog/services").with(TestAuth.as(1L, Role.ADMIN))
                        .param("name", "").param("durationMinutes", "0").param("price", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.categoryId").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.durationMinutes").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void create_should_return400_when_multipartValueNotNumeric() throws Exception {
        mvc.perform(multipart("/api/catalog/services").with(TestAuth.as(1L, Role.ADMIN))
                        .param("categoryId", "1").param("name", "X").param("durationMinutes", "45").param("price", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("invalid value"));
    }

    @Test
    void create_should_return403_when_stylist() throws Exception {
        mvc.perform(multipart("/api/catalog/services").with(TestAuth.as(2L, Role.STYLIST))
                        .param("categoryId", "1").param("name", "X").param("durationMinutes", "45").param("price", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_should_return404_when_categoryMissing() throws Exception {
        when(salonServiceService.createService(any(), any())).thenThrow(new NotFoundException("Category not found"));

        mvc.perform(multipart("/api/catalog/services").with(TestAuth.as(1L, Role.ADMIN))
                        .param("categoryId", "99").param("name", "X").param("durationMinutes", "45").param("price", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    void update_should_return200_when_adminJson() throws Exception {
        when(salonServiceService.updateServiceById(eq(7L), any())).thenReturn(CORTE);

        mvc.perform(put("/api/catalog/services/7").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Corte dama"));
    }

    @Test
    void update_should_return403_when_cashier() throws Exception {
        mvc.perform(put("/api/catalog/services/7").with(TestAuth.as(2L, Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }
}
