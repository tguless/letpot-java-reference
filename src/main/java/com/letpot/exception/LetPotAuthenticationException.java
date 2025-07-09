package com.letpot.exception;

public class LetPotAuthenticationException extends RuntimeException {
    
    public LetPotAuthenticationException(String message) {
        super(message);
    }
    
    public LetPotAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
} 