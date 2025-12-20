package com.example.machines.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String title;
    private String description;
    private String brandName;
    private String brandSlug;
    private String brandLogoUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageUrl;
    private String mainImageUrl;
    private List<String> galleryImages;
    private Boolean isActive;
    private Boolean inStock;
    private Integer stockQuantity;
    private Boolean isOnSale;
    private Boolean isNew;
    private BigDecimal rating;
    private Integer reviewCount;
    private BigDecimal scheduledPrice;
    private LocalDateTime priceStartDate;
    private LocalDateTime priceEndDate;
    private List<String> highlights;
    private String specificationsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

