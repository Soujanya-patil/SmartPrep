package com.smartprep.controller;

import com.smartprep.model.User;
import com.smartprep.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        try {
            userService.register(user);
            return ResponseEntity.ok("Registration successful!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Email already exists!");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        Optional<User> found = userService.login(user.getEmail(), user.getPassword());
        if (found.isPresent()) {
            return ResponseEntity.ok("Login successful! Welcome " + found.get().getName());
        }
        return ResponseEntity.badRequest().body("Invalid email or password!");
    }
}