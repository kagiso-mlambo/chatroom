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

    public String logIn(String[] userCredentials) {
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
        } catch (SQLException e) {
            System.out.println("UserRepository - Method logIn: ");
            System.out.println(e);
        }
        return "Logged in successfully";
    }

    public String signUp(String[] userCredentials) {
        String query =  "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userCredentials[1]);
            pstmt.setString(2, userCredentials[2]);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return "That username is already taken, please choose another.";
            }
                System.out.println("UserRepository - Method signUp: ");
                System.out.println(e);
        }
        return "Sign up successful!";
    }

    public boolean checkIfUserExists(String username) {
        String query = "SELECT * FROM users WHERE username = ?";

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, username);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()){
                return true;
            }
        } catch (SQLException e) {
            System.out.println("UserRepository - Method checkIfUserExists: ");
            System.out.println(e);
        }
        return false;
    }

    public int getUserId(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        int id = -1;

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, username);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()){
                id = result.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println("UserRepository - Method getUserId: ");
            System.out.println(e);
        }
        return id;
    }

    public ArrayList<String> getAllUsers() {
        ArrayList<String> users = new ArrayList<>();
        String query = "SELECT username FROM users";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){
                users.add(results.getString("username"));
            }
        } catch (SQLException e) {
            System.out.println("UserRepository - Method getAllUsers: ");
            System.out.println(e);
        }
        return users;
    }
}
