package com.example.machines;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.File;

@SpringBootApplication
@EnableAsync
public class MachinesApplication {
	public static void main(String[] args) {
		// Load .env file before Spring Boot starts
		try {
			// Try to find .env file in multiple locations
			String[] possiblePaths = {
				".env",                    // Current directory
				"./.env",                  // Current directory (explicit)
				"backend/.env",            // If running from project root
				"../.env"                  // Parent directory
			};
			
			Dotenv dotenv = null;
			for (String path : possiblePaths) {
				File envFile = new File(path);
				if (envFile.exists() && envFile.isFile()) {
					dotenv = Dotenv.configure()
							.filename(".env")
							.directory(envFile.getParent() != null ? envFile.getParent() : ".")
							.ignoreIfMissing()
							.load();
					System.out.println("✓ Found .env file at: " + envFile.getAbsolutePath());
					break;
				}
			}
			
			// If not found in specific paths, try default location
			if (dotenv == null) {
				dotenv = Dotenv.configure()
						.directory("./")
						.ignoreIfMissing()
						.load();
			}
			
			// Set system properties from .env file (Spring Boot will read these)
			int loadedCount = 0;
			for (var entry : dotenv.entries()) {
				String key = entry.getKey();
				String value = entry.getValue();
				// Set as system property so Spring Boot can access it via ${VAR_NAME}
				System.setProperty(key, value);
				loadedCount++;
			}
			
			if (loadedCount > 0) {
				System.out.println("✓ Environment variables loaded from .env file");
				System.out.println("  Loaded " + loadedCount + " variables");
			} else {
				System.out.println("⚠ No environment variables found in .env file");
			}
		} catch (Exception e) {
			System.err.println("⚠ Warning: Could not load .env file: " + e.getMessage());
			System.err.println("  Make sure .env file exists in the backend root directory");
			System.err.println("  Current working directory: " + System.getProperty("user.dir"));
			System.err.println("  Application will continue with system environment variables or defaults");
		}
		
		SpringApplication.run(MachinesApplication.class, args);
	}
}
