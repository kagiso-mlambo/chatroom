package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Test-only helper that opens a fresh, schema-initialised in-memory SQLite
 * connection, mirroring {@link DatabaseInitialiser}'s table definitions
 * without touching the real {@code chatroom.db} file on disk.
 *
 * Each call returns a brand new in-memory database tied to the returned
 * connection; closing that connection discards it, so tests get full
 * isolation from one another as long as they open their own connection in
 * setup and close it in teardown.
 */
final class DatabaseTestSupport {

    private DatabaseTestSupport() { }

    static Connection newInMemoryConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE users ( " +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE channels (" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    name TEXT UNIQUE NOT NULL," +
                    "    type TEXT NOT NULL," +
                    "creator TEXT, " +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("CREATE TABLE channel_members (" +
                    "    channel_id INTEGER NOT NULL," +
                    "    user_id INTEGER NOT NULL," +
                    "    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (channel_id) REFERENCES channels(id)," +
                    "    FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");

            stmt.execute("CREATE TABLE messages (" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    channel_id INTEGER NOT NULL," +
                    "    user_id INTEGER NOT NULL," +
                    "    content TEXT NOT NULL," +
                    "    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (channel_id) REFERENCES channels(id)," +
                    "    FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");
        }

        return connection;
    }
}
