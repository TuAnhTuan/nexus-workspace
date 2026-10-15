package com.todoapp.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Body for PATCH /tasks/{id}/reschedule, e.g. {"dueDate": "2026-10-16"}. */
public record RescheduleRequest(
        @NotNull(message = "Due date is required")
        LocalDate dueDate
) {
}
