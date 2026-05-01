package com.raez.finance.util;

import com.raez.finance.dao.FinanceUserDao;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary utility to create one FinanceUser for testing login.
 * Run from Eclipse: Right‑click this class → Run As → Java Application.
 * Or: mvn -q exec:java -Dexec.mainClass=com.raez.finance.util.FinanceCreateTestUser -Dexec.args="email password username FirstName LastName ADMIN 1"
 *
 * Args: email password username firstName lastName role active
 *   role: ADMIN or FINANCE_USER
 *   active: 1 (active) or 0 (inactive)
 *
 * Example: test@test.com mypass testuser Test FinanceUser ADMIN 1
 */
public class FinanceCreateTestUser {
    private static final Logger log = LoggerFactory.getLogger(FinanceCreateTestUser.class);


    public static void main(String[] args) {
        String email, password, username, firstName, lastName, roleStr;
        boolean isActive;

        if (args != null && args.length >= 7) {
            email = args[0].trim();
            password = args[1];
            username = args[2].trim();
            firstName = args[3].trim();
            lastName = args[4].trim();
            roleStr = args[5].trim().toUpperCase();
            isActive = "1".equals(args[6].trim());
        } else {
            // Default test user when run from Eclipse without program arguments
            email = "test@raez.com";
            password = "Test123!";
            username = "testuser";
            firstName = "Test";
            lastName = "FinanceUser";
            roleStr = "ADMIN";
            isActive = true;
            log.info("{}", "No args provided – creating default test user (email=test@raez.com, password=Test123!)");
        }

        if (!"ADMIN".equals(roleStr) && !"FINANCE_USER".equals(roleStr)) {
            log.error("{}", "role must be ADMIN or FINANCE_USER");
            System.exit(1);
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        FinanceUserDao dao = new FinanceUserDao();

        try {
            dao.insertUser(email, username, passwordHash, roleStr, firstName, lastName, "",
                null, null, null, null, isActive);
            log.info("{}", "Created FinanceUser:");
            log.info("{}", "  email: " + email);
            log.info("{}", "  username: " + username);
            log.info("{}", "  firstName: " + firstName + ", lastName: " + lastName);
            log.info("{}", "  role: " + roleStr + ", active: " + isActive);
            log.info("");
            log.info("{}", "You can now login with email/username: " + email + " (or " + username + ") and your password.");
            log.info("{}", "(Role Selection → Admin FinanceLogin for ADMIN; Finance FinanceUser FinanceLogin for FINANCE_USER)");
        } catch (Exception e) {
            log.error("{}", "Failed to create user: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                log.error("{}", "That email or username already exists. Use different args or run with custom email/username.");
            }
            log.error("Error", e);
            System.exit(1);
        }
    }
}
