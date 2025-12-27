package com.example.machines.controller;

import com.example.machines.dto.AuthResponse;
import com.example.machines.dto.ForgotPasswordRequest;
import com.example.machines.dto.LoginRequest;
import com.example.machines.dto.RegisterRequest;
import com.example.machines.dto.ResetPasswordRequest;
import com.example.machines.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register/user")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.registerUser(request);
        if (response.isSuccess() && response.getData() != null && response.getData().getToken() != null) {
            setAuthCookie(httpResponse, response.getData().getToken());
        }
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerAdmin(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/login/user")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.loginUser(request);
        if (response.isSuccess() && response.getData() != null && response.getData().getToken() != null) {
            setAuthCookie(httpResponse, response.getData().getToken());
        }
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/login/admin")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.loginAdmin(request);
        if (response.isSuccess() && response.getData() != null && response.getData().getToken() != null) {
            setAuthCookie(httpResponse, response.getData().getToken());
        }
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUnified(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.loginUnified(request);
        if (response.isSuccess() && response.getData() != null && response.getData().getToken() != null) {
            setAuthCookie(httpResponse, response.getData().getToken());
        }
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        String token = null;
        
        // First try to get token from cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("authToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        
        // Fallback to Authorization header if cookie not found
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Unauthorized"));
        }
        
        return authService.getCurrentUser(token);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse httpResponse) {
        clearAuthCookie(httpResponse);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("authToken", token);
        cookie.setHttpOnly(true); // Prevent XSS attacks
        cookie.setSecure(true); // Only send over HTTPS in production
        cookie.setPath("/"); // Available for all paths
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        // Set SameSite attribute to prevent CSRF
        response.addHeader("Set-Cookie", 
            String.format("authToken=%s; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=%d", 
                token, 7 * 24 * 60 * 60));
    }

    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("authToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete cookie
        
        // Check if running in production (HTTPS)
        String sslEnabled = System.getProperty("server.ssl.enabled", "false");
        String httpsEnv = System.getenv("HTTPS");
        String productionEnv = System.getenv("PRODUCTION");
        
        boolean isProduction = "true".equals(sslEnabled) || 
                              "true".equals(httpsEnv) ||
                              "true".equals(productionEnv);
        
        if (isProduction) {
            cookie.setSecure(true);
            response.addHeader("Set-Cookie", 
                "authToken=; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=0");
        } else {
            response.addHeader("Set-Cookie", 
                "authToken=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0");
        }
        response.addCookie(cookie);
    }
}

