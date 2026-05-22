package com.budget.buoy.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadProfilePicture(MultipartFile file, String userId) throws IOException {
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }

        // Convert MultipartFile → temp File
        File tempFile = File.createTempFile("profile-", "-" + file.getOriginalFilename());
        file.transferTo(tempFile);

        try {
            Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                    "folder", "profile_pictures",
                    "public_id", "user_" + userId,
                    "overwrite", true,
                    "resource_type", "image",
                    "width", 400,
                    "height", 400,
                    "crop", "fill",
                    "gravity", "face"));
            return uploadResult;
        } finally {
            tempFile.delete(); // always clean up
        }
    }

    public void deleteProfilePicture(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
    }
}
