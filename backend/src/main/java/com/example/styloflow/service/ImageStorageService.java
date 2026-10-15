package com.example.styloflow.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    /**
     * Uploads an image and returns its public URL.
     *
     * @param file   uploaded file; {@code null} or empty means "no image"
     * @param folder destination folder, e.g. {@code "styloflow/tenant_1/products"}
     * @return the secure URL, or {@code null} when there is no file
     * @throws com.example.styloflow.exception.BusinessRuleException if the file is not an image, exceeds 5 MB,
     *                                                               or image storage is not configured
     */
    String upload(MultipartFile file, String folder);
}
