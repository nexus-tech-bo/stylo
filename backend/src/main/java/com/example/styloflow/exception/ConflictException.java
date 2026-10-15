package com.example.styloflow.exception;

import org.springframework.http.HttpStatus;

/** 409: duplicate value or record in use by other data. */
public class ConflictException extends ApiException {
    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, "Conflict", detail);
    }
}
