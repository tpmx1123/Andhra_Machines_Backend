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

            helper.setFrom(fromEmail, "Andhra Machines Agencies");
            helper.setTo(toEmail);
            helper.setSubject("New Blog Post: " + blogTitle);
            
            // Use production URL
            String blogUrl = "https://andhramachinesagencies.com/blog/" + blogSlug;
            
            // Professional HTML email template
            String content = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "</head>" +
                    "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f5f5f5;'>" +
                    "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f5f5f5; padding: 20px;'>" +
                    "<tr>" +
                    "<td align='center'>" +
                    "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                    // Header
                    "<tr>" +
                    "<td style='background-color: #c54513; padding: 30px 20px; text-align: center;'>" +
                    "<h1 style='color: #ffffff; margin: 0; font-size: 24px; font-weight: bold;'>New Blog Post Alert!</h1>" +
                    "</td>" +
                    "</tr>" +
                    // Content
                    "<tr>" +
                    "<td style='padding: 30px 20px;'>" +
                    "<p style='color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;'>Hi there,</p>" +
                    "<p style='color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;'>We just published a new blog post that we think you'll find interesting:</p>" +
                    "<div style='background-color: #f9f9f9; border-left: 4px solid #c54513; padding: 15px; margin: 20px 0;'>" +
                    "<h2 style='color: #c54513; margin: 0 0 10px 0; font-size: 20px; font-weight: bold;'>" + escapeHtml(blogTitle) + "</h2>" +
                    "</div>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + blogUrl + "' style='display: inline-block; background-color: #c54513; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 5px; font-weight: bold; font-size: 16px;'>Read Full Article</a>" +
                    "</div>" +
                    "<p style='color: #666666; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;'>Or copy and paste this link into your browser:</p>" +
                    "<p style='color: #c54513; font-size: 14px; word-break: break-all; margin: 5px 0;'>" + blogUrl + "</p>" +
                    "</td>" +
                    "</tr>" +
                    // Footer
                    "<tr>" +
                    "<td style='background-color: #f9f9f9; padding: 20px; text-align: center; border-top: 1px solid #e0e0e0;'>" +
                    "<p style='color: #666666; font-size: 14px; margin: 0 0 10px 0;'>Best regards,</p>" +
                    "<p style='color: #c54513; font-size: 16px; font-weight: bold; margin: 0;'>Andhra Machines Agencies</p>" +
                    "<p style='color: #999999; font-size: 12px; margin: 15px 0 0 0;'>Stitching Trust Since 1982</p>" +
                    "</td>" +
                    "</tr>" +
                    "</table>" +
                    "</td>" +
                    "</tr>" +
                    "</table>" +
                    "</body>" +
                    "</html>";
            
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

