package com.example.styloflow.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Cloudinary storage; with a blank {@code CLOUDINARY_URL} the app still starts and uploads are rejected. */
@Service
public class CloudinaryImageStorageService implements ImageStorageService {
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;

    public CloudinaryImageStorageService(@Value("${cloudinary.url:}") String cloudinaryUrl) {
        this.cloudinary = cloudinaryUrl.isBlank() ? null : new Cloudinary(cloudinaryUrl);
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) return null;
        var contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new BusinessRuleException("The file must be an image");
        if (file.getSize() > MAX_SIZE)
            throw new BusinessRuleException("The image must be at most 5 MB");
        if (cloudinary == null)
            throw new BusinessRuleException("Image storage is not configured");
        try {
            var result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", folder, "resource_type", "image"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new UncheckedIOException("Image upload failed", e);
        }
    }
}
