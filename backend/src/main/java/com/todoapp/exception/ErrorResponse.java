package com.todoapp.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * A single error format returned for every kind of exception, so the frontend can handle errors
 * with one structure instead of each endpoint returning something different.
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private List<String> details;
}
