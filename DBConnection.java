package database;

import java.sql.*;

/**
 * Manages the SQLite database connection and schema initialization.
 * 
 * Dependencies: sqlite-jdbc-3.x.x.jar (add to classpath)
 * Download: https://github.com/xerial/sqlite-jdbc/releases
 */
public class DBConnection {

    private static final String DB_URL = "jdbc:sqlite:notevault.db";
    private static Connection connection;

    /**
     * Returns the singleton database connection.
     */
   public static Connection getConnection() {
    try {
        Class.forName("org.sqlite.JDBC"); // IMPORTANT
        return DriverManager.getConnection("jdbc:sqlite:notevault.db");
    } catch (Exception e) {
        throw new RuntimeException("Failed to connect to database", e);
    }
}

    /**
     * Creates the schema if it doesn't exist.
     * Call this once at startup from Main.java.
     */
    public static void initialize() {

    String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "username TEXT NOT NULL UNIQUE," +
            "password_hash TEXT NOT NULL," +
            "first_name TEXT NOT NULL," +
            "last_name TEXT NOT NULL," +
            "created_at DATETIME DEFAULT (datetime('now'))" +
            ");";

    String notesTable = "CREATE TABLE IF NOT EXISTS notes (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER NOT NULL," +
            "title TEXT NOT NULL," +
            "body TEXT NOT NULL," +
            "category TEXT DEFAULT 'Personal'," +
            "created_at DATETIME DEFAULT (datetime('now'))," +
            "updated_at DATETIME DEFAULT (datetime('now'))," +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
            ");";

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement()) {

        
        stmt.execute("PRAGMA journal_mode=WAL;");

        stmt.execute(usersTable);
        stmt.execute(notesTable);

        System.out.println("INIT FIX APPLIED");

    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}

    /**
     * Closes the connection (call on app shutdown).
     */
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
