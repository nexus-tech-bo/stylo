package com.example.styloflow.controller;

import com.example.styloflow.dto.catalog.ProductRequest;
import com.example.styloflow.dto.catalog.ProductResponse;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.service.ProductService;
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

@WebMvcTest(ProductController.class)
@SecuredWebMvcTest
class ProductControllerTest {
    private static final ProductResponse SHAMPOO = new ProductResponse(4L, "Shampoo 500ml", "SH-500",
            new BigDecimal("85.00"), 2, 3, true, true, "https://img/s.jpg");
    private static final String BODY = """
            {"name":"Shampoo 500ml","sku":"SH-500","price":85.00,"stock":12,"minStock":3,"active":true}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService productService;

    @Test
    void findAll_should_return200_with_defaultActiveOnlyFalse() throws Exception {
        when(productService.findAllProducts(false)).thenReturn(List.of(SHAMPOO));

        mvc.perform(get("/api/catalog/products").with(TestAuth.as(2L, Role.STYLIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SH-500"))
                .andExpect(jsonPath("$[0].lowStock").value(true))
                .andExpect(jsonPath("$[0].imageUrl").value("https://img/s.jpg"));
    }

    @Test
    void lowStock_should_return200() throws Exception {
        when(productService.findLowStockProducts()).thenReturn(List.of(SHAMPOO));

        mvc.perform(get("/api/catalog/products/low-stock").with(TestAuth.as(2L, Role.CASHIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    void lowStock_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/catalog/products/low-stock")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_should_return201_and_bindFormFieldsAndImage_when_multipart() throws Exception {
        when(productService.createProduct(any(), any())).thenReturn(SHAMPOO);
        var image = new MockMultipartFile("image", "shampoo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/catalog/products").file(image).with(TestAuth.as(1L, Role.ADMIN))
                        .param("name", "Shampoo 500ml").param("sku", " ").param("price", "85.00").param("stock", "12"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));

        var request = forClass(ProductRequest.class);
        var file = forClass(MultipartFile.class);
        verify(productService).createProduct(request.capture(), file.capture());
        assertThat(request.getValue().sku()).isNull();
        assertThat(request.getValue().minStock()).isZero();
        assertThat(request.getValue().active()).isTrue();
        assertThat(file.getValue().getOriginalFilename()).isEqualTo("shampoo.jpg");
    }

    @Test
    void create_should_return201_when_multipartWithoutImage() throws Exception {
        when(productService.createProduct(any(), any())).thenReturn(SHAMPOO);

        mvc.perform(multipart("/api/catalog/products").with(TestAuth.as(1L, Role.ADMIN))
                        .param("name", "Shampoo 500ml").param("price", "85.00").param("stock", "12"))
                .andExpect(status().isCreated());
    }

    @Test
    void create_should_return400_when_multipartFieldsInvalid() throws Exception {
        mvc.perform(multipart("/api/catalog/products").with(TestAuth.as(1L, Role.ADMIN))
                        .param("name", "").param("price", "-1").param("stock", "-2").param("minStock", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists())
                .andExpect(jsonPath("$.errors.minStock").exists());
    }

    @Test
    void create_should_return400_when_stockMissing() throws Exception {
        mvc.perform(multipart("/api/catalog/products").with(TestAuth.as(1L, Role.ADMIN))
                        .param("name", "Shampoo").param("price", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stock").exists());
    }

    /** A non-convertible value prevents building the record, so only the conversion errors are reported. */
    @Test
    void create_should_return400_when_multipartValueNotNumeric() throws Exception {
        mvc.perform(multipart("/api/catalog/products").with(TestAuth.as(1L, Role.ADMIN))
                        .param("name", "Shampoo").param("price", "1").param("stock", "1").param("minStock", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.minStock").value("invalid value"));
    }

    @Test
    void create_should_return409_when_skuDuplicate() throws Exception {
        when(productService.createProduct(any(), any())).thenThrow(new ConflictException("dup"));

        mvc.perform(multipart("/api/catalog/products").with(TestAuth.as(1L, Role.ADMIN))
                        .param("name", "Shampoo").param("sku", "SH-500").param("price", "1").param("stock", "1"))
                .andExpect(status().isConflict());
    }

    @Test
    void create_should_return403_when_cashier() throws Exception {
        mvc.perform(multipart("/api/catalog/products").with(TestAuth.as(2L, Role.CASHIER))
                        .param("name", "Shampoo").param("price", "1").param("stock", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_should_return200_when_adminJson() throws Exception {
        when(productService.updateProductById(eq(4L), any())).thenReturn(SHAMPOO);

        mvc.perform(put("/api/catalog/products/4").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shampoo 500ml"));
    }

    @Test
    void update_should_return400_when_requiredFieldsMissing() throws Exception {
        mvc.perform(put("/api/catalog/products/4").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").doesNotExist());
    }

    @Test
    void update_should_return200_when_stockOmitted() throws Exception {
        when(productService.updateProductById(eq(4L), any())).thenReturn(SHAMPOO);

        mvc.perform(put("/api/catalog/products/4").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Shampoo 500ml","price":85.00}"""))
                .andExpect(status().isOk());
    }

    @Test
    void stockAdjustment_should_return200_when_admin() throws Exception {
        when(productService.adjustStock(eq(4L), any())).thenReturn(SHAMPOO);

        mvc.perform(post("/api/catalog/products/4/stock-adjustment").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(2));
    }

    @Test
    void stockAdjustment_should_return400_when_quantityMissing() throws Exception {
        mvc.perform(post("/api/catalog/products/4/stock-adjustment").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    void stockAdjustment_should_return422_when_stockWouldBeNegative() throws Exception {
        when(productService.adjustStock(eq(4L), any())).thenThrow(new BusinessRuleException("Insufficient stock"));

        mvc.perform(post("/api/catalog/products/4/stock-adjustment").with(TestAuth.as(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-99}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Business rule"));
    }

    @Test
    void stockAdjustment_should_return403_when_stylist() throws Exception {
        mvc.perform(post("/api/catalog/products/4/stock-adjustment").with(TestAuth.as(2L, Role.STYLIST))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isForbidden());
    }
}
