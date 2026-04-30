package com.raez.util;

import com.raez.db.DBConnection;
import com.raez.storage.ImageStorage;
import com.raez.storage.ImageStorageFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MigrateImagesToCloud {

    private record Row(int id, String name, String imagePath) {}

    public static void main(String[] args) {
        Connection conn = DBConnection.getInstance().getConnection();
        ImageStorage storage = ImageStorageFactory.create();
        System.out.println("Storage backend: " + storage.getClass().getSimpleName());

        List<Row> rows = loadPending(conn);
        System.out.println("Pending products to migrate: " + rows.size());
        if (rows.isEmpty()) {
            System.out.println("Nothing to do.");
            return;
        }

        try {
            writeBackup(rows);
        } catch (IOException e) {
            System.err.println("FATAL: backup failed, aborting: " + e.getMessage());
            System.exit(2);
            return;
        }

        int ok = 0;
        int fail = 0;
        List<Integer> failedIds = new ArrayList<>();
        int i = 0;
        for (Row r : rows) {
            i++;
            System.out.printf("Migrating %d/%d (id=%d): %s%n", i, rows.size(), r.id, r.name);
            try {
                File local = resolveLocalFile(r.imagePath);
                if (local == null) {
                    throw new IOException("local file not found for " + r.imagePath);
                }
                String url = storage.upload(local);
                String publicId = storage.getPublicIdFromUrl(url);
                updateRow(conn, r.id, url, publicId);
                ok++;
            } catch (Exception e) {
                fail++;
                failedIds.add(r.id);
                System.err.println("  FAILED id=" + r.id + ": " + e.getMessage());
            }
        }

        System.out.printf("Done. %d succeeded, %d failed.%n", ok, fail);
        if (fail > 0) {
            System.err.println("Failed product IDs: " + failedIds);
            System.exit(1);
        }
    }

    private static List<Row> loadPending(Connection conn) {
        List<Row> out = new ArrayList<>();
        String sql = "SELECT productID, name, imagePath FROM products " +
                     "WHERE imagePath IS NOT NULL AND imagePath <> '' " +
                     "  AND (imageUrl IS NULL OR imageUrl = '')";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new Row(rs.getInt("productID"),
                                rs.getString("name"),
                                rs.getString("imagePath")));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read products: " + e.getMessage(), e);
        }
        return out;
    }

    private static void writeBackup(List<Row> rows) throws IOException {
        Path dir = Path.of(System.getProperty("user.home"), ".raez", "backups");
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = dir.resolve("images-map-" + stamp + ".csv");
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("product_id,image_path");
            w.newLine();
            for (Row r : rows) {
                w.write(r.id + "," + csv(r.imagePath));
                w.newLine();
            }
        }
        System.out.println("Backup written: " + file);
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static File resolveLocalFile(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        String normalized = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

        Path src = Path.of(System.getProperty("user.dir"), "src", "main", "resources", normalized);
        if (Files.exists(src)) return src.toFile();

        Path classes = Path.of(System.getProperty("user.dir"), "target", "classes", normalized);
        if (Files.exists(classes)) return classes.toFile();

        Path abs = Path.of(imagePath);
        if (Files.exists(abs)) return abs.toFile();

        return null;
    }

    private static void updateRow(Connection conn, int id, String url, String publicId)
            throws Exception {
        String sql = "UPDATE products SET imageUrl = ?, imagePublicId = ? WHERE productID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, url);
            ps.setString(2, publicId);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }
}
