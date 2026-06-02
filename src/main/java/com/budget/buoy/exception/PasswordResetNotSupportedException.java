package com.budget.buoy.exception;

public class PasswordResetNotSupportedException extends RuntimeException{
    public PasswordResetNotSupportedException(String message) {
        super(message);
    }
}
