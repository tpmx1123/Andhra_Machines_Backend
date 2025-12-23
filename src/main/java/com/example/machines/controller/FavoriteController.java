package com.example.machines.controller;

import com.example.machines.dto.FavoriteResponse;
import com.example.machines.entity.Favorite;
import com.example.machines.service.FavoriteService;
import com.example.machines.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

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
    public ResponseEntity<?> getFavorites(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            List<Favorite> favorites = favoriteService.getUserFavorites(userId);
            List<FavoriteResponse> favoriteResponses = favorites.stream()
                    .map(fav -> new FavoriteResponse(
                            fav.getProduct().getId(),
                            fav.getProductName(),
                            fav.getBrandName(),
                            fav.getBrandSlug(),
                            fav.getPrice(),
                            fav.getOriginalPrice(),
                            fav.getProductImage(),
                            fav.getProduct().getInStock() != null ? fav.getProduct().getInStock() : true
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("success", true, "data", favoriteResponses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addFavorite(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Long productId = Long.valueOf(requestData.get("productId").toString());
            Favorite favorite = favoriteService.addFavorite(userId, productId);
            FavoriteResponse response = new FavoriteResponse(
                    favorite.getProduct().getId(),
                    favorite.getProductName(),
                    favorite.getBrandName(),
                    favorite.getBrandSlug(),
                    favorite.getPrice(),
                    favorite.getOriginalPrice(),
                    favorite.getProductImage(),
                    favorite.getProduct().getInStock() != null ? favorite.getProduct().getInStock() : true
            );
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "Favorite added successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            favoriteService.removeFavorite(userId, productId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Favorite removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("success", true, "isFavorite", false));
        }

        try {
            boolean isFavorite = favoriteService.isFavorite(userId, productId);
            return ResponseEntity.ok(Map.of("success", true, "isFavorite", isFavorite));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncFavorites(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            List<Long> productIds = ((List<?>) requestData.get("productIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());
            favoriteService.syncFavorites(userId, productIds);
            return ResponseEntity.ok(Map.of("success", true, "message", "Favorites synced successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}

