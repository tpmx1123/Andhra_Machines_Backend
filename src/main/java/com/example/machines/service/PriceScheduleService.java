package com.example.machines.service;

import com.example.machines.entity.Product;
import com.example.machines.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PriceScheduleService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductService productService;
    /**
     * Check for scheduled price changes every minute
     * This ensures prices are updated in real-time when schedules trigger
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    public void checkScheduledPriceChanges() {
        // Get all products with active schedules
        List<Product> productsWithSchedules = productRepository.findByScheduledPriceIsNotNullAndPriceStartDateIsNotNullAndPriceEndDateIsNotNull();
        
        if (productsWithSchedules.isEmpty()) {
            return;
        }

        // Apply price changes for all products with schedules
        // The applyScheduledPriceChangeForSchedule method will check timing and send WebSocket notifications
        for (Product product : productsWithSchedules) {
            try {
                productService.applyScheduledPriceChangeForSchedule(product);
            } catch (Exception e) {
                System.err.println("Error checking scheduled price for product " + product.getId() + ": " + e.getMessage());
            }
        }
    }
}

