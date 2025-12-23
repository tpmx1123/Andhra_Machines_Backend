package com.example.machines.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {
    private Long productId;
    private String name;
    private String brand;
    private String brandSlug;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String image;
    private boolean inStock;
}

