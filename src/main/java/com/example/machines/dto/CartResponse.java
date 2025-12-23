package com.example.machines.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private java.math.BigDecimal totalPrice;
}

