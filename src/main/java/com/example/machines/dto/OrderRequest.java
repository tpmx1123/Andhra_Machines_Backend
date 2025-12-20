package com.example.machines.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private List<OrderItemRequest> items;
    private ShippingAddressRequest shippingAddress;
    private String paymentMethod;
    private String paymentId;

    @Data
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;
        private java.math.BigDecimal price; // Price from cart (what user saw)
        private java.math.BigDecimal originalPrice; // Original price from cart
        private List<AccessoryRequest> accessories;
    }

    @Data
    public static class AccessoryRequest {
        private String id;
        private Integer quantity;
    }

    @Data
    public static class ShippingAddressRequest {
        private String name;
        private String phone;
        private String email;
        private String address;
        private String city;
        private String state;
        private String pincode;
        private String landmark;
        private String deliveryInstructions;
    }
}

