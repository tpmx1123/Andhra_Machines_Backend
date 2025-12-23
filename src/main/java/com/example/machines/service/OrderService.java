package com.example.machines.service;

import com.example.machines.dto.OrderRequest;
import com.example.machines.dto.OrderResponse;
import com.example.machines.dto.OrderStatusUpdateMessage;
import com.example.machines.entity.*;
import com.example.machines.repository.OrderRepository;
import com.example.machines.repository.ProductRepository;
import com.example.machines.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WebSocketService webSocketService;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus("pending");
        order.setWhatsappSent(false);

        // Set shipping address
        order.setShippingName(request.getShippingAddress().getName());
        order.setShippingPhone(request.getShippingAddress().getPhone());
        order.setShippingEmail(request.getShippingAddress().getEmail());
        order.setShippingAddress(request.getShippingAddress().getAddress());
        order.setShippingCity(request.getShippingAddress().getCity());
        order.setShippingState(request.getShippingAddress().getState());
        order.setShippingPincode(request.getShippingAddress().getPincode());
        order.setShippingLandmark(request.getShippingAddress().getLandmark());
        order.setShippingInstructions(request.getShippingAddress().getDeliveryInstructions());

        // Initialize items list
        order.setItems(new java.util.ArrayList<>());
        
        // Calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        // Process all items from the request
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

            // Use price from cart (what user saw) if provided, otherwise use database price
            BigDecimal unitPrice = itemRequest.getPrice() != null ? itemRequest.getPrice() : product.getPrice();
            BigDecimal originalPrice = itemRequest.getOriginalPrice() != null ? itemRequest.getOriginalPrice() : product.getOriginalPrice();
            
            // If originalPrice is null, use unitPrice as originalPrice
            if (originalPrice == null) {
                originalPrice = unitPrice;
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getTitle());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setOriginalPrice(originalPrice);

            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            orderItem.setTotalPrice(itemTotal);

            // Calculate subtotal using original prices (before discount)
            BigDecimal itemSubtotal = originalPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            // Calculate discount (difference between original and current price)
            if (originalPrice.compareTo(unitPrice) > 0) {
                BigDecimal itemDiscount = originalPrice
                        .subtract(unitPrice)
                        .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                discount = discount.add(itemDiscount);
            }
            
            // Add item to order's items list (cascade will save it)
            order.getItems().add(orderItem);
        }

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setDeliveryCharge(BigDecimal.ZERO); // Free delivery
        // Total is subtotal minus discount
        order.setTotal(subtotal.subtract(discount).add(order.getDeliveryCharge()));
        
        // Payment status remains "pending" until order is confirmed

        // Save order (cascade will save all items)
        order = orderRepository.save(order);
        
        // Flush to ensure all items are persisted before returning
        orderRepository.flush();

        // Send order confirmation email
        try {
            emailService.sendOrderConfirmation(user.getEmail(), order.getOrderNumber(), order.getTotal().toString());
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }

        // Fetch the order again to ensure all items are loaded (with EAGER fetch)
        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow(() -> new RuntimeException("Order not found after save"));
        
        return OrderResponse.fromEntity(savedOrder);
    }

    public List<OrderResponse> getUserOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Check if user owns this order or is admin
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order");
        }
        
        return OrderResponse.fromEntity(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(newStatus);
            
            // Update payment status to "paid" when order status changes to CONFIRMED
            if (newStatus == Order.OrderStatus.CONFIRMED && "pending".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("paid");
            }
            
            order = orderRepository.save(order);

            // Send order status update email
            try {
                emailService.sendOrderStatusUpdate(order.getUser().getEmail(), order.getOrderNumber(), status);
            } catch (Exception e) {
                System.err.println("Failed to send order status update email: " + e.getMessage());
            }

            // Send WebSocket notification to order owner
            OrderStatusUpdateMessage statusUpdate = new OrderStatusUpdateMessage(
                order.getId(),
                order.getOrderNumber(),
                status.toUpperCase(),
                "Order status updated to: " + status,
                order.getUser().getId()
            );
            webSocketService.sendOrderStatusUpdate(order.getUser().getId(), statusUpdate);
            
            // Also broadcast to admin panel
            webSocketService.broadcastOrderStatusUpdate(statusUpdate);

            return OrderResponse.fromEntity(order);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }

    @Transactional
    public OrderResponse markWhatsAppSent(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setWhatsappSent(true);
        order = orderRepository.save(order);
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        orderRepository.delete(order);
    }
}

