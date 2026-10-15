package com.example.styloflow.exception;

import org.springframework.http.HttpStatus;

/** 401: wrong credentials. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
    }
}
