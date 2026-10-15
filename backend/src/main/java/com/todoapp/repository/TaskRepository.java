package com.todoapp.repository;

import com.todoapp.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository: just declare the interface and Spring generates the implementation
 * (CRUD, pagination...) at runtime based on method names and the generic types <Task, Long>.
 * No hand-written SQL/HQL is needed for basic operations.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Derived query: Spring parses the method name and generates
     * SELECT ... FROM tasks WHERE completed = false. Used by the recommendation rules,
     * which only ever look at unfinished tasks.
     */
    List<Task> findByCompletedFalse();
}
