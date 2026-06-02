package com.budget.buoy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordResetNotSupportedException extends RuntimeException{
    public PasswordResetNotSupportedException(String message) {
        super(message);
    }
}
