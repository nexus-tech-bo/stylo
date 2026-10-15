package com.example.styloflow.exception;

import org.springframework.http.HttpStatus;

/** 403: authenticated but not allowed on this resource. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN, "Forbidden", detail);
    }
}
