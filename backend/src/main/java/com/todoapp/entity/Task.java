package com.todoapp.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity = a class mapped 1-1 to a database table (ORM - Object Relational Mapping).
 * @Entity tells JPA this is a table; @Id + @GeneratedValue let Postgres generate the primary key (SERIAL/IDENTITY).
 * Entities are NEVER returned directly from the REST API (that would leak the DB structure and make the API
 * hard to version) -> always map through a DTO.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // completed/progress/postponedCount have NO public setters (AccessLevel.NONE): they must change
    // together, so they're only modified through the methods below. That way no caller can
    // ever produce "completed = true but progress = 40".
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private boolean completed = false;

    /** 0-100 (%). */
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private int progress = 0;

    /** The day the task is planned for; null = not scheduled. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Setter(AccessLevel.NONE)
    @Column(name = "postponed_count", nullable = false)
    private int postponedCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // @PrePersist/@PreUpdate are JPA lifecycle callbacks: they run automatically
    // right before the entity is INSERTed/UPDATEd, so the service doesn't have to set timestamps manually.
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Single place that keeps the invariant completed == (progress == 100). */
    public void updateProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("progress must be between 0 and 100");
        }
        this.progress = progress;
        this.completed = progress == 100;
    }

    /** Done -> 0% (start over), not done -> 100%. */
    public void toggleCompleted() {
        updateProgress(completed ? 0 : 100);
    }

    /** Moving the task to a later day counts as a postponement; moving it earlier doesn't. */
    public void reschedule(LocalDate newDueDate) {
        if (dueDate != null && newDueDate != null && newDueDate.isAfter(dueDate)) {
            postponedCount++;
        }
        this.dueDate = newDueDate;
    }
}
