package com.raez.storage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUpload {
    private static final Logger log = LoggerFactory.getLogger(TestUpload.class);

    public static void main(String[] args) throws Exception {
        Path dir = Path.of("src/main/resources/images/products");
        if (!Files.isDirectory(dir)) {
            log.error("{}", "Image directory not found: " + dir.toAbsolutePath());
            System.exit(1);
        }
        Optional<Path> first;
        try (Stream<Path> s = Files.list(dir)) {
            first = s.filter(Files::isRegularFile).findFirst();
        }
        if (first.isEmpty()) {
            log.error("{}", "No files found in " + dir.toAbsolutePath());
            System.exit(1);
        }
        File file = first.get().toFile();
        log.info("{}", "Uploading: " + file.getAbsolutePath());

        ImageStorage storage = ImageStorageFactory.create();
        log.info("{}", "Storage: " + storage.getClass().getSimpleName());

        long t0 = System.currentTimeMillis();
        String url = storage.upload(file);
        long ms = System.currentTimeMillis() - t0;

        log.info("{}", "URL: " + url);
        log.info("{}", "Time: " + ms + "ms");
    }
}
