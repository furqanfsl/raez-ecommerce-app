package com.raez.util;

import com.raez.db.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-shot migration from SHA-256 password hashes to BCrypt ($2a$).
 *
 * SHA-256 is one-way, so we map known demo hashes back to known plaintexts
 * (every demo account in this repo seeds to raez123 / admin123). Anything
 * not in the table is logged and skipped — those users have to reset their
 * password through the normal recovery flow.
 *
 * Idempotent: rows already in $2a$/$2b$/$2y$ format are left alone.
 *
 * Run: mvn exec:java -Dexec.mainClass=com.raez.util.MigratePasswords
 */
public final class MigratePasswords {
    private static final Logger log = LoggerFactory.getLogger(MigratePasswords.class);


    private static final Map<String, String> KNOWN_SHA256 = Map.ofEntries(
        // SHA-256("raez123")  — standardised demo customer / staff password
        Map.entry("88a8f794da996191c92594eb6bad82fc5077ecd9db0967da39ed2d92b5fe02ee", "raez123"),
        // SHA-256("admin123") — super-admin demo password
        Map.entry("240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9", "admin123"),
        // Legacy customer seed hashes — all standardised to raez123 by DBConnection.migrateCustomerPasswords
        Map.entry("4e40e8ffe0ee32fa53e139147ed559229a5930f89c2204706fc174beb36210b3", "raez123"),
        Map.entry("56318228b3a39a2af341c080cc2d8b1d7e088ed24bd28d6cc9b34a8711253434", "raez123"),
        Map.entry("926b4b8a00cfab44b758450fa6bf188d4bf8541c2fd6b3d9b93d152d43a99f64", "raez123"),
        Map.entry("3688058a6965c4c8e143d7002afb557fe910657ad819714abb0356c7551c84b7", "raez123"),
        Map.entry("4eb84dcc7275bc750ea32fbfe061fc0477d7d332ed1071c1e06911ddec3a6556", "raez123")
    );

    public static void main(String[] args) throws Exception {
        // Trigger DB bootstrap (schema + seed) before reading.
        DBConnection.getInstance();

        Path backupDir = Paths.get(System.getProperty("user.home"), ".raez", "backups");
        Files.createDirectories(backupDir);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = backupDir.resolve("users-" + ts + ".csv");

        try (Connection c = DBConnection.openNew();
             PreparedStatement read = c.prepareStatement(
                 "SELECT userID, email, passwordHash FROM users ORDER BY userID");
             ResultSet rs = read.executeQuery();
             BufferedWriter w = Files.newBufferedWriter(backup, StandardCharsets.UTF_8)) {
            w.write("userID,email,passwordHash\n");
            while (rs.next()) {
                w.write(rs.getInt("userID") + ","
                    + csv(rs.getString("email")) + ","
                    + csv(rs.getString("passwordHash")) + "\n");
            }
        }
        log.info("{}", "Backup written: " + backup);

        int migrated = 0, alreadyBcrypt = 0, unknown = 0, empty = 0;

        try (Connection c = DBConnection.openNew();
             PreparedStatement read = c.prepareStatement(
                 "SELECT userID, email, passwordHash FROM users ORDER BY userID");
             PreparedStatement update = c.prepareStatement(
                 "UPDATE users SET passwordHash = ?, updatedAt = CURRENT_TIMESTAMP WHERE userID = ?");
             ResultSet rs = read.executeQuery()) {

            while (rs.next()) {
                int    id    = rs.getInt("userID");
                String email = rs.getString("email");
                String hash  = rs.getString("passwordHash");

                if (hash == null || hash.isBlank()) {
                    log.info("{}", "  [skip empty]  userID=" + id + " email=" + email);
                    empty++;
                    continue;
                }
                if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
                    alreadyBcrypt++;
                    continue;
                }
                String plain = KNOWN_SHA256.get(hash.toLowerCase());
                if (plain == null) {
                    log.error("{}", "  [WARN unknown]  userID=" + id + " email=" + email
                        + " — hash not in known-plaintext map. User must reset password.");
                    unknown++;
                    continue;
                }
                String bcrypt = BCrypt.hashpw(plain, BCrypt.gensalt(12));
                update.setString(1, bcrypt);
                update.setInt(2, id);
                update.executeUpdate();
                migrated++;
            }
        }

        log.info("{}", "Migrated " + migrated + " users (skipped "
            + alreadyBcrypt + " already-hashed, "
            + unknown + " unknown, "
            + empty + " empty)");

        if (unknown > 0) System.exit(2);
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private MigratePasswords() {}
}
