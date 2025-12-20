package com.example.machines.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Async
    public void sendNewBlogNotification(String toEmail, String blogTitle, String blogSlug) {
        if (mailSender == null) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("New Blog Post: " + blogTitle);
            
            String blogUrl = "http://localhost:5173/blog/" + blogSlug; // Update with production URL in future
            String content = "<h1>New Blog Post Alert!</h1>" +
                             "<p>Hi there,</p>" +
                             "<p>We just published a new blog post: <strong>" + blogTitle + "</strong></p>" +
                             "<p>You can read it here: <a href='" + blogUrl + "'>" + blogUrl + "</a></p>" +
                             "<br><p>Best regards,<br>Andhra Sewing Machines</p>";
            
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmation(String toEmail, String orderId, String totalAmount) {
        if (mailSender == null) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Order Confirmation - Order #" + orderId);
            
            String content = "<h1>Thank you for your order!</h1>" +
                             "<p>Hi,</p>" +
                             "<p>We've received your order <strong>#" + orderId + "</strong>.</p>" +
                             "<p>Total Amount: <strong>₹" + totalAmount + "</strong></p>" +
                             "<p>We will notify you when your order is shipped.</p>" +
                             "<br><p>Best regards,<br>Andhra Sewing Machines</p>";
            
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending order confirmation to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdate(String toEmail, String orderId, String status) {
        if (mailSender == null) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Order Status Update - Order #" + orderId);
            
            String content = "<h1>Order Status Updated</h1>" +
                             "<p>Hi,</p>" +
                             "<p>The status of your order <strong>#" + orderId + "</strong> has been updated to: <strong>" + status + "</strong>.</p>" +
                             "<p>Log in to your account to see more details.</p>" +
                             "<br><p>Best regards,<br>Andhra Sewing Machines</p>";
            
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending order status update to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        if (mailSender == null) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Andhra Sewing Machines!");
            
            String content = "<h1>Welcome, " + userName + "!</h1>" +
                             "<p>Thank you for creating an account with Andhra Sewing Machines.</p>" +
                             "<p>We are glad to have you with us.</p>" +
                             "<br><p>Best regards,<br>Andhra Sewing Machines</p>";
            
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending welcome email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendContactFormEmail(String toEmail, String name, String userEmail, String phone, String message) {
        if (mailSender == null) return;

        try {
            MimeMessage emailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(emailMessage, true);

            helper.setFrom(fromEmail, name);
            helper.setTo(toEmail);
            helper.setSubject("New Contact Form Submission - Andhra Machines");
            
            String content = "<h2>New Contact Form Submission</h2>" +
                             "<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>" +
                             "<tr><td style='padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9; font-weight: bold;'>Name:</td>" +
                             "<td style='padding: 8px; border: 1px solid #ddd;'>" + escapeHtml(name) + "</td></tr>" +
                             "<tr><td style='padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9; font-weight: bold;'>Email:</td>" +
                             "<td style='padding: 8px; border: 1px solid #ddd;'><a href='mailto:" + escapeHtml(userEmail) + "'>" + escapeHtml(userEmail) + "</a></td></tr>" +
                             "<tr><td style='padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9; font-weight: bold;'>Phone:</td>" +
                             "<td style='padding: 8px; border: 1px solid #ddd;'><a href='tel:" + escapeHtml(phone) + "'>" + escapeHtml(phone) + "</a></td></tr>" +
                             "<tr><td style='padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9; font-weight: bold; vertical-align: top;'>Message:</td>" +
                             "<td style='padding: 8px; border: 1px solid #ddd;'>" + escapeHtml(message).replace("\n", "<br>") + "</td></tr>" +
                             "</table>" +
                             "<br><p style='color: #666; font-size: 12px;'>This email was sent from the contact form on your website.</p>";
            
            helper.setText(content, true);
            mailSender.send(emailMessage);
        } catch (Exception e) {
            System.err.println("Error sending contact form email to " + toEmail + ": " + e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

