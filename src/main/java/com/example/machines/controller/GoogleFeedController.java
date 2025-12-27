package com.example.machines.controller;

import com.example.machines.service.GoogleFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GoogleFeedController {

    @Autowired
    private GoogleFeedService googleFeedService;

    @GetMapping(value = "/google-feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getGoogleProductFeed() {
        try {
            String xmlFeed = googleFeedService.generateGoogleProductFeed();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Cache-Control", "public, max-age=3600"); // Cache for 1 hour
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(xmlFeed);
        } catch (Exception e) {
            System.err.println("Error generating Google feed: " + e.getMessage());
            e.printStackTrace();
            
            // Return minimal valid XML on error
            String errorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<rss version=\"2.0\" xmlns:g=\"http://base.google.com/ns/1.0\">\n" +
                    "  <channel>\n" +
                    "    <title>Andhra Machines Agencies</title>\n" +
                    "    <link>https://andhramachinesagencies.com</link>\n" +
                    "    <description>Product feed temporarily unavailable</description>\n" +
                    "  </channel>\n" +
                    "</rss>";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            
            return ResponseEntity.status(500)
                    .headers(headers)
                    .body(errorXml);
        }
    }

    @GetMapping(value = "/google-feed.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getGoogleProductFeedJson() {
        return ResponseEntity.ok(googleFeedService.generateGoogleProductFeedJson());
    }
}

