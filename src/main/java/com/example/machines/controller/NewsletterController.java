package com.example.machines.controller;

import com.example.machines.entity.NewsletterSubscriber;
import com.example.machines.service.NewsletterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
@CrossOrigin(origins = "*")
public class NewsletterController {

    @Autowired
    private NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Email is required");
            return ResponseEntity.badRequest().body(error);
        }

        newsletterService.subscribe(email);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Subscribed successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NewsletterSubscriber>> getAllSubscribers() {
        return ResponseEntity.ok(newsletterService.getAllSubscribers());
    }

    @DeleteMapping("/subscribers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteSubscriber(@PathVariable Long id) {
        newsletterService.deleteSubscriber(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Subscriber removed successfully");
        return ResponseEntity.ok(response);
    }
}

