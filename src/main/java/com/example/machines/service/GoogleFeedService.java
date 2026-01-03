package com.example.machines.service;

import com.example.machines.entity.Product;
import com.example.machines.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        try {
            List<Product> products = productRepository.findByIsActiveTrue();
            
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<rss version=\"2.0\" xmlns:g=\"http://base.google.com/ns/1.0\">\n");
            xml.append("  <channel>\n");
            xml.append("    <title>Andhra Machines Agencies - Sewing Machines</title>\n");
            xml.append("    <link>").append(BASE_URL).append("</link>\n");
            xml.append("    <description>Premium sewing machines and accessories from Andhra Machines Agencies</description>\n");
            
            // Format date with timezone
            try {
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                xml.append("    <lastBuildDate>").append(now.format(RFC_822_FORMATTER)).append("</lastBuildDate>\n");
            } catch (Exception e) {
                // Fallback to simple date format if RFC 822 fails
                xml.append("    <lastBuildDate>").append(LocalDateTime.now().toString()).append("</lastBuildDate>\n");
            }
            
            for (Product product : products) {
                try {
                    if (product == null) {
                        continue; // Skip null products
                    }
                    // Don't skip out of stock products - Google accepts them, but we'll mark availability correctly
                    
                    // Skip if required fields are missing
                    if (product.getId() == null || product.getTitle() == null || product.getTitle().isEmpty()) {
                        continue;
                    }
                    
                    xml.append("    <item>\n");
                    
                    // Required fields
                    xml.append("      <g:id>").append(escapeXml(String.valueOf(product.getId()))).append("</g:id>\n");
                    xml.append("      <title>").append(escapeXml(product.getTitle())).append("</title>\n");
                    xml.append("      <description>").append(escapeXml(getProductDescription(product))).append("</description>\n");
                    xml.append("      <link>").append(escapeXml(getProductUrl(product))).append("</link>\n");
                    xml.append("      <g:image_link>").append(escapeXml(getProductImage(product))).append("</g:image_link>\n");
                    xml.append("      <g:availability>").append(getAvailability(product)).append("</g:availability>\n");
                    
                    // Price handling: Only ONE <g:price> tag per item
                    // If on sale: <g:price> = original price, <g:sale_price> = discounted price
                    // If not on sale: <g:price> = current price
                    BigDecimal currentPrice = getCurrentPrice(product);
                    BigDecimal originalPrice = getOriginalPriceForFeed(product);
                    
                    if (product.getIsOnSale() != null && product.getIsOnSale() && 
                        originalPrice != null && originalPrice.compareTo(currentPrice) > 0) {
                        // Product is on sale: show original price as <g:price> and current as <g:sale_price>
                        xml.append("      <g:price>").append(escapeXml(formatPrice(originalPrice))).append("</g:price>\n");
                        xml.append("      <g:sale_price>").append(escapeXml(formatPrice(currentPrice))).append("</g:sale_price>\n");
                    } else {
                        // Product not on sale: show current price as <g:price>
                        xml.append("      <g:price>").append(escapeXml(formatPrice(currentPrice))).append("</g:price>\n");
                    }
                    
                    xml.append("      <g:condition>new</g:condition>\n");
                    
                    // Brand (if available)
                    if (product.getBrandName() != null && !product.getBrandName().isEmpty()) {
                        xml.append("      <g:brand>").append(escapeXml(product.getBrandName())).append("</g:brand>\n");
                    }
                    
                    // Product type/category
                    xml.append("      <g:product_type>Sewing Machine</g:product_type>\n");
                    
                    // Identifier exists (to avoid GTIN errors)
                    xml.append("      <g:identifier_exists>false</g:identifier_exists>\n");
                    
                    // Shipping information (required for India)
                    xml.append("      <g:shipping>\n");
                    xml.append("        <g:country>IN</g:country>\n");
                    xml.append("        <g:service>Standard</g:service>\n");
                    xml.append("        <g:price>0 INR</g:price>\n");
                    xml.append("      </g:shipping>\n");
                    
                    xml.append("    </item>\n");
                } catch (Exception e) {
                    // Log error but continue with other products
                    System.err.println("Error processing product " + (product != null ? product.getId() : "null") + ": " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }
            }
            
            xml.append("  </channel>\n");
            xml.append("</rss>");
            
            return xml.toString();
        } catch (Exception e) {
            System.err.println("Error generating Google product feed: " + e.getMessage());
            e.printStackTrace();
            // Return minimal valid XML on error
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rss version=\"2.0\" xmlns:g=\"http://base.google.com/ns/1.0\">\n  <channel>\n    <title>Andhra Machines Agencies</title>\n    <link>" + BASE_URL + "</link>\n    <description>Product feed temporarily unavailable</description>\n  </channel>\n</rss>";
        }
    }

    public List<Map<String, Object>> generateGoogleProductFeedJson() {
        List<Product> products = productRepository.findByIsActiveTrue();
        List<Map<String, Object>> feed = new ArrayList<>();
        
        for (Product product : products) {
            if (product == null) {
                continue;
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId().toString());
            item.put("title", product.getTitle());
            item.put("description", getProductDescription(product));
            item.put("link", getProductUrl(product));
            item.put("image_link", getProductImage(product));
            item.put("availability", getAvailability(product));
            
            // Price handling: Only ONE price field per item
            // If on sale: price = original price, sale_price = discounted price
            // If not on sale: price = current price
            BigDecimal currentPrice = getCurrentPrice(product);
            BigDecimal originalPrice = getOriginalPriceForFeed(product);
            
            if (product.getIsOnSale() != null && product.getIsOnSale() && 
                originalPrice != null && originalPrice.compareTo(currentPrice) > 0) {
                // Product is on sale: show original price as price and current as sale_price
                item.put("price", formatPrice(originalPrice));
                item.put("sale_price", formatPrice(currentPrice));
            } else {
                // Product not on sale: show current price as price
                item.put("price", formatPrice(currentPrice));
            }
            
            item.put("condition", "new");
            
            if (product.getBrandName() != null && !product.getBrandName().isEmpty()) {
                item.put("brand", product.getBrandName());
            }
            
            item.put("product_type", "Sewing Machine");
            item.put("identifier_exists", false);
            
            // Shipping information (required for India)
            Map<String, Object> shipping = new HashMap<>();
            shipping.put("country", "IN");
            shipping.put("service", "Standard");
            shipping.put("price", "0 INR");
            item.put("shipping", shipping);
            
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
        if (product == null) {
            return "Premium sewing machine from Andhra Machines Agencies";
        }
        
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            // Limit description to 5000 characters (Google's limit)
            String desc = product.getDescription();
            if (desc.length() > 5000) {
                desc = desc.substring(0, 4997) + "...";
            }
            return desc;
        }
        
        String title = product.getTitle() != null ? product.getTitle() : "Sewing Machine";
        return title + " - Premium sewing machine from Andhra Machines Agencies";
    }

    private String getAvailability(Product product) {
        // More lenient availability check: if inStock is true OR stockQuantity > 0, mark as in stock
        // This ensures at least some products show as "in stock" for Google indexing
        if (product.getInStock() != null && product.getInStock()) {
            return "in stock";
        }
        if (product.getStockQuantity() != null && product.getStockQuantity() > 0) {
            return "in stock";
        }
        // Default to in stock if both are null (to ensure some products are available)
        if (product.getInStock() == null && product.getStockQuantity() == null) {
            return "in stock";
        }
        return "out of stock";
    }


    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0.00 INR";
        }
        return String.format("%.2f INR", price);
    }

    /**
     * Get the current price (considering scheduled prices)
     */
    private BigDecimal getCurrentPrice(Product product) {
        if (product == null || product.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal price = product.getPrice();
        
        // Apply scheduled price if active
        try {
            if (product.getScheduledPrice() != null && 
                product.getPriceStartDate() != null && 
                product.getPriceEndDate() != null) {
                ZoneId istZone = ZoneId.of("Asia/Kolkata");
                ZonedDateTime nowZoned = ZonedDateTime.now(istZone);
                LocalDateTime now = nowZoned.toLocalDateTime();
                
                if ((now.isAfter(product.getPriceStartDate()) || now.isEqual(product.getPriceStartDate())) &&
                    (now.isBefore(product.getPriceEndDate()) || now.isEqual(product.getPriceEndDate()))) {
                    price = product.getScheduledPrice();
                }
            }
        } catch (Exception e) {
            // If scheduled price check fails, use regular price
            System.err.println("Error checking scheduled price: " + e.getMessage());
        }
        
        return price;
    }

    /**
     * Get the original price for feed (for sale_price comparison)
     * Returns the higher of originalPrice or current price before scheduled price
     */
    private BigDecimal getOriginalPriceForFeed(Product product) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal originalPrice = product.getOriginalPrice();
        BigDecimal currentPrice = getCurrentPrice(product);
        
        // If originalPrice exists and is higher than current, use it
        if (originalPrice != null && originalPrice.compareTo(currentPrice) > 0) {
            return originalPrice;
        }
        
        // If no originalPrice but we have originalPriceBeforeSchedule, use that
        if (product.getOriginalPriceBeforeSchedule() != null && 
            product.getOriginalPriceBeforeSchedule().compareTo(currentPrice) > 0) {
            return product.getOriginalPriceBeforeSchedule();
        }
        
        // Fallback: if product has scheduled price active, use the regular price as original
        if (product.getScheduledPrice() != null && 
            product.getPriceStartDate() != null && 
            product.getPriceEndDate() != null) {
            try {
                ZoneId istZone = ZoneId.of("Asia/Kolkata");
                ZonedDateTime nowZoned = ZonedDateTime.now(istZone);
                LocalDateTime now = nowZoned.toLocalDateTime();
                
                if ((now.isAfter(product.getPriceStartDate()) || now.isEqual(product.getPriceStartDate())) &&
                    (now.isBefore(product.getPriceEndDate()) || now.isEqual(product.getPriceEndDate()))) {
                    // Scheduled price is active, so regular price is the "original"
                    if (product.getPrice() != null && product.getPrice().compareTo(currentPrice) > 0) {
                        return product.getPrice();
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // If no better option, return current price (no sale)
        return currentPrice;
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

