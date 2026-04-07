package service;
import org.mindrot.jbcrypt.BCrypt;

import database.DBConnection;
import model.User;

import java.sql.*;

/**
 * Handles user registration, login, and password management.
 *
 * Password Security:
 *   - Passwords are hashed with BCrypt (work factor 12) before storage.
 *   - Plain-text passwords are NEVER stored in the database.
 *   - BCrypt automatically handles per-user salt generation.
 *
 * Dependency: jBCrypt-0.4.jar (add to classpath)
 * Download: https://www.mindrot.org/projects/jBCrypt/
 * Maven: org.mindrot:jbcrypt:0.4
 */
public class AuthService {

    // BCrypt work factor — higher = slower but more secure
    // 12 is a good balance for 2024 hardware
    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Registers a new user. Returns true if successful, false if username taken.
     */
    public static boolean register(String username, String plainPassword,
                                   String firstName, String lastName) {
        // Hash the password with BCrypt before touching the DB
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));

        String sql = "INSERT INTO users (username, password_hash, first_name, last_name) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.executeUpdate();
            System.out.println("User registered: " + username);
            return true;
        } catch (SQLException e) {
            // SQLITE_CONSTRAINT (19) = unique constraint violation (duplicate username)
            if (e.getErrorCode() == 19) {
                System.out.println("Username already taken: " + username);
                return false;
            }
            throw new RuntimeException("Registration failed", e);
        }
    }

    /**
     * Authenticates a user. Returns the User object on success, null on failure.
     */
    public static User login(String username, String plainPassword) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null; // User not found

            String storedHash = rs.getString("password_hash");
            // BCrypt.checkpw does constant-time comparison — safe against timing attacks
            if (!BCrypt.checkpw(plainPassword, storedHash)) return null;

            // Login successful — build and return User object
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(storedHash);
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            user.setCreatedAt(rs.getString("created_at"));
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    /**
     * Checks password strength. Returns score 0–4.
     * 0 = very weak, 4 = strong
     */
    public static int passwordStrength(String password) {
        int score = 0;
        if (password.length() >= 8)               score++;
        if (password.matches(".*[A-Z].*"))         score++;
        if (password.matches(".*[0-9].*"))         score++;
        if (password.matches(".*[^A-Za-z0-9].*")) score++;
        return score;
    }

    /**
     * Returns a human-readable strength label.
     */
    public static String strengthLabel(int score) {
        return switch (score) {
            case 0  -> "Very Weak";
            case 1  -> "Weak";
            case 2  -> "Fair";
            case 3  -> "Good";
            case 4  -> "Strong";
            default -> "Unknown";
        };
    }
}
