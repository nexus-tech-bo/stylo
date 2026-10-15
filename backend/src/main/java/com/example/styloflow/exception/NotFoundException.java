package com.example.styloflow.exception;

import org.springframework.http.HttpStatus;

/** 404: the resource does not exist (or belongs to another business). */
public class NotFoundException extends ApiException {
    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, "Not found", detail);
    }
}
