package com.todoapp.service;

import com.todoapp.dto.RecommendationResponse;
import com.todoapp.dto.RecommendationType;
import com.todoapp.dto.TaskRequest;
import com.todoapp.dto.TaskResponse;
import com.todoapp.entity.Task;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TaskService: the repository is a Mockito mock (no DB) and the Clock is fixed,
 * so each test controls exactly "what day and time it is".
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private TaskRepository taskRepository;

    private TaskService serviceAt(int hour, int minute) {
        LocalDateTime now = TODAY.atTime(hour, minute);
        Clock clock = Clock.fixed(now.atZone(ZONE).toInstant(), ZONE);
        return new TaskService(taskRepository, clock);
    }

    private static Task task(long id, LocalDate dueDate, int progress) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Task " + id);
        task.setDueDate(dueDate);
        task.updateProgress(progress);
        return task;
    }

    // ---------- recommendations ----------

    @Test
    void overdueTask_isSuggestedToMoveToToday() {
        when(taskRepository.findByCompletedFalse()).thenReturn(List.of(task(1, TODAY.minusDays(2), 50)));

        List<RecommendationResponse> result = serviceAt(9, 0).getRecommendations();

        assertThat(result).containsExactly(new RecommendationResponse(1L, RecommendationType.OVERDUE, TODAY));
    }

    @Test
    void todaysUnfinishedTask_inTheEvening_isSuggestedToMoveToTomorrow() {
        when(taskRepository.findByCompletedFalse()).thenReturn(List.of(task(1, TODAY, 80)));

        List<RecommendationResponse> result = serviceAt(17, 30).getRecommendations();

        assertThat(result).containsExactly(
                new RecommendationResponse(1L, RecommendationType.CARRY_OVER, TODAY.plusDays(1)));
    }

    @Test
    void todaysTaskWithLowProgress_inTheAfternoon_getsFocus() {
        when(taskRepository.findByCompletedFalse()).thenReturn(List.of(task(1, TODAY, 10), task(2, TODAY, 50)));

        List<RecommendationResponse> result = serviceAt(14, 0).getRecommendations();

        // Only task 1 is under the 30% threshold.
        assertThat(result).containsExactly(new RecommendationResponse(1L, RecommendationType.FOCUS, null));
    }

    @Test
    void todaysTask_inTheMorning_hasNoSuggestion() {
        when(taskRepository.findByCompletedFalse()).thenReturn(List.of(task(1, TODAY, 0)));

        assertThat(serviceAt(9, 0).getRecommendations()).isEmpty();
    }

    @Test
    void futureOrUnscheduledTasks_haveNoDaySuggestion() {
        when(taskRepository.findByCompletedFalse())
                .thenReturn(List.of(task(1, TODAY.plusDays(3), 0), task(2, null, 0)));

        assertThat(serviceAt(20, 0).getRecommendations()).isEmpty();
    }

    @Test
    void taskPostponedThreeTimes_isSuggestedToBeSplit_alongsideDaySuggestion() {
        Task task = task(1, TODAY.minusDays(3), 20);
        task.reschedule(TODAY.minusDays(2));
        task.reschedule(TODAY.minusDays(1));
        task.reschedule(TODAY); // 3 postponements, now due today
        when(taskRepository.findByCompletedFalse()).thenReturn(List.of(task));

        List<RecommendationResponse> result = serviceAt(18, 0).getRecommendations();

        assertThat(result).extracting(RecommendationResponse::type)
                .containsExactly(RecommendationType.CARRY_OVER, RecommendationType.SPLIT);
    }

    // ---------- progress / reschedule ----------

    @Test
    void updateProgress_to100_completesTheTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task(1, TODAY, 40)));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = serviceAt(10, 0).updateProgress(1L, 100);

        assertThat(result.getProgress()).isEqualTo(100);
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void reschedule_toLaterDay_incrementsPostponedCount() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task(1, TODAY, 40)));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = serviceAt(18, 0).reschedule(1L, TODAY.plusDays(1));

        assertThat(result.getDueDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(result.getPostponedCount()).isEqualTo(1);
    }

    @Test
    void create_withoutProgress_defaultsToZero() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        TaskRequest request = new TaskRequest();
        request.setTitle("New");

        TaskResponse result = serviceAt(10, 0).create(request);

        assertThat(result.getProgress()).isZero();
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void unknownId_throwsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAt(10, 0).updateProgress(99L, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
