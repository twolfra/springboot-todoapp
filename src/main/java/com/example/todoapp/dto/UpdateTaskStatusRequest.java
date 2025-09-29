// dto/UpdateTaskStatusRequest.java
package com.example.todoapp.dto;

public class UpdateTaskStatusRequest {
    private boolean done;

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
