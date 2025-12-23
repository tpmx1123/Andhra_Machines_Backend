package com.example.machines.service;

import com.example.machines.entity.Favorite;
import com.example.machines.entity.Product;
import com.example.machines.entity.User;
import com.example.machines.repository.FavoriteRepository;
import com.example.machines.repository.ProductRepository;
import com.example.machines.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Transactional
    public Favorite addFavorite(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if favorite already exists
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserIdAndProductId(userId, productId);
        if (existingFavorite.isPresent()) {
            return existingFavorite.get(); // Return existing favorite
        }

        // Apply scheduled price changes to get current price
        productService.applyScheduledPriceChangeForSchedule(product);
        product = productRepository.findById(productId).orElse(product);

        // Create new favorite
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        favorite.setProductName(product.getTitle());
        favorite.setProductImage(product.getMainImageUrl() != null ? product.getMainImageUrl() : product.getImageUrl());
        favorite.setBrandName(product.getBrandName());
        favorite.setBrandSlug(product.getBrandSlug());
        favorite.setPrice(product.getPrice());
        favorite.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice());

        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public List<Favorite> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserId(userId);
    }

    @Transactional
    public boolean isFavorite(Long userId, Long productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void syncFavorites(Long userId, List<Long> productIds) {
        // Get current favorites
        List<Favorite> currentFavorites = favoriteRepository.findByUserId(userId);
        
        // Remove favorites that are not in the new list
        for (Favorite favorite : currentFavorites) {
            if (!productIds.contains(favorite.getProduct().getId())) {
                favoriteRepository.delete(favorite);
            }
        }

        // Add new favorites
        for (Long productId : productIds) {
            if (!favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
                try {
                    addFavorite(userId, productId);
                } catch (Exception e) {
                    // Product might not exist, skip it
                    System.err.println("Error adding favorite for product " + productId + ": " + e.getMessage());
                }
            }
        }
    }
}

