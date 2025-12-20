package com.example.machines.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

    /**
     * Use system default timezone instead of forcing IST
     * This allows the application to work correctly in any timezone
     */
    @PostConstruct
    public void init() {
        // Use system default timezone - don't force IST
        System.out.println("Application using system timezone: " + TimeZone.getDefault().getID());
    }
}

