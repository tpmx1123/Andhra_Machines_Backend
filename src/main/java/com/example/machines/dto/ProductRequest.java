package com.example.machines.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    // Brand
    private String brandName;
    private String brandSlug;
    private String brandLogoUrl;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    // Images
    private String imageUrl;      // legacy
    private String mainImageUrl;
    private List<String> galleryImages;

    // Status / stock
    private Boolean isActive = true;
    private Boolean inStock = true;
    private Integer stockQuantity = 0;

    // Badges
    private Boolean isOnSale = false;
    private Boolean isNew = false;

    // Price scheduling
    private BigDecimal scheduledPrice;
    private LocalDateTime priceStartDate;
    private LocalDateTime priceEndDate;

    // Highlights and specifications
    private List<String> highlights;
    private String specificationsJson;
}

