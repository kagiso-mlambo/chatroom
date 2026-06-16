package database;

import java.sql.*;

public class DatabaseInitialiser {
    public static void initialise() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users ( " +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

//            stmt.execute("CREATE TABLE IF NOT EXISTS channels (\n" +
//                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
//                    "    name TEXT,\n" +
//                    "    type TEXT NOT NULL,\n" +
//                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
//                    ")");
//            stmt.execute("CREATE TABLE IF NOT EXISTS channel_members (\n" +
//                    "    channel_id INTEGER NOT NULL,\n" +
//                    "    user_id INTEGER NOT NULL,\n" +
//                    "    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
//                    "    FOREIGN KEY (channel_id) REFERENCES channels(id),\n" +
//                    "    FOREIGN KEY (user_id) REFERENCES users(id)\n" +
//                    ")");
//            stmt.execute("CREATE TABLE IF NOT EXISTS messages (\n" +
//                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
//                    "    channel_id INTEGER NOT NULL,\n" +
//                    "    user_id INTEGER NOT NULL,\n" +
//                    "    content TEXT NOT NULL,\n" +
//                    "    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
//                    "    FOREIGN KEY (channel_id) REFERENCES channels(id),\n" +
//                    "    FOREIGN KEY (user_id) REFERENCES users(id)\n" +
//                    ")");
        }
    }
}
