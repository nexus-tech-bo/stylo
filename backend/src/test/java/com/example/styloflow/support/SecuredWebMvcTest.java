package com.example.styloflow.support;

import com.example.styloflow.config.SecurityConfig;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.UserRepository;
import com.example.styloflow.security.JwtService;
import com.example.styloflow.security.ProblemResponseWriter;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put next to {@code @WebMvcTest(XController.class)} to run the real security chain (URL rules, 401/403 bodies,
 * {@code @PreAuthorize}). Authenticate requests with {@link TestAuth}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, ProblemResponseWriter.class})
@MockitoBean(types = {JwtService.class, BusinessRepository.class, UserRepository.class})
public @interface SecuredWebMvcTest {
}
