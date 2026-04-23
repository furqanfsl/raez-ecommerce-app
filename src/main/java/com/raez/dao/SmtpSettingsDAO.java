package com.raez.dao;

import com.raez.db.DBConnection;
import com.raez.model.SmtpSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SmtpSettingsDAO {

    public SmtpSettings load() {
        String sql = "SELECT host, port, username, password, fromAddress, fromName, useTls, isEnabled "
                   + "FROM smtp_settings WHERE settingID = 1";
        try (Connection c = DBConnection.getInstance().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                SmtpSettings s = new SmtpSettings();
                s.host        = rs.getString("host");
                s.port        = rs.getInt("port");
                s.username    = rs.getString("username");
                s.password    = rs.getString("password");
                s.fromAddress = rs.getString("fromAddress");
                s.fromName    = rs.getString("fromName");
                s.useTls      = rs.getInt("useTls") == 1;
                s.isEnabled   = rs.getInt("isEnabled") == 1;
                return s;
            }
        } catch (SQLException e) {
            System.err.println("SmtpSettingsDAO.load failed: " + e.getMessage());
        }
        return new SmtpSettings();
    }

    public boolean save(SmtpSettings s) {
        String sql = "INSERT INTO smtp_settings "
                   + "(settingID, host, port, username, password, fromAddress, fromName, useTls, isEnabled, updatedAt) "
                   + "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) "
                   + "ON CONFLICT(settingID) DO UPDATE SET "
                   + "  host = excluded.host, port = excluded.port, "
                   + "  username = excluded.username, password = excluded.password, "
                   + "  fromAddress = excluded.fromAddress, fromName = excluded.fromName, "
                   + "  useTls = excluded.useTls, isEnabled = excluded.isEnabled, "
                   + "  updatedAt = CURRENT_TIMESTAMP";
        try (Connection c = DBConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.host);
            ps.setInt   (2, s.port);
            ps.setString(3, s.username);
            ps.setString(4, s.password);
            ps.setString(5, s.fromAddress);
            ps.setString(6, s.fromName);
            ps.setInt   (7, s.useTls    ? 1 : 0);
            ps.setInt   (8, s.isEnabled ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SmtpSettingsDAO.save failed: " + e.getMessage());
            return false;
        }
    }
}
