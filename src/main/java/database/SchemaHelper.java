package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class SchemaHelper {

    public static Set<String> getColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();

        String sql = "PRAGMA table_info(" + tableName + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }

        return columns;
    }

    public static String findColumn(Connection conn, String tableName, String... candidates) throws SQLException {
        Set<String> columns = getColumns(conn, tableName);

        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase())) {
                return candidate;
            }
        }

        throw new SQLException("Could not find matching column in table '" + tableName + "'");
    }

    public static boolean hasColumn(Connection conn, String tableName, String candidate) throws SQLException {
        return getColumns(conn, tableName).contains(candidate.toLowerCase());
    }
}