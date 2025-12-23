package com.example.machines.controller;

import com.example.machines.dto.CartItemResponse;
import com.example.machines.dto.CartResponse;
import com.example.machines.entity.Cart;
import com.example.machines.entity.CartItem;
import com.example.machines.service.CartService;
import com.example.machines.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

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

    @GetMapping
    public ResponseEntity<?> getCart(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Cart cart = cartService.getCart(userId);
            CartResponse response = convertToResponse(cart);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItem(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Long productId = Long.valueOf(requestData.get("productId").toString());
            Integer quantity = requestData.get("quantity") != null 
                    ? Integer.valueOf(requestData.get("quantity").toString()) 
                    : 1;

            Cart cart = cartService.addItemToCart(userId, productId, quantity);
            CartResponse response = convertToResponse(cart);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Item added to cart"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateItem(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Long productId = Long.valueOf(requestData.get("productId").toString());
            Integer quantity = Integer.valueOf(requestData.get("quantity").toString());

            Cart cart = cartService.updateItemQuantity(userId, productId, quantity);
            CartResponse response = convertToResponse(cart);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Cart updated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeItem(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Cart cart = cartService.removeItemFromCart(userId, productId);
            CartResponse response = convertToResponse(cart);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Item removed from cart"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Cart cart = cartService.clearCart(userId);
            CartResponse response = convertToResponse(cart);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Cart cleared"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/sync-prices")
    public ResponseEntity<?> syncPrices(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            cartService.syncCartPrices(userId);
            Cart cart = cartService.getCart(userId);
            CartResponse response = convertToResponse(cart);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Prices synced"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private CartResponse convertToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::convertItemToResponse)
                .collect(Collectors.toList());

        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalPrice = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, totalItems, totalPrice);
    }

    private CartItemResponse convertItemToResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getBrandName(),
                item.getBrandSlug(),
                item.getProductImage(),
                item.getQuantity(),
                item.getPrice(),
                item.getOriginalPrice(),
                item.getProduct().getInStock()
        );
    }
}

