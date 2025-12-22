package com.example.machines.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateMessage {
    private Long orderId;
    private String orderNumber;
    private String newStatus;
    private String message;
    private Long userId; // To send only to the order owner
}

