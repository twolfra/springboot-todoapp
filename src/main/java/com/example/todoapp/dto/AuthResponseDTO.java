package com.example.todoapp.dto;

public class AuthResponseDTO {
    private String message;
    private UserDTO user;

    public AuthResponseDTO(String message, UserDTO user) {
        this.message = message;
        this.user = user;
    }

    public String getMessage() { return message; }
    public UserDTO getUser() { return user; }
}
