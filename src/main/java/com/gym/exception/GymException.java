package com.gym.exception;

// Base exception for gym application
public class GymException extends Exception {

    private static final long serialVersionUID = 1L;

    public GymException() {
        super();
    }

    public GymException(String message) {
        super(message);
    }

    public GymException(String message, Throwable cause) {
        super(message, cause);
    }
}
