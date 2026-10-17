package com.kg.museumly.backend.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException{

    private final String code;
    private final HttpStatus status;

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
