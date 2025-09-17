package com.example.todoapp.mapper;

import com.example.todoapp.dto.TaskDTO;
import com.example.todoapp.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskDTO toDTO(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.isDone(),
                task.getUser() != null ? task.getUser().getUsername() : null
        );
    }
}
