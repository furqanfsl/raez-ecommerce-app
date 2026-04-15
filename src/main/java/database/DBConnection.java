package database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DB_FILE_NAME = "data.db";

    public static Connection getConnection() throws SQLException {
        Path dbPath = Paths.get(DB_FILE_NAME).toAbsolutePath();
        String url = "jdbc:sqlite:" + dbPath.toString();
        return DriverManager.getConnection(url);
    }
}