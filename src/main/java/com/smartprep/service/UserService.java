package com.smartprep.service;

import com.smartprep.model.User;
import com.smartprep.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public Optional<User> login(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || found.get().getPassword() == null) {
            return Optional.empty();
        }
        User user = found.get();
        String stored = user.getPassword();

        if (isBcryptHash(stored)) {
            return passwordEncoder.matches(password, stored) ? found : Optional.empty();
        }

        // Legacy plain-text password: check it, then upgrade it to a BCrypt hash
        boolean matches = MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            return Optional.empty();
        }
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return found;
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
