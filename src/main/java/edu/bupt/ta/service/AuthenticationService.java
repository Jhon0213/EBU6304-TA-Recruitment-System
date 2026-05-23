package edu.bupt.ta.service;

import edu.bupt.ta.dto.LoginResult;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.User;
import edu.bupt.ta.repository.UserRepository;
import edu.bupt.ta.util.PasswordUtils;

import java.util.Optional;

public class AuthenticationService {

    private final UserRepository userRepository;
    private User currentUser;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank()) {
            return LoginResult.fail("Username cannot be empty.");
        }
        if (password == null || password.isBlank()) {
            return LoginResult.fail("Password cannot be empty.");
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return LoginResult.fail("User not found.");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            return LoginResult.fail("Account is inactive.");
        }
        if (!PasswordUtils.matches(password, user.getPasswordHash())) {
            return LoginResult.fail("Incorrect password.");
        }

        this.currentUser = user;
        return LoginResult.success(user);
    }

    public void logout() {
        currentUser = null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    public String updatePassword(String currentPassword, String newPassword) {
        if (currentUser == null) {
            return "Not logged in.";
        }
        if (!PasswordUtils.matches(currentPassword, currentUser.getPasswordHash())) {
            return "Incorrect current password.";
        }
        if (newPassword == null || newPassword.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        currentUser.setPasswordHash(PasswordUtils.sha256(newPassword));
        userRepository.save(currentUser);
        return null;
    }

    public String updateUsername(String newUsername) {
        if (currentUser == null) {
            return "Not logged in.";
        }
        if (newUsername == null || newUsername.isBlank()) {
            return "Username cannot be empty.";
        }
        Optional<User> existing = userRepository.findByUsername(newUsername.trim());
        if (existing.isPresent() && !existing.get().getUserId().equals(currentUser.getUserId())) {
            return "Username already taken.";
        }
        currentUser.setUsername(newUsername.trim());
        userRepository.save(currentUser);
        return null;
    }
}
