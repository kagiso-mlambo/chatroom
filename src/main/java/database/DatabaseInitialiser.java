package database;

import java.sql.*;


/**
 * Creates the chatroom database's schema on server startup.
 *
 * Sets up all four tables (users, channels, channel_members, messages) using
 * "CREATE TABLE IF NOT EXISTS", so it's safe to call every time the server
 * starts without wiping existing data.
 */
public class DatabaseInitialiser {

    /**
     * Creates the users, channels, channel_members, and messages tables if
     * they don't already exist.
     *
     * @throws SQLException if a database access error occurs
     */
    public static void initialise() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users ( " +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS channels (\n" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    name TEXT UNIQUE NOT NULL,\n" +
                    "    type TEXT NOT NULL,\n" +
                    "creator TEXT, \n" +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS channel_members (\n" +
                    "    channel_id INTEGER NOT NULL,\n" +
                    "    user_id INTEGER NOT NULL,\n" +
                    "    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    FOREIGN KEY (channel_id) REFERENCES channels(id),\n" +
                    "    FOREIGN KEY (user_id) REFERENCES users(id)\n" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS messages (\n" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    channel_id INTEGER NOT NULL,\n" +
                    "    user_id INTEGER NOT NULL,\n" +
                    "    content TEXT NOT NULL,\n" +
                    "    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    FOREIGN KEY (channel_id) REFERENCES channels(id),\n" +
                    "    FOREIGN KEY (user_id) REFERENCES users(id)\n" +
                    ")");
        }
    }
}
