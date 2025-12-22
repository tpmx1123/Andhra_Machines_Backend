package com.example.machines.service;

import com.example.machines.dto.ProductRequest;
import com.example.machines.dto.ProductResponse;
import com.example.machines.entity.Product;
import com.example.machines.repository.ProductRepository;
import com.example.machines.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    public List<ProductResponse> getAllProducts() {
        // Only return active products for public listing
        List<Product> products = productRepository.findByIsActiveTrue();
        // Apply scheduled price changes
        products.forEach(this::applyScheduledPriceChange);
        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    // Admin method to get all products including inactive ones
    public List<ProductResponse> getAllProductsForAdmin() {
        List<Product> products = productRepository.findAll();
        // Apply scheduled price changes
        products.forEach(this::applyScheduledPriceChange);
        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        applyScheduledPriceChange(product);
        return convertToResponse(product);
    }

    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findByBrandSlug(slug)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        applyScheduledPriceChange(product);
        return convertToResponse(product);
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        applyRequestToProduct(product, request);

        product = productRepository.save(product);
        return convertToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        applyRequestToProduct(product, request);

        product = productRepository.save(product);
        return convertToResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Instead of hard delete, use soft delete to preserve order history
        // This prevents foreign key constraint errors with order_items table
        // Set product as inactive and out of stock
        product.setIsActive(false);
        product.setInStock(false);
        
        // Keep reviews for historical data - don't delete them
        // The product will no longer appear in public listings but order history is preserved
        
        // Save the updated product (soft delete)
        productRepository.save(product);
        
        // Note: This is a soft delete - the product still exists in the database
        // but is marked as inactive, so it won't appear in public product listings
    }

    private void applyRequestToProduct(Product product, ProductRequest request) {
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());

        // Brand
        product.setBrandName(request.getBrandName());
        product.setBrandSlug(request.getBrandSlug());
        product.setBrandLogoUrl(request.getBrandLogoUrl());

        // Prices
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());

        // Images
        product.setImageUrl(request.getImageUrl());
        product.setMainImageUrl(request.getMainImageUrl() != null ? request.getMainImageUrl() : request.getImageUrl());
        product.setGalleryImages(request.getGalleryImages());

        // Status / stock
        product.setIsActive(request.getIsActive());
        product.setInStock(request.getInStock());
        product.setStockQuantity(request.getStockQuantity());

        // Badges
        product.setIsOnSale(request.getIsOnSale());
        product.setIsNew(request.getIsNew());

        // Price scheduling
        // Store original price before schedule if scheduling is being set for the first time
        if (request.getScheduledPrice() != null && request.getPriceStartDate() != null && 
            request.getPriceEndDate() != null) {
            // If original price before schedule is not set, store current price
            if (product.getOriginalPriceBeforeSchedule() == null) {
                product.setOriginalPriceBeforeSchedule(product.getPrice());
            }
            
            product.setScheduledPrice(request.getScheduledPrice());
            product.setPriceStartDate(request.getPriceStartDate());
            product.setPriceEndDate(request.getPriceEndDate());
        } else {
            // If scheduling is being cleared, restore original price if it exists
            if (product.getOriginalPriceBeforeSchedule() != null && 
                product.getPrice().equals(product.getScheduledPrice())) {
                product.setPrice(product.getOriginalPriceBeforeSchedule());
            }
            product.setScheduledPrice(null);
            product.setPriceStartDate(null);
            product.setPriceEndDate(null);
            product.setOriginalPriceBeforeSchedule(null);
        }

        // Highlights / specs
        product.setHighlights(request.getHighlights());
        // Handle specificationsJson - allow null or empty string, but preserve valid JSON strings
        String specsJson = request.getSpecificationsJson();
        if (specsJson != null && specsJson.trim().isEmpty()) {
            product.setSpecificationsJson(null);
        } else {
            product.setSpecificationsJson(specsJson);
        }

        // Ensure rating defaults
        if (product.getRating() == null) {
            product.setRating(BigDecimal.ZERO);
        }
        if (product.getReviewCount() == null) {
            product.setReviewCount(0);
        }
    }

    /**
     * Apply scheduled price changes if current time is within the scheduled period
     * Revert to original price after end date or before start date
     * Uses system default timezone for all date comparisons
     */
    private void applyScheduledPriceChange(Product product) {
        if (product.getScheduledPrice() != null && 
            product.getPriceStartDate() != null && 
            product.getPriceEndDate() != null) {
            
            // Get current time using system default timezone
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = product.getPriceStartDate();
            LocalDateTime endDate = product.getPriceEndDate();
            
            // Debug logging
            System.out.println("=== Price Scheduling Check for Product ID: " + product.getId() + " ===");
            System.out.println("Current System Time: " + now);
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            System.out.println("Scheduled Price: " + product.getScheduledPrice());
            System.out.println("Current Price: " + product.getPrice());
            System.out.println("Original Price Before Schedule: " + product.getOriginalPriceBeforeSchedule());
            
            // Store original price before schedule if not already stored
            if (product.getOriginalPriceBeforeSchedule() == null) {
                product.setOriginalPriceBeforeSchedule(product.getPrice());
                productRepository.save(product);
            }
            
            // Check if current time is within scheduled period
            if ((now.isAfter(startDate) || now.isEqual(startDate)) && 
                (now.isBefore(endDate) || now.isEqual(endDate))) {
                // Apply scheduled price if not already applied
                System.out.println("Status: WITHIN scheduled period - Applying scheduled price");
                if (!product.getPrice().equals(product.getScheduledPrice())) {
                    product.setPrice(product.getScheduledPrice());
                    productRepository.save(product);
                    System.out.println("Price updated to scheduled price: " + product.getScheduledPrice());
                } else {
                    System.out.println("Price already set to scheduled price");
                }
            } 
            // If before start date, use original price
            else if (now.isBefore(startDate)) {
                System.out.println("Status: BEFORE start date - Using original price");
                if (product.getOriginalPriceBeforeSchedule() != null && 
                    !product.getPrice().equals(product.getOriginalPriceBeforeSchedule())) {
                    product.setPrice(product.getOriginalPriceBeforeSchedule());
                    productRepository.save(product);
                    System.out.println("Price reverted to original: " + product.getOriginalPriceBeforeSchedule());
                } else {
                    System.out.println("Price already set to original");
                }
            }
            // If end date has passed, revert to original price and clear scheduling
            else if (now.isAfter(endDate)) {
                System.out.println("Status: AFTER end date - Clearing schedule and reverting price");
                if (product.getOriginalPriceBeforeSchedule() != null) {
                    product.setPrice(product.getOriginalPriceBeforeSchedule());
                    System.out.println("Price reverted to original: " + product.getOriginalPriceBeforeSchedule());
                }
                
                // Clear scheduled price fields
                product.setScheduledPrice(null);
                product.setPriceStartDate(null);
                product.setPriceEndDate(null);
                product.setOriginalPriceBeforeSchedule(null);
                productRepository.save(product);
                System.out.println("Schedule cleared");
            }
            System.out.println("=== End Price Scheduling Check ===\n");
        }
    }

    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getBrandName(),
                product.getBrandSlug(),
                product.getBrandLogoUrl(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getImageUrl(),
                product.getMainImageUrl(),
                product.getGalleryImages(),
                product.getIsActive(),
                product.getInStock(),
                product.getStockQuantity(),
                product.getIsOnSale(),
                product.getIsNew(),
                product.getRating(),
                product.getReviewCount(),
                product.getScheduledPrice(),
                product.getPriceStartDate(),
                product.getPriceEndDate(),
                product.getHighlights(),
                product.getSpecificationsJson(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

