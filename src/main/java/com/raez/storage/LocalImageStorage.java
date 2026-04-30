package com.raez.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

public class LocalImageStorage implements ImageStorage {
    private static final Logger log = Logger.getLogger(LocalImageStorage.class.getName());

    private final Path imagesDir;

    public LocalImageStorage() {
        this.imagesDir = Path.of(System.getProperty("user.home"), ".raez", "images");
    }

    @Override
    public String upload(File file) throws IOException {
        Files.createDirectories(imagesDir);
        String name = file.getName();
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0) ext = name.substring(dot);
        String filename = UUID.randomUUID() + ext;
        Path dest = imagesDir.resolve(filename);
        Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        String url = dest.toUri().toString();
        log.info("Local upload OK: " + url);
        return url;
    }

    @Override
    public void delete(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) return;
        Path p = imagesDir.resolve(publicId);
        Files.deleteIfExists(p);
        log.info("Local delete OK: " + publicId);
    }

    @Override
    public String getPublicIdFromUrl(String url) {
        if (url == null) return null;
        int slash = url.lastIndexOf('/');
        if (slash < 0) return url;
        return url.substring(slash + 1);
    }
}
