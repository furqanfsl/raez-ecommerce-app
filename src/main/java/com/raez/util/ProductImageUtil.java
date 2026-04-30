package com.raez.util;

import com.raez.storage.ImageStorage;
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

    /**
     * Background-loads a product image. If src is an http(s) URL it is treated as a
     * Cloudinary URL — auto-applies the q_auto,f_auto detail transform. Use
     * {@link #loadThumbnail} for list views to get a fixed crop.
     */
    public static Image loadProductImage(String src) {
        if (src == null || src.isBlank()) return null;
        String lower = src.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            String url = src.contains("/upload/") && !src.contains("q_auto")
                ? src.replace("/upload/", "/upload/q_auto,f_auto/")
                : src;
            return new Image(url, true);
        }
        return loadFromProductPath(ProductImageUtil.class, src);
    }

    /** Same as {@link #loadProductImage} but injects a Cloudinary fill+resize transform for list thumbnails. */
    public static Image loadThumbnail(String src, int w, int h) {
        if (src == null || src.isBlank()) return null;
        String lower = src.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return new Image(ImageStorage.thumbnail(src, w, h), true);
        }
        return loadFromProductPath(ProductImageUtil.class, src);
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
            String low = imagePath.toLowerCase(Locale.ROOT);
            if (low.startsWith("file:")) {
                return new Image(imagePath, true);
            }
            if (low.startsWith("http://") || low.startsWith("https://")) {
                String url = imagePath.contains("/upload/") && !imagePath.contains("q_auto")
                    ? imagePath.replace("/upload/", "/upload/q_auto,f_auto/")
                    : imagePath;
                return new Image(url, true);
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
