package com.example.machines.service;

import com.example.machines.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ContactService {

    @Autowired
    private EmailService emailService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.phone}")
    private String adminPhone;

    // Comprehensive email validation pattern
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    // Indian phone number validation pattern (supports various formats)
    // Matches: 10 digits, or +91 followed by 10 digits, or 0 followed by 10 digits
    private static final String PHONE_PATTERN = 
        "^(\\+91|91|0)?[6-9]\\d{9}$";

    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
    private static final Pattern phonePattern = Pattern.compile(PHONE_PATTERN);

    public void submitContactForm(ContactRequest request) {
        // Validate email with comprehensive pattern
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format. Please provide a valid email address.");
        }

        // Validate phone number
        String cleanedPhone = request.getPhone().replaceAll("[\\s-]", "");
        if (!isValidPhone(cleanedPhone)) {
            throw new IllegalArgumentException("Invalid phone number. Please provide a valid 10-digit Indian phone number.");
        }

        // Validate message word count (max 500 words)
        int wordCount = countWords(request.getMessage());
        if (wordCount > 500) {
            throw new IllegalArgumentException("Message must not exceed 500 words. Current word count: " + wordCount);
        }

        // Send email to admin
        emailService.sendContactFormEmail(
            adminEmail,
            request.getName(),
            request.getEmail(),
            cleanedPhone,
            request.getMessage()
        );
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        // Split by whitespace and filter out empty strings
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Check basic structure
        if (!emailPattern.matcher(email).matches()) {
            return false;
        }

        // Additional checks
        // Must not start or end with dot
        if (email.startsWith(".") || email.endsWith(".")) {
            return false;
        }

        // Must not have consecutive dots
        if (email.contains("..")) {
            return false;
        }

        // Must have @ symbol
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return false;
        }

        // Domain must have at least one dot
        String domain = email.substring(atIndex + 1);
        if (!domain.contains(".")) {
            return false;
        }

        // Domain extension must be 2-7 characters
        String[] domainParts = domain.split("\\.");
        if (domainParts.length < 2) {
            return false;
        }
        String extension = domainParts[domainParts.length - 1];
        if (extension.length() < 2 || extension.length() > 7) {
            return false;
        }

        return true;
    }

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Remove all non-digit characters except +
        String cleaned = phone.replaceAll("[^+\\d]", "");
        
        // Check pattern
        if (!phonePattern.matcher(cleaned).matches()) {
            return false;
        }

        // Extract just the digits
        String digitsOnly = cleaned.replaceAll("[^\\d]", "");
        
        // Must be exactly 10 digits (after removing country code if present)
        if (digitsOnly.length() < 10 || digitsOnly.length() > 13) {
            return false;
        }

        // If it starts with +91 or 91, remove it and check remaining digits
        if (cleaned.startsWith("+91") || cleaned.startsWith("91")) {
            String remaining = digitsOnly.substring(cleaned.startsWith("+91") ? 2 : 2);
            if (remaining.length() != 10) {
                return false;
            }
            // First digit after country code must be 6-9
            return remaining.charAt(0) >= '6' && remaining.charAt(0) <= '9';
        }

        // If it starts with 0, remove it
        if (digitsOnly.startsWith("0") && digitsOnly.length() == 11) {
            digitsOnly = digitsOnly.substring(1);
        }

        // Must be exactly 10 digits and start with 6-9
        if (digitsOnly.length() != 10) {
            return false;
        }

        return digitsOnly.charAt(0) >= '6' && digitsOnly.charAt(0) <= '9';
    }
}
