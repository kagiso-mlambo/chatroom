package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    public String logIn(String[] userCredentials) throws SQLException {
        String query = "SELECT password_hash FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userCredentials[0]);
            try (ResultSet result = pstmt.executeQuery()) {
                if (!result.next()) {
                    return "Username or password was entered incorrectly!\nIf you don't have an account please sign up.";
                }
                String storedPassword = result.getString("password");
                if (!userCredentials[1].equals(storedPassword)) {
                    return "Username or password was entered incorrectly!\nIf you don't have an account please sign up.";
                }
            }
        }
        return "Logged in successfully";
    }

    public String signUp(String[] userCredentials) throws SQLException {
        String query =  "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userCredentials[1]);
            pstmt.setString(2, userCredentials[2]);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return "That username is already taken, please choose another.";
            }
            throw e;
        }
        return "Sign up successful!";
    }
}
