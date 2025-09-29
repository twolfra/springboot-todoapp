package com.example.todoapp;

import com.example.todoapp.dto.TaskDTO;
import com.example.todoapp.dto.CreateTaskRequest;
import com.example.todoapp.dto.UpdateTaskStatusRequest;
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

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskController(TaskRepository taskRepository,
                          UserRepository userRepository,
                          TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    // Get tasks (users see only their own, admins see all)
    @GetMapping
    public List<TaskDTO> getTasks(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN"));

        List<Task> tasks = isAdmin
                ? taskRepository.findAll()
                : taskRepository.findByUserUsername(auth.getName()); // 👈 updated repo method

        return tasks.stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    // Add task → always linked to logged-in user
    @PostMapping
    public TaskDTO addTask(@Valid @RequestBody CreateTaskRequest req, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Task task = new Task();
        task.setTitle(req.getTitle());
        task.setDone(req.isDone());
        task.setUser(user);

        Task saved = taskRepository.save(task);
        return taskMapper.toDTO(saved);
    }

    // Update task → only owner or admin
    @PutMapping("/{id}")
    public TaskDTO updateTask(@PathVariable Long id,
                              @Valid @RequestBody CreateTaskRequest req,
                              Authentication auth) {
        return taskRepository.findById(id).map(task -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> r.equals("ROLE_ADMIN"));

            if (!isAdmin && !task.getUser().getUsername().equals(auth.getName())) {
                throw new ForbiddenException("You are not allowed to update this task");
            }

            task.setTitle(req.getTitle());
            task.setDone(req.isDone());

            Task updated = taskRepository.save(task);
            return taskMapper.toDTO(updated);
        }).orElseThrow(() -> new RuntimeException("Task not found with id " + id));
    }

    // Delete task → only admin (already enforced in SecurityConfig)
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id, Authentication auth) {
        taskRepository.findById(id).ifPresent(task -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> r.equals("ROLE_ADMIN"));

            if (!isAdmin && !task.getUser().getUsername().equals(auth.getName())) {
                throw new ForbiddenException("You are not allowed to delete this task");
            }

            taskRepository.delete(task);
        });
    }
    @PatchMapping("/{id}/done")
    public TaskDTO toggleDone(@PathVariable Long id,
                              @RequestBody UpdateTaskStatusRequest req,
                              Authentication auth) {
        return taskRepository.findById(id).map(task -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> r.equals("ROLE_ADMIN"));

            if (!isAdmin && !task.getUser().getUsername().equals(auth.getName())) {
                throw new ForbiddenException("You are not allowed to update this task");
            }

            task.setDone(req.isDone());
            Task updated = taskRepository.save(task);
            return taskMapper.toDTO(updated);
        }).orElseThrow(() -> new RuntimeException("Task not found with id " + id));
    }


}
