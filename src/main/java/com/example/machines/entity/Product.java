package com.example.machines.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Brand basics
    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "brand_slug")
    private String brandSlug;

    @Column(name = "brand_logo_url")
    private String brandLogoUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    // Main product image
    @Column(name = "image_url")
    private String imageUrl;

    // Main hero image (can be same as imageUrl)
    @Column(name = "main_image_url")
    private String mainImageUrl;

    // Additional gallery images for thumbnails
    @ElementCollection
    @CollectionTable(name = "product_gallery_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> galleryImages;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "in_stock")
    private Boolean inStock = true;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    // Badges
    @Column(name = "is_on_sale")
    private Boolean isOnSale = false;

    @Column(name = "is_new")
    private Boolean isNew = false;

    // Rating summary (calculated from reviews)
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "scheduled_price", precision = 10, scale = 2)
    private BigDecimal scheduledPrice;

    @Column(name = "price_start_date")
    private LocalDateTime priceStartDate;

    @Column(name = "price_end_date")
    private LocalDateTime priceEndDate;

    @Column(name = "original_price_before_schedule", precision = 10, scale = 2)
    private BigDecimal originalPriceBeforeSchedule; // Store original price when scheduling starts

    // Highlights shown as bullet points
    @ElementCollection
    @CollectionTable(name = "product_highlights", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "highlight")
    private List<String> highlights;

    // Specifications stored as JSON so frontend can map to UI
    @Lob
    @Column(name = "specifications_json")
    private String specificationsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

