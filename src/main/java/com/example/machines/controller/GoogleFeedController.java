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
        String xmlFeed = googleFeedService.generateGoogleProductFeed();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set("Cache-Control", "public, max-age=3600"); // Cache for 1 hour
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(xmlFeed);
    }

    @GetMapping(value = "/google-feed.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getGoogleProductFeedJson() {
        return ResponseEntity.ok(googleFeedService.generateGoogleProductFeedJson());
    }
}

