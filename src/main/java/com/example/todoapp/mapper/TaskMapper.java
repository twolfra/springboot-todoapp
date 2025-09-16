package com.example.todoapp.mapper;

import com.example.todoapp.dto.TaskDTO;
import com.example.todoapp.Task;

public class TaskMapper {

    public static TaskDTO toDTO(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.isDone(),
                task.getOwner()   // ✅ already a String
        );
    }
}
