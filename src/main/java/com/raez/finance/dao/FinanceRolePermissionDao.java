package com.raez.finance.dao;

import com.raez.finance.model.FinanceUserRole;
import com.raez.finance.util.FinanceDatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Checks role_permissions table for RBAC.
 */
public class FinanceRolePermissionDao {

    /**
     * Returns true if the given role has the given action in role_permissions.
     */
    public boolean hasPermission(FinanceUserRole role, String action) {
        if (role == null || action == null || action.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM role_permissions WHERE role = ? AND action = ? LIMIT 1";
        try (Connection conn = FinanceDatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ps.setString(2, action.trim());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }
}
