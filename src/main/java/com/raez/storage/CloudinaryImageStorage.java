package com.raez.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class CloudinaryImageStorage implements ImageStorage {
    private static final Logger log = Logger.getLogger(CloudinaryImageStorage.class.getName());
    private static final String FOLDER = "raez/products";

    private final Cloudinary cloudinary;

    public CloudinaryImageStorage(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public static CloudinaryImageStorage fromConfig() throws IOException {
        Path configPath = Path.of(System.getProperty("user.home"), ".raez", "config.properties");
        if (!Files.exists(configPath)) {
            throw new IOException("Cloudinary config not found at " + configPath);
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(configPath)) {
            props.load(in);
        }
        String cloudName = props.getProperty("cloudinary.cloud_name");
        String apiKey = props.getProperty("cloudinary.api_key");
        String apiSecret = props.getProperty("cloudinary.api_secret");
        if (cloudName == null || cloudName.isBlank()
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new IOException("Cloudinary config is missing one or more keys");
        }
        Cloudinary c = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        return new CloudinaryImageStorage(c);
    }

    @Override
    public String upload(File file) throws IOException {
        long start = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "folder", FOLDER,
                    "resource_type", "image"
            ));
            String url = (String) result.get("secure_url");
            long ms = System.currentTimeMillis() - start;
            log.info("Cloudinary upload OK in " + ms + "ms: " + url);
            return url;
        } catch (Exception e) {
            throw new IOException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete OK: " + publicId);
        } catch (Exception e) {
            throw new IOException("Cloudinary delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPublicIdFromUrl(String url) {
        if (url == null || !url.contains("/upload/")) return null;
        String after = url.substring(url.indexOf("/upload/") + "/upload/".length());
        if (after.startsWith("v") && after.contains("/")) {
            int slash = after.indexOf('/');
            String maybeVersion = after.substring(0, slash);
            if (maybeVersion.length() > 1 && maybeVersion.substring(1).chars().allMatch(Character::isDigit)) {
                after = after.substring(slash + 1);
            }
        }
        int dot = after.lastIndexOf('.');
        if (dot > 0) after = after.substring(0, dot);
        return after;
    }
}
