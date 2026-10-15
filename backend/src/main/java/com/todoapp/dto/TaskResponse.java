package com.todoapp.dto;

import com.todoapp.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO returned to the client. Kept separate from the entity so the DB structure can change
 * without breaking the API contract, and so we control exactly which fields are exposed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private boolean completed;
    private int progress;
    private LocalDate dueDate;
    private int postponedCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getProgress(),
                task.getDueDate(),
                task.getPostponedCount(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
