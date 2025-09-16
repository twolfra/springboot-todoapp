package com.example.todoapp.dto;

public class TaskDTO {
    private Long id;
    private String title;
    private boolean done;
    private String owner; // just username, not full User entity

    public TaskDTO(Long id, String title, boolean done, String owner) {
        this.id = id;
        this.title = title;
        this.done = done;
        this.owner = owner;
    }

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
