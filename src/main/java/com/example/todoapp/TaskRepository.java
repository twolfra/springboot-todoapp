package com.example.todoapp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Spring Data JPA will join Task.user.username automatically
    List<Task> findByUserUsername(String username);
}
