package com.example.machines.controller;

import com.example.machines.entity.User;
import com.example.machines.repository.UserRepository;
import com.example.machines.service.WebSocketService;
import com.example.machines.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebSocketService webSocketService;

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractUsername(token);
                if (email != null && jwtUtil.validateToken(token, email)) {
                    io.jsonwebtoken.Claims claims = jwtUtil.getClaims(token);
                    Object userIdObj = claims.get("userId");
                    if (userIdObj instanceof Number) {
                        return ((Number) userIdObj).longValue();
                    } else if (userIdObj instanceof Integer) {
                        return ((Integer) userIdObj).longValue();
                    }
                }
            } catch (Exception e) {
                // Token invalid or expired
            }
        }
        return null;
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> userData, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields
        if (userData.containsKey("name")) {
            user.setName(userData.get("name"));
        }
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

        // Send WebSocket notification
        webSocketService.sendUserUpdateNotification(user.getId(), "Your profile has been updated");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userMap);
        response.put("message", "Profile updated successfully");
        return ResponseEntity.ok(response);
    }
}

