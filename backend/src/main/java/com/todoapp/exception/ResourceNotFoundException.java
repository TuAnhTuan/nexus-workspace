package com.todoapp.exception;

/**
 * Custom unchecked exception, thrown when a resource cannot be found by id.
 * GlobalExceptionHandler catches it and converts it into a consistent 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
