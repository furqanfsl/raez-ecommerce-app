package com.raez;

import com.raez.db.DBConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Boots {@link DBConnection} against a one-shot temp SQLite file so JUnit
 * tests get a real, fully-migrated, fully-seeded database without touching
 * the dev raez.db. Sets {@code raez.db.path} + {@code raez.db.reset} before
 * the singleton initialises. Idempotent across test classes within one JVM.
 */
public final class TestDb {

    private static volatile boolean initialised = false;

    private TestDb() {}

    public static synchronized void init() {
        if (initialised) return;
        try {
            Path tempDb = Files.createTempFile("raeztest-", ".db");
            Files.deleteIfExists(tempDb);
            System.setProperty("raez.db.path", tempDb.toString());
            System.setProperty("raez.db.reset", "true");
            tempDb.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new IllegalStateException("Could not allocate test DB file", e);
        }
        DBConnection.getInstance();
        initialised = true;
    }
}
