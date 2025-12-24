package com.example.machines.dto;

import com.example.machines.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal deliveryCharge;
    private BigDecimal total;
    private String status;
    private String paymentStatus;
    private ShippingAddressResponse shippingAddress;
    private Boolean whatsappSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setItems(order.getItems().stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList()));
        response.setSubtotal(order.getSubtotal());
        response.setDiscount(order.getDiscount());
        response.setDeliveryCharge(order.getDeliveryCharge());
        response.setTotal(order.getTotal());
        response.setStatus(order.getStatus().name());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setWhatsappSent(order.getWhatsappSent());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        ShippingAddressResponse address = new ShippingAddressResponse();
        address.setName(order.getShippingName());
        address.setPhone(order.getShippingPhone());
        address.setEmail(order.getShippingEmail());
        address.setAddress(order.getShippingAddress());
        address.setCity(order.getShippingCity());
        address.setState(order.getShippingState());
        address.setPincode(order.getShippingPincode());
        address.setLandmark(order.getShippingLandmark());
        address.setDeliveryInstructions(order.getShippingInstructions());
        response.setShippingAddress(address);

        return response;
    }
}

@Data
class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String brandSlug;
    private String productImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal originalPrice;
    private BigDecimal totalPrice;

    public static OrderItemResponse fromEntity(com.example.machines.entity.OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProductName());
        response.setBrandSlug(item.getProduct().getBrandSlug());
        response.setProductImage(item.getProduct().getMainImageUrl() != null ? item.getProduct().getMainImageUrl() : item.getProduct().getImageUrl());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setOriginalPrice(item.getOriginalPrice());
        response.setTotalPrice(item.getTotalPrice());
        return response;
    }
}

@Data
class ShippingAddressResponse {
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

