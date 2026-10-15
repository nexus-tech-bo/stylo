package com.example.styloflow.exception;

import org.springframework.http.HttpStatus;

/** 422: a business rule was violated. */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String detail) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Business rule", detail);
    }
}
