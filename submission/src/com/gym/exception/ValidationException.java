package com.gym.exception;

// Thrown when input data or business validation fails
public class ValidationException extends GymException {

    private static final long serialVersionUID = 1L;

    public ValidationException() {
        super();
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
