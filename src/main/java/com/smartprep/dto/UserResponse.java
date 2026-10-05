package com.smartprep.dto;

import com.smartprep.model.User;

public class UserResponse {
    private int id;
    private String name;
    private String email;
    private User.Role role;

    // Constructor
    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public User.Role getRole() { return role; }
}
