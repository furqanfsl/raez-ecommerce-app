package com.raez.storage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class ImageStorageFactory {
    private static final Logger log = Logger.getLogger(ImageStorageFactory.class.getName());

    private ImageStorageFactory() {}

    public static ImageStorage create() {
        if ("local".equalsIgnoreCase(System.getProperty("raez.storage"))) {
            log.info("ImageStorage = Local (forced via -Draez.storage=local)");
            return new LocalImageStorage();
        }

        Path configPath = Path.of(System.getProperty("user.home"), ".raez", "config.properties");
        if (!Files.exists(configPath)) {
            log.info("ImageStorage = Local (no config at " + configPath + ")");
            return new LocalImageStorage();
        }

        if (!isCloudinaryReachable()) {
            log.warning("ImageStorage = Local (Cloudinary host unreachable)");
            return new LocalImageStorage();
        }

        try {
            ImageStorage cloud = CloudinaryImageStorage.fromConfig();
            log.info("ImageStorage = Cloudinary");
            return cloud;
        } catch (IOException e) {
            log.warning("ImageStorage = Local (Cloudinary init failed: " + e.getMessage() + ")");
            return new LocalImageStorage();
        }
    }

    private static boolean isCloudinaryReachable() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("api.cloudinary.com", 443), 5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
