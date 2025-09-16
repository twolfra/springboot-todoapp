package com.example.todoapp;

import com.example.todoapp.dto.TaskDTO;
import com.example.todoapp.dto.CreateTaskRequest;
import com.example.todoapp.exception.ForbiddenException;
import com.example.todoapp.mapper.TaskMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskRepository repo;

    public TaskController(TaskRepository repo) {
        this.repo = repo;
    }

    // Get tasks (users see only their own, admins see all)
    @GetMapping
    public List<TaskDTO> getTasks(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN"));

        List<Task> tasks = isAdmin
                ? repo.findAll()
                : repo.findByOwner(auth.getName());

        return tasks.stream()
                .map(TaskMapper::toDTO)
                .toList();
    }

    // Add task → always linked to logged-in user
    @PostMapping
    public TaskDTO addTask(@Valid @RequestBody CreateTaskRequest req, Authentication auth) {
        Task task = new Task();
        task.setTitle(req.getTitle());
        task.setDone(req.isDone());
        task.setOwner(auth.getName());

        Task saved = repo.save(task);
        return TaskMapper.toDTO(saved);
    }

    // Update task → only owner or admin
    @PutMapping("/{id}")
    public TaskDTO updateTask(@PathVariable Long id,
                              @Valid @RequestBody CreateTaskRequest req,
                              Authentication auth) {
        return repo.findById(id).map(task -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> r.equals("ROLE_ADMIN"));

            if (!isAdmin && !task.getOwner().equals(auth.getName())) {
                throw new ForbiddenException("You are not allowed to update this task");
            }

            task.setTitle(req.getTitle());
            task.setDone(req.isDone());

            Task updated = repo.save(task);
            return TaskMapper.toDTO(updated);
        }).orElseThrow(() -> new RuntimeException("Task not found with id " + id));
    }

    // Delete task → only admin (already enforced in SecurityConfig)
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
