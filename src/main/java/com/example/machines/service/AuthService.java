package com.example.machines.service;

import com.example.machines.dto.AuthResponse;
import com.example.machines.dto.LoginRequest;
import com.example.machines.dto.RegisterRequest;
import com.example.machines.entity.Admin;
import com.example.machines.entity.User;
import com.example.machines.repository.AdminRepository;
import com.example.machines.repository.UserRepository;
import com.example.machines.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

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
}

