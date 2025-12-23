package com.example.machines.controller;

import com.example.machines.entity.User;
import com.example.machines.repository.UserRepository;
import com.example.machines.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("updatedAt", user.getUpdatedAt());
            return userMap;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userList);
        response.put("total", userList.size());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, String> userData) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields (email cannot be changed)
        if (userData.containsKey("name")) {
            user.setName(userData.get("name"));
        }
        // Email editing is disabled for security reasons
        // if (userData.containsKey("email")) {
        //     // Check if email is already taken by another user
        //     User existingUser = userRepository.findByEmail(userData.get("email")).orElse(null);
        //     if (existingUser != null && !existingUser.getId().equals(id)) {
        //         Map<String, Object> errorResponse = new HashMap<>();
        //         errorResponse.put("success", false);
        //         errorResponse.put("error", "Email already exists");
        //         return ResponseEntity.badRequest().body(errorResponse);
        //     }
        //     user.setEmail(userData.get("email"));
        // }
        if (userData.containsKey("phone")) {
            user.setPhone(userData.get("phone"));
        }

        user = userRepository.save(user);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());

        // Send WebSocket notification to user
        webSocketService.sendUserUpdateNotification(user.getId(), "Your profile has been updated by admin");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userMap);
        response.put("message", "User updated successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }
}

