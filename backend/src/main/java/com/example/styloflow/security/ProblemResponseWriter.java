package com.example.styloflow.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/** Writes ProblemDetail bodies from the security filter chain, where the controller advice does not apply. */
@Component
@RequiredArgsConstructor
public class ProblemResponseWriter {
    private final JsonMapper jsonMapper;

    public void write(HttpServletResponse response, HttpStatus status, String title, String detail) throws IOException {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), pd);
    }
}
