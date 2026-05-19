package com.budget.buoy.exception;

public class InvalidGoogleTokenException extends RuntimeException{
    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}
