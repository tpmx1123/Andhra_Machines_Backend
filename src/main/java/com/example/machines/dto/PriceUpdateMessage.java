package com.example.machines.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateMessage {
    private Long productId;
    private BigDecimal newPrice;
    private BigDecimal originalPrice;
    private String message;
    private String type; // "PRICE_CHANGED", "PRICE_REVERTED", "SCHEDULE_STARTED", "SCHEDULE_ENDED"
}

