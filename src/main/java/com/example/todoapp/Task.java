package com.example.todoapp;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean done = false;

    // 🔗 Link to User entity instead of plain string
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // foreign key column
    private User user;

    public Task() {}

    public Task(String title, User user) {
        this.title = title;
        this.user = user;
    }

    // --- getters & setters ---
    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
