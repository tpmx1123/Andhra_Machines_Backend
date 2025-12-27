package com.example.machines.service;

import com.example.machines.dto.PriceUpdateMessage;
import com.example.machines.dto.ProductRequest;
import com.example.machines.dto.ProductResponse;
import com.example.machines.entity.Product;
import com.example.machines.repository.ProductRepository;
import com.example.machines.repository.ReviewRepository;
import com.example.machines.repository.CartItemRepository;
import com.example.machines.repository.FavoriteRepository;
import com.example.machines.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    @Lazy
    private CartService cartService;

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
        // Set default values for new products
        if (request.getIsActive() == null) {
            product.setIsActive(true);
        }
        if (request.getInStock() == null) {
            product.setInStock(true);
        }
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

        // Check if product has been ordered (has OrderItems)
        // If it has orders, we need to handle the foreign key constraint
        boolean hasOrders = orderItemRepository.findAll().stream()
                .anyMatch(item -> item.getProduct() != null && item.getProduct().getId().equals(id));

        if (hasOrders) {
            // Product has been ordered - cannot hard delete due to foreign key constraints
            // Use soft delete instead to preserve order history
            product.setIsActive(false);
            product.setInStock(false);
            productRepository.save(product);
            throw new RuntimeException("Cannot delete product that has been ordered. Product has been deactivated instead.");
        }

        // Hard delete - remove all related entities first
        // 1. Delete all cart items for this product
        List<com.example.machines.entity.CartItem> cartItems = cartItemRepository.findByProductId(id);
        cartItemRepository.deleteAll(cartItems);

        // 2. Delete all favorites for this product
        favoriteRepository.deleteByProductId(id);

        // 3. Delete all reviews for this product
        reviewRepository.deleteByProduct(product);

        // 4. Delete all order items for this product (this will break order history)
        // Note: This is a hard delete, so order history will lose product references
        List<com.example.machines.entity.OrderItem> orderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(id))
                .collect(java.util.stream.Collectors.toList());
        orderItemRepository.deleteAll(orderItems);

        // 5. Now delete the product itself
        productRepository.delete(product);
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
        // Only update isActive if explicitly provided in request, otherwise preserve existing value
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }
        // Only update inStock if explicitly provided in request, otherwise preserve existing value
        if (request.getInStock() != null) {
            product.setInStock(request.getInStock());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }

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
            
            // Check if scheduled price is currently active and automatically set isOnSale
            ZoneId istZone = ZoneId.of("Asia/Kolkata");
            ZonedDateTime nowZoned = ZonedDateTime.now(istZone);
            LocalDateTime now = nowZoned.toLocalDateTime();
            LocalDateTime startDate = request.getPriceStartDate();
            LocalDateTime endDate = request.getPriceEndDate();
            
            if ((now.isAfter(startDate) || now.isEqual(startDate)) && 
                (now.isBefore(endDate) || now.isEqual(endDate))) {
                // Scheduled price is currently active, automatically set isOnSale to true
                product.setIsOnSale(true);
                System.out.println("Scheduled price is currently active - automatically set isOnSale to true");
            } else {
                // Scheduled price is not active yet or has expired, set isOnSale to false
                product.setIsOnSale(false);
                System.out.println("Scheduled price is not active - automatically set isOnSale to false");
            }
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
            // When schedule is cleared, don't automatically change isOnSale (let admin control it manually)
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
     * Public method for scheduled service to call
     */
    public void applyScheduledPriceChangeForSchedule(Product product) {
        applyScheduledPriceChange(product);
    }

    /**
     * Apply scheduled price changes if current time is within the scheduled period
     * Revert to original price after end date or before start date
     * Uses IST (Asia/Kolkata) timezone for all date comparisons
     */
    private void applyScheduledPriceChange(Product product) {
        if (product.getScheduledPrice() != null && 
            product.getPriceStartDate() != null && 
            product.getPriceEndDate() != null) {
            
            // Get current time in IST (Asia/Kolkata) timezone
            ZoneId istZone = ZoneId.of("Asia/Kolkata");
            ZonedDateTime nowZoned = ZonedDateTime.now(istZone);
            LocalDateTime now = nowZoned.toLocalDateTime();
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
                
                // Automatically set isOnSale to true when scheduled price is active
                boolean needsSave = false;
                if (product.getIsOnSale() == null || !product.getIsOnSale()) {
                    product.setIsOnSale(true);
                    needsSave = true;
                    System.out.println("Automatically set isOnSale to true (scheduled price is active)");
                }
                
                if (!product.getPrice().equals(product.getScheduledPrice())) {
                    product.setPrice(product.getScheduledPrice());
                    needsSave = true;
                    System.out.println("Price updated to scheduled price: " + product.getScheduledPrice());
                } else {
                    System.out.println("Price already set to scheduled price");
                }
                
                if (needsSave) {
                    productRepository.save(product);
                }
                
                // Send WebSocket notification
                PriceUpdateMessage priceUpdate = new PriceUpdateMessage(
                    product.getId(),
                    product.getScheduledPrice(),
                    product.getOriginalPriceBeforeSchedule(),
                    "Price updated: Scheduled discount applied",
                    "PRICE_CHANGED"
                );
                webSocketService.broadcastPriceUpdate(priceUpdate);
            } 
            // If before start date, use original price
            else if (now.isBefore(startDate)) {
                System.out.println("Status: BEFORE start date - Using original price");
                
                boolean needsSave = false;
                // Automatically set isOnSale to false when scheduled price hasn't started yet
                if (product.getIsOnSale() != null && product.getIsOnSale()) {
                    product.setIsOnSale(false);
                    needsSave = true;
                    System.out.println("Automatically set isOnSale to false (scheduled price not started yet)");
                }
                
                if (product.getOriginalPriceBeforeSchedule() != null && 
                    !product.getPrice().equals(product.getOriginalPriceBeforeSchedule())) {
                    product.setPrice(product.getOriginalPriceBeforeSchedule());
                    needsSave = true;
                    System.out.println("Price reverted to original: " + product.getOriginalPriceBeforeSchedule());
                } else {
                    System.out.println("Price already set to original");
                }
                
                if (needsSave) {
                    productRepository.save(product);
                }
                
                // Send WebSocket notification
                PriceUpdateMessage priceUpdate = new PriceUpdateMessage(
                    product.getId(),
                    product.getOriginalPriceBeforeSchedule(),
                    product.getOriginalPriceBeforeSchedule(),
                    "Price reverted: Schedule not started yet",
                    "PRICE_REVERTED"
                );
                webSocketService.broadcastPriceUpdate(priceUpdate);
            }
            // If end date has passed, revert to original price and clear scheduling
            else if (now.isAfter(endDate)) {
                System.out.println("Status: AFTER end date - Clearing schedule and reverting price");
                BigDecimal originalPrice = product.getOriginalPriceBeforeSchedule();
                if (originalPrice != null) {
                    product.setPrice(originalPrice);
                    System.out.println("Price reverted to original: " + originalPrice);
                }
                
                // Automatically set isOnSale to false when scheduled price expires
                if (product.getIsOnSale() != null && product.getIsOnSale()) {
                    product.setIsOnSale(false);
                    System.out.println("Automatically set isOnSale to false (scheduled price expired)");
                }
                
                // Clear scheduled price fields
                product.setScheduledPrice(null);
                product.setPriceStartDate(null);
                product.setPriceEndDate(null);
                product.setOriginalPriceBeforeSchedule(null);
                productRepository.save(product);
                System.out.println("Schedule cleared");
                
                // Send WebSocket notification (always send to sync cart)
                PriceUpdateMessage priceUpdate = new PriceUpdateMessage(
                    product.getId(),
                    originalPrice != null ? originalPrice : product.getPrice(),
                    originalPrice != null ? originalPrice : product.getPrice(),
                    "Price reverted: Schedule ended",
                    "SCHEDULE_ENDED"
                );
                webSocketService.broadcastPriceUpdate(priceUpdate);
                
                // Also sync cart prices in database for all users who have this product in cart
                try {
                    cartService.syncCartPricesForProduct(product.getId());
                } catch (Exception e) {
                    System.err.println("Error syncing cart prices for product " + product.getId() + ": " + e.getMessage());
                }
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

