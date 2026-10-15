package com.todoapp.service;

import com.todoapp.dto.RecommendationResponse;
import com.todoapp.dto.RecommendationType;
import com.todoapp.dto.TaskRequest;
import com.todoapp.dto.TaskResponse;
import com.todoapp.entity.Task;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service = where business logic lives, separate from the controller (HTTP only) and the repository
 * (DB queries only). @RequiredArgsConstructor (Lombok) generates a constructor for final fields
 * -> Spring uses constructor injection, which makes the class easy to test (mock the repository) and immutable.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    // Recommendation thresholds. Constants (not magic numbers inside if's) so the rules read clearly
    // and can later move to application.yml if they need tuning.
    static final LocalTime FOCUS_FROM = LocalTime.NOON;
    static final LocalTime CARRY_OVER_FROM = LocalTime.of(17, 0);
    static final int FOCUS_PROGRESS_BELOW = 30;
    static final int SPLIT_AFTER_POSTPONES = 3;

    private final TaskRepository taskRepository;
    private final Clock clock;

    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    public TaskResponse findById(Long id) {
        Task task = getTaskOrThrow(id);
        return TaskResponse.fromEntity(task);
    }

    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        applyRequest(task, request);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getTaskOrThrow(id);
        applyRequest(task, request);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public TaskResponse toggleCompleted(Long id) {
        Task task = getTaskOrThrow(id);
        task.toggleCompleted();
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public TaskResponse updateProgress(Long id, int progress) {
        Task task = getTaskOrThrow(id);
        task.updateProgress(progress);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public TaskResponse reschedule(Long id, LocalDate dueDate) {
        Task task = getTaskOrThrow(id);
        task.reschedule(dueDate);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public void delete(Long id) {
        Task task = getTaskOrThrow(id);
        taskRepository.delete(task);
    }

    /**
     * Rule-based suggestions for unfinished tasks, evaluated against "now" from the injected Clock.
     * Only suggests — nothing is changed here; the user decides by calling reschedule.
     *
     * Per task, at most one of OVERDUE / CARRY_OVER / FOCUS (they're about the same day plan, checked
     * from most to least urgent), plus SPLIT independently (it's about the task's size, not the day).
     */
    public List<RecommendationResponse> getRecommendations() {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        List<RecommendationResponse> result = new ArrayList<>();

        for (Task task : taskRepository.findByCompletedFalse()) {
            LocalDate due = task.getDueDate();

            if (due != null && due.isBefore(today)) {
                result.add(new RecommendationResponse(task.getId(), RecommendationType.OVERDUE, today));
            } else if (Objects.equals(due, today) && !now.isBefore(CARRY_OVER_FROM)) {
                result.add(new RecommendationResponse(task.getId(), RecommendationType.CARRY_OVER, today.plusDays(1)));
            } else if (Objects.equals(due, today) && !now.isBefore(FOCUS_FROM)
                    && task.getProgress() < FOCUS_PROGRESS_BELOW) {
                result.add(new RecommendationResponse(task.getId(), RecommendationType.FOCUS, null));
            }

            if (task.getPostponedCount() >= SPLIT_AFTER_POSTPONES) {
                result.add(new RecommendationResponse(task.getId(), RecommendationType.SPLIT, null));
            }
        }
        return result;
    }

    /** POST and PUT share the same mapping; PUT is a full replace, so missing progress means 0. */
    private void applyRequest(Task task, TaskRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.updateProgress(request.getProgress() != null ? request.getProgress() : 0);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id = " + id));
    }
}
