package com.example.machines.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String name;
    private String brand;
    private String brandSlug;
    private String image;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Boolean inStock;
}

