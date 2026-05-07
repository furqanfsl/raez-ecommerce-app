package com.raez;

import com.raez.db.DBConnection;
import com.raez.service.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private static final String EMAIL = "auth.test@raez.org.uk";
    private static final String PLAIN = "AuthTestPa55!";

    @BeforeAll
    static void boot() throws Exception {
        TestDb.init();
        seedBcryptUser();
    }

    @Test
    void login_correctPassword_returnsUser() {
        Optional<AuthService.AuthenticatedSession> session = AuthService.authenticate(EMAIL, PLAIN);
        assertTrue(session.isPresent(), "correct password should authenticate");
        assertEquals(EMAIL, session.get().user().email);
        assertTrue(session.get().allRoleNames().contains("customer"), "customer role should be loaded");
    }

    @Test
    void login_wrongPassword_returnsFailure() {
        Optional<AuthService.AuthenticatedSession> session = AuthService.authenticate(EMAIL, "totally-wrong");
        assertTrue(session.isEmpty(), "wrong password must not authenticate");
    }

    private static void seedBcryptUser() throws Exception {
        Connection c = DBConnection.getInstance().getConnection();
        try (PreparedStatement check = c.prepareStatement("SELECT userID FROM users WHERE email = ?")) {
            check.setString(1, EMAIL);
            ResultSet rs = check.executeQuery();
            if (rs.next()) return; // already seeded by an earlier test class
        }
        String hash = BCrypt.hashpw(PLAIN, BCrypt.gensalt(10));
        int userId;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (email, username, passwordHash, firstName, lastName, isActive) "
              + "VALUES (?, 'auth_test', ?, 'Auth', 'Test', 1)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, EMAIL);
            ps.setString(2, hash);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            userId = keys.getInt(1);
        }
        // role 2 = customer (per seed)
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO user_roles (userID, roleID) VALUES (?, 2)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
