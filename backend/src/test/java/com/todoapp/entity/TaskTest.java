package com.todoapp.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests for the entity's own rules — no Spring, no DB, so they run in milliseconds. */
class TaskTest {

    @Test
    void updateProgress_100_marksCompleted_andLowerUnmarksIt() {
        Task task = new Task();

        task.updateProgress(100);
        assertThat(task.isCompleted()).isTrue();

        task.updateProgress(60);
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.getProgress()).isEqualTo(60);
    }

    @Test
    void updateProgress_outOfRange_throws() {
        Task task = new Task();
        assertThatThrownBy(() -> task.updateProgress(101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> task.updateProgress(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toggleCompleted_switchesBetween0And100() {
        Task task = new Task();
        task.updateProgress(40);

        task.toggleCompleted();
        assertThat(task.getProgress()).isEqualTo(100);
        assertThat(task.isCompleted()).isTrue();

        task.toggleCompleted();
        assertThat(task.getProgress()).isZero();
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    void reschedule_onlyCountsMovesToALaterDay() {
        Task task = new Task();
        LocalDate day = LocalDate.of(2026, 10, 15);

        task.reschedule(day);                  // first schedule: not a postponement
        task.reschedule(day.plusDays(1));      // later: +1
        task.reschedule(day);                  // earlier: no change
        task.reschedule(day.plusDays(2));      // later: +1

        assertThat(task.getPostponedCount()).isEqualTo(2);
        assertThat(task.getDueDate()).isEqualTo(day.plusDays(2));
    }
}
