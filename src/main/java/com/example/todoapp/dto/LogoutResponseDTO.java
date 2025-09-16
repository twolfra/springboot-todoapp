package com.example.todoapp.dto;

public class LogoutResponseDTO {
    private String message;

    public LogoutResponseDTO(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
