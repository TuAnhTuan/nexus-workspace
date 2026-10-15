package com.todoapp.dto;

import java.time.LocalDate;

/**
 * One suggestion for one task.
 * suggestedDate is set when the suggestion is "move it to this day" (OVERDUE, CARRY_OVER), null otherwise.
 */
public record RecommendationResponse(
        Long taskId,
        RecommendationType type,
        LocalDate suggestedDate
) {
}
