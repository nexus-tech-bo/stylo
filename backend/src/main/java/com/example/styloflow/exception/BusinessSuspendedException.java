package com.example.styloflow.exception;

import org.springframework.http.HttpStatus;

/** 403: the business is suspended. */
public class BusinessSuspendedException extends ApiException {
    public BusinessSuspendedException(String detail) {
        super(HttpStatus.FORBIDDEN, "Business suspended", detail);
    }
}
