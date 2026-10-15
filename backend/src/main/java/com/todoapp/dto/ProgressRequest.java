package com.todoapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Body for PATCH /tasks/{id}/progress. A record is enough for a small immutable DTO:
 * Java generates the constructor, accessor and equals/hashCode, and Jackson can deserialize it.
 */
public record ProgressRequest(
        @NotNull(message = "Progress is required")
        @Min(value = 0, message = "Progress must be between 0 and 100")
        @Max(value = 100, message = "Progress must be between 0 and 100")
        Integer progress
) {
}
