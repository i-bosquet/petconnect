package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidTemporaryTokenException extends RuntimeException {
    public InvalidTemporaryTokenException(String message) {
        super(message);
    }
}
