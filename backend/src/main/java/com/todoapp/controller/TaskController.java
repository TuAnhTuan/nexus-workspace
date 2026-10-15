package com.todoapp.controller;

import com.todoapp.dto.ProgressRequest;
import com.todoapp.dto.RecommendationResponse;
import com.todoapp.dto.RescheduleRequest;
import com.todoapp.dto.TaskRequest;
import com.todoapp.dto.TaskResponse;
import com.todoapp.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller = the layer that receives HTTP requests; it only maps URL/method -> service call
 * and holds no business logic. @RestController = @Controller + @ResponseBody (return values are
 * serialized to JSON automatically). @Valid triggers Bean Validation on TaskRequest.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll() {
        return ResponseEntity.ok(taskService.findAll());
    }

    // A literal path segment wins over the {id} pattern, so this doesn't clash with GET /{id}.
    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationResponse>> recommendations() {
        return ResponseEntity.ok(taskService.getRecommendations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TaskResponse> toggleCompleted(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.toggleCompleted(id));
    }

    // PATCH = change one aspect of the task, without resending the whole object like PUT.
    @PatchMapping("/{id}/progress")
    public ResponseEntity<TaskResponse> updateProgress(@PathVariable Long id,
                                                       @Valid @RequestBody ProgressRequest request) {
        return ResponseEntity.ok(taskService.updateProgress(id, request.progress()));
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<TaskResponse> reschedule(@PathVariable Long id,
                                                   @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(taskService.reschedule(id, request.dueDate()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
