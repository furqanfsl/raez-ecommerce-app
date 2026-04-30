package com.raez.storage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class TestUpload {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of("src/main/resources/images/products");
        if (!Files.isDirectory(dir)) {
            System.err.println("Image directory not found: " + dir.toAbsolutePath());
            System.exit(1);
        }
        Optional<Path> first;
        try (Stream<Path> s = Files.list(dir)) {
            first = s.filter(Files::isRegularFile).findFirst();
        }
        if (first.isEmpty()) {
            System.err.println("No files found in " + dir.toAbsolutePath());
            System.exit(1);
        }
        File file = first.get().toFile();
        System.out.println("Uploading: " + file.getAbsolutePath());

        ImageStorage storage = ImageStorageFactory.create();
        System.out.println("Storage: " + storage.getClass().getSimpleName());

        long t0 = System.currentTimeMillis();
        String url = storage.upload(file);
        long ms = System.currentTimeMillis() - t0;

        System.out.println("URL: " + url);
        System.out.println("Time: " + ms + "ms");
    }
}
