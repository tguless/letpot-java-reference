package com.letpot.exception;

public class LetPotConnectionException extends RuntimeException {
    
    public LetPotConnectionException(String message) {
        super(message);
    }
    
    public LetPotConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
} 