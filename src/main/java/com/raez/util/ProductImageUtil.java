package com.raez.util;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

public final class ProductImageUtil {

    private static final Path SOURCE_IMAGE_DIR = Path.of(
        System.getProperty("user.dir"), "src", "main", "resources", "images", "products");
    private static final Path CLASSES_IMAGE_DIR = Path.of(
        System.getProperty("user.dir"), "target", "classes", "images", "products");

    private ProductImageUtil() {}

    public static String copyImageToResources(File sourceFile) throws IOException {
        if (sourceFile == null) throw new IOException("No source image file selected.");
        Files.createDirectories(SOURCE_IMAGE_DIR);
        Files.createDirectories(CLASSES_IMAGE_DIR);

        String original = sourceFile.getName();
        String ext = extensionOf(original);
        String base = sanitizeBaseName(stripExtension(original));
        if (base.isBlank()) base = "product_image";
        String targetName = base + ext;

        Path sourceTarget = SOURCE_IMAGE_DIR.resolve(targetName);
        if (Files.exists(sourceTarget)) {
            targetName = base + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            sourceTarget = SOURCE_IMAGE_DIR.resolve(targetName);
        }

        Files.copy(sourceFile.toPath(), sourceTarget, StandardCopyOption.REPLACE_EXISTING);
        Path classesTarget = CLASSES_IMAGE_DIR.resolve(targetName);
        Files.copy(sourceFile.toPath(), classesTarget, StandardCopyOption.REPLACE_EXISTING);
        return "/images/products/" + targetName;
    }

    public static Image loadFromProductPath(Class<?> context, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        try {
            if (imagePath.startsWith("/")) {
                try (InputStream is = context.getResourceAsStream(imagePath)) {
                    if (is != null) {
                        byte[] bytes = is.readAllBytes();
                        return new Image(new ByteArrayInputStream(bytes));
                    }
                }
                String normalized = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
                Path sourcePath = Path.of(System.getProperty("user.dir"), "src", "main", "resources", normalized);
                if (Files.exists(sourcePath)) {
                    return new Image(sourcePath.toUri().toString());
                }
                Path classesPath = Path.of(System.getProperty("user.dir"), "target", "classes", normalized);
                if (Files.exists(classesPath)) {
                    return new Image(classesPath.toUri().toString());
                }
                return null;
            }
            if (imagePath.toLowerCase(Locale.ROOT).startsWith("http://")
                || imagePath.toLowerCase(Locale.ROOT).startsWith("https://")) {
                return new Image(imagePath, true);
            }
        } catch (Exception e) {
            System.err.println("ProductImageUtil.loadFromProductPath: " + e.getMessage());
        }
        return null;
    }

    private static String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(0, idx) : filename;
    }

    private static String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return ".png";
        return filename.substring(idx).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeBaseName(String base) {
        return base.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_\\-]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    }
}
