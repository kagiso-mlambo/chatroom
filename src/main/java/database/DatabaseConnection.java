package database;

import java.sql.*;


/**
 * Provides connections to the SQLite chatroom database.
 *
 * Every repository class obtains its connection through this single point,
 * so the JDBC URL only needs to be defined in one place.
 */
public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:chatroom.db";

    /**
     * Opens a new connection to the chatroom database.
     *
     * @return a JDBC connection to chatroom.db
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
