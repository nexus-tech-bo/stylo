package com.example.styloflow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base of the controlled errors; {@link GlobalExceptionHandler} turns it into a ProblemDetail with its status and title. */
@Getter
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String title;

    protected ApiException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }
}
