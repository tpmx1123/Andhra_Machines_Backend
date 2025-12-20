package com.example.machines.controller;

import com.example.machines.dto.OrderRequest;
import com.example.machines.dto.OrderResponse;
import com.example.machines.service.OrderService;
import com.example.machines.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

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

    private boolean isAdminFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractUsername(token);
                if (email != null && jwtUtil.validateToken(token, email)) {
                    io.jsonwebtoken.Claims claims = jwtUtil.getClaims(token);
                    String role = (String) claims.get("role");
                    return "ADMIN".equals(role);
                }
            } catch (Exception e) {
                // Token invalid or expired
            }
        }
        return false;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            OrderResponse order = orderService.createOrder(userId, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully");
            response.put("data", order);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserOrders(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            List<OrderResponse> orders = orderService.getUserOrders(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            OrderResponse order = orderService.getOrderById(orderId, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllOrders(HttpServletRequest httpRequest) {
        try {
            boolean isAdmin = isAdminFromToken(httpRequest);
            if (!isAdmin) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Admin access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            List<OrderResponse> orders = orderService.getAllOrders();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            boolean isAdmin = isAdminFromToken(httpRequest);
            if (!isAdmin) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Admin access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            String status = request.get("status");
            OrderResponse order = orderService.updateOrderStatus(orderId, status);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated successfully");
            response.put("data", order);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{orderId}/whatsapp-sent")
    public ResponseEntity<Map<String, Object>> markWhatsAppSent(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {
        try {
            boolean isAdmin = isAdminFromToken(httpRequest);
            if (!isAdmin) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Admin access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            OrderResponse order = orderService.markWhatsAppSent(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "WhatsApp sent status updated");
            response.put("data", order);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> deleteOrder(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {
        try {
            boolean isAdmin = isAdminFromToken(httpRequest);
            if (!isAdmin) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Admin access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            orderService.deleteOrder(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

