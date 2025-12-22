package com.example.machines.service;

import com.example.machines.dto.OrderStatusUpdateMessage;
import com.example.machines.dto.PriceUpdateMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast price update to all connected clients
     */
    public void broadcastPriceUpdate(PriceUpdateMessage message) {
        messagingTemplate.convertAndSend("/topic/price-updates", message);
    }

    /**
     * Send price update to specific user (for cart updates)
     */
    public void sendPriceUpdateToUser(String userId, PriceUpdateMessage message) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/price-updates", message);
    }

    /**
     * Send order status update to specific user
     */
    public void sendOrderStatusUpdate(Long userId, OrderStatusUpdateMessage message) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/order-updates", message);
    }

    /**
     * Broadcast order status update (for admin panel)
     */
    public void broadcastOrderStatusUpdate(OrderStatusUpdateMessage message) {
        messagingTemplate.convertAndSend("/topic/order-updates", message);
    }
}

