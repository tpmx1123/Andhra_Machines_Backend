package com.example.machines.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
            "folder", "sewing machines",
            "resource_type", "auto"
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }

    public void deleteImage(String imageUrl) throws IOException {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Extract public_id from URL
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        }
    }

    private String extractPublicId(String url) {
        try {
            // Extract public_id from Cloudinary URL
            // Format: https://res.cloudinary.com/{cloud_name}/image/upload/{folder}/{public_id}.{ext}
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Remove file extension
                int lastDot = path.lastIndexOf('.');
                if (lastDot > 0) {
                    return path.substring(0, lastDot);
                }
                return path;
            }
        } catch (Exception e) {
            // If extraction fails, return null
        }
        return null;
    }
}

