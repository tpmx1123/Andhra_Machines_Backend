package com.example.machines.service;

import com.example.machines.entity.Product;
import com.example.machines.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleFeedService {

    @Autowired
    private ProductRepository productRepository;

    private static final String BASE_URL = "https://andhramachinesagencies.com";
    private static final DateTimeFormatter RFC_822_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");

    public String generateGoogleProductFeed() {
        List<Product> products = productRepository.findByIsActiveTrue();
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:g=\"http://base.google.com/ns/1.0\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>Andhra Machines Agencies - Sewing Machines</title>\n");
        xml.append("    <link>").append(BASE_URL).append("</link>\n");
        xml.append("    <description>Premium sewing machines and accessories from Andhra Machines Agencies</description>\n");
        xml.append("    <lastBuildDate>").append(LocalDateTime.now().format(RFC_822_FORMATTER)).append("</lastBuildDate>\n");
        
        for (Product product : products) {
            if (product.getInStock() != null && !product.getInStock()) {
                continue; // Skip out of stock products
            }
            
            xml.append("    <item>\n");
            
            // Required fields
            xml.append("      <g:id>").append(escapeXml(product.getId().toString())).append("</g:id>\n");
            xml.append("      <title>").append(escapeXml(product.getTitle())).append("</title>\n");
            xml.append("      <description>").append(escapeXml(getProductDescription(product))).append("</description>\n");
            xml.append("      <link>").append(escapeXml(getProductUrl(product))).append("</link>\n");
            xml.append("      <g:image_link>").append(escapeXml(getProductImage(product))).append("</g:image_link>\n");
            xml.append("      <g:availability>").append(getAvailability(product)).append("</g:availability>\n");
            xml.append("      <g:price>").append(formatPrice(product)).append("</g:price>\n");
            xml.append("      <g:condition>new</g:condition>\n");
            
            // Brand (if available)
            if (product.getBrandName() != null && !product.getBrandName().isEmpty()) {
                xml.append("      <g:brand>").append(escapeXml(product.getBrandName())).append("</g:brand>\n");
            }
            
            // Product type/category
            xml.append("      <g:product_type>Sewing Machine</g:product_type>\n");
            
            // Additional fields for better visibility
            if (product.getIsOnSale() != null && product.getIsOnSale()) {
                xml.append("      <g:sale_price>").append(formatPrice(product)).append("</g:sale_price>\n");
                if (product.getOriginalPrice() != null) {
                    xml.append("      <g:price>").append(formatPrice(product.getOriginalPrice())).append("</g:price>\n");
                }
            }
            
            xml.append("    </item>\n");
        }
        
        xml.append("  </channel>\n");
        xml.append("</rss>");
        
        return xml.toString();
    }

    public List<Map<String, Object>> generateGoogleProductFeedJson() {
        List<Product> products = productRepository.findByIsActiveTrue();
        List<Map<String, Object>> feed = new ArrayList<>();
        
        for (Product product : products) {
            if (product.getInStock() != null && !product.getInStock()) {
                continue;
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId().toString());
            item.put("title", product.getTitle());
            item.put("description", getProductDescription(product));
            item.put("link", getProductUrl(product));
            item.put("image_link", getProductImage(product));
            item.put("availability", getAvailability(product));
            item.put("price", formatPrice(product));
            item.put("condition", "new");
            
            if (product.getBrandName() != null && !product.getBrandName().isEmpty()) {
                item.put("brand", product.getBrandName());
            }
            
            item.put("product_type", "Sewing Machine");
            
            if (product.getIsOnSale() != null && product.getIsOnSale() && product.getOriginalPrice() != null) {
                item.put("sale_price", formatPrice(product));
                item.put("price", formatPrice(product.getOriginalPrice()));
            }
            
            feed.add(item);
        }
        
        return feed;
    }

    private String getProductUrl(Product product) {
        if (product.getBrandSlug() != null && !product.getBrandSlug().isEmpty()) {
            try {
                String encodedSlug = URLEncoder.encode(product.getBrandSlug(), StandardCharsets.UTF_8.toString());
                return BASE_URL + "/products/" + encodedSlug;
            } catch (Exception e) {
                return BASE_URL + "/products/" + product.getId();
            }
        }
        return BASE_URL + "/products/" + product.getId();
    }

    private String getProductImage(Product product) {
        if (product.getMainImageUrl() != null && !product.getMainImageUrl().isEmpty()) {
            return product.getMainImageUrl();
        }
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            return product.getImageUrl();
        }
        return BASE_URL + "/images/placeholder.jpg"; // Fallback
    }

    private String getProductDescription(Product product) {
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            // Limit description to 5000 characters (Google's limit)
            String desc = product.getDescription();
            if (desc.length() > 5000) {
                desc = desc.substring(0, 4997) + "...";
            }
            return desc;
        }
        return product.getTitle() + " - Premium sewing machine from Andhra Machines Agencies";
    }

    private String getAvailability(Product product) {
        if (product.getInStock() != null && product.getInStock() && 
            (product.getStockQuantity() == null || product.getStockQuantity() > 0)) {
            return "in stock";
        }
        return "out of stock";
    }

    private String formatPrice(Product product) {
        BigDecimal price = product.getPrice();
        
        // Apply scheduled price if active
        if (product.getScheduledPrice() != null && 
            product.getPriceStartDate() != null && 
            product.getPriceEndDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(product.getPriceStartDate()) && now.isBefore(product.getPriceEndDate())) {
                price = product.getScheduledPrice();
            }
        }
        
        return String.format("%.2f INR", price);
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%.2f INR", price);
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

