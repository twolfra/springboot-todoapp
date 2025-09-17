package com.example.todoapp.dto;

import java.util.Collections;
import java.util.Set;

public class UserDTO {
    private String username;
    private Set<String> roles;

    public UserDTO(String username, Set<String> roles) {
        this.username = username;
        // make sure roles is never null
        this.roles = (roles == null) ? Collections.emptySet() : roles;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
