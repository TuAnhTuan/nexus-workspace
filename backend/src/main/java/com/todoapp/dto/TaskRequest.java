package com.todoapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) used as the request body for POST/PUT.
 * Bean Validation (@NotBlank, @Size...) runs when the controller receives @Valid @RequestBody,
 * validating input right at the "entry point" without hand-written if/else checks in the service.
 *
 * There is no `completed` field any more: it's derived from progress (100% = completed),
 * so the client can't send a contradictory pair.
 */
@Getter
@Setter
public class TaskRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    /** Optional; JSON "yyyy-MM-dd". */
    private LocalDate dueDate;

    /** Optional, defaults to 0. Integer (not int) so "not sent" can be told apart from 0. */
    @Min(value = 0, message = "Progress must be between 0 and 100")
    @Max(value = 100, message = "Progress must be between 0 and 100")
    private Integer progress;
}
