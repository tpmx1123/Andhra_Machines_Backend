package com.example.machines.service;

import com.example.machines.dto.AuthResponse;
import com.example.machines.dto.ForgotPasswordRequest;
import com.example.machines.dto.LoginRequest;
import com.example.machines.dto.RegisterRequest;
import com.example.machines.dto.ResetPasswordRequest;
import com.example.machines.entity.Admin;
import com.example.machines.entity.User;
import com.example.machines.repository.AdminRepository;
import com.example.machines.repository.UserRepository;
import com.example.machines.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";

    public AuthResponse registerUser(RegisterRequest request) {
        if (!request.getEmail().matches(EMAIL_PATTERN)) {
            return new AuthResponse(false, "Invalid email format", null);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "Email already exists", null);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());

        user = userRepository.save(user);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        String token = jwtUtil.generateToken(user.getEmail(), "USER", user.getId());
        
        String createdAt = user.getCreatedAt() != null 
            ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;

        AuthResponse.AuthData authData = new AuthResponse.AuthData(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                "USER",
                token,
                createdAt
        );

        return new AuthResponse(true, "User registered successfully", authData);
    }

    public AuthResponse registerAdmin(RegisterRequest request) {
        if (adminRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "Email already exists", null);
        }

        Admin admin = new Admin();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setPhone(request.getPhone());

        admin = adminRepository.save(admin);

        String token = jwtUtil.generateToken(admin.getEmail(), "ADMIN", admin.getId());
        
        String createdAt = admin.getCreatedAt() != null 
            ? admin.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;

        AuthResponse.AuthData authData = new AuthResponse.AuthData(
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getPhone(),
                "ADMIN",
                token,
                createdAt
        );

        return new AuthResponse(true, "Admin registered successfully", authData);
    }

    public AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(false, "Invalid email or password", null);
        }

        String token = jwtUtil.generateToken(user.getEmail(), "USER", user.getId());
        
        String createdAt = user.getCreatedAt() != null 
            ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;

        AuthResponse.AuthData authData = new AuthResponse.AuthData(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                "USER",
                token,
                createdAt
        );

        return new AuthResponse(true, "Login successful", authData);
    }

    public AuthResponse loginAdmin(LoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return new AuthResponse(false, "Invalid email or password", null);
        }

        String token = jwtUtil.generateToken(admin.getEmail(), "ADMIN", admin.getId());
        
        String createdAt = admin.getCreatedAt() != null 
            ? admin.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;

        AuthResponse.AuthData authData = new AuthResponse.AuthData(
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getPhone(),
                "ADMIN",
                token,
                createdAt
        );

        return new AuthResponse(true, "Login successful", authData);
    }

    public AuthResponse loginUnified(LoginRequest request) {
        // Try admin first
        Admin admin = adminRepository.findByEmail(request.getEmail()).orElse(null);
        if (admin != null && passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            String token = jwtUtil.generateToken(admin.getEmail(), "ADMIN", admin.getId());
            String createdAt = admin.getCreatedAt() != null 
                ? admin.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
            AuthResponse.AuthData authData = new AuthResponse.AuthData(
                    admin.getId(), admin.getName(), admin.getEmail(), admin.getPhone(), "ADMIN", token, createdAt
            );
            return new AuthResponse(true, "Admin login successful", authData);
        }

        // Try user
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user != null && passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            String token = jwtUtil.generateToken(user.getEmail(), "USER", user.getId());
            String createdAt = user.getCreatedAt() != null 
                ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
            AuthResponse.AuthData authData = new AuthResponse.AuthData(
                    user.getId(), user.getName(), user.getEmail(), user.getPhone(), "USER", token, createdAt
            );
            return new AuthResponse(true, "User login successful", authData);
        }

        return new AuthResponse(false, "Invalid email or password", null);
    }

    public ResponseEntity<?> getCurrentUser(String token) {
        try {
            Claims claims = jwtUtil.getClaims(token);
            String role = (String) claims.get("role");
            Long userId = ((Number) claims.get("userId")).longValue();

            Map<String, Object> userData = new HashMap<>();
            
            if ("ADMIN".equals(role)) {
                Admin admin = adminRepository.findById(userId).orElse(null);
                if (admin == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("success", false, "message", "User not found"));
                }
                userData.put("id", admin.getId());
                userData.put("name", admin.getName());
                userData.put("email", admin.getEmail());
                userData.put("phone", admin.getPhone());
                userData.put("role", "ADMIN");
            } else {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("success", false, "message", "User not found"));
                }
                userData.put("id", user.getId());
                userData.put("name", user.getName());
                userData.put("email", user.getEmail());
                userData.put("phone", user.getPhone());
                userData.put("role", "USER");
            }

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid or expired token"));
        }
    }

    public ResponseEntity<Map<String, Object>> forgotPassword(ForgotPasswordRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            
            // Always return success message for security (don't reveal if email exists)
            if (user == null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "If an account with that email exists, a password reset link has been sent."
                ));
            }

            // Generate secure reset token
            String resetToken = generateResetToken();
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(1); // Token valid for 1 hour

            // Save token to user
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(expiryTime);
            userRepository.save(user);

            // Send password reset email
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetToken);
            } catch (Exception e) {
                System.err.println("Failed to send password reset email: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "If an account with that email exists, a password reset link has been sent."
            ));
        } catch (Exception e) {
            System.err.println("Error in forgot password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An error occurred. Please try again."));
        }
    }

    public ResponseEntity<Map<String, Object>> resetPassword(ResetPasswordRequest request) {
        try {
            // Find user by reset token
            User user = userRepository.findByResetToken(request.getToken()).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Invalid or expired reset token."));
            }

            // Check if token is expired
            if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                // Clear expired token
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Reset token has expired. Please request a new one."));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password has been reset successfully. You can now login with your new password."
            ));
        } catch (Exception e) {
            System.err.println("Error in reset password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An error occurred. Please try again."));
        }
    }

    private String generateResetToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

