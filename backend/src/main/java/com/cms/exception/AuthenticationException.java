package com.cms.exception;

import lombok.Getter;

@Getter
public class AuthenticationException extends RuntimeException {
    private final String code;

    public AuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
