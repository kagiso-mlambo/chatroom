package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class UserRepository {
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    public String logIn(String[] userCredentials) throws SQLException {
        String query = "SELECT password_hash FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userCredentials[1]);
            try (ResultSet result = pstmt.executeQuery()) {
                if (!result.next()) {
                    return "Username was entered incorrectly!.";
                }
                String storedPassword = result.getString("password_hash");
                if (!userCredentials[2].equals(storedPassword)) {
                    return "Password was entered incorrectly. If you don't have an account please sign up.";
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

    public boolean checkIfUserExists(String username) throws SQLException{
        String query = "SELECT * FROM users WHERE username = ?";

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, username);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()){
                return true;
            }
        }
        return false;
    }

    public int getUserId(String username) throws SQLException{
        String query = "SELECT user_id FROM users WHERE username = ?";
        int id = -1;

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, username);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()){
                id = result.getInt("user_id");
            }
        }
        return id;
    }

    public ArrayList<String> getAllUsers() throws SQLException{
        ArrayList<String> users = new ArrayList<>();
        String query = "SELECT username FROM users";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){
                users.add(results.getString("name"));
            }
        }
        return users;
    }
}
