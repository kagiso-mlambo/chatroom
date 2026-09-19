package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.mindrot.jbcrypt.BCrypt;

public class UserRepository {
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    public String hashPassword(String password) {
        int logRounds = 12;
        String salt = BCrypt.gensalt(logRounds);
        return BCrypt.hashpw(password, salt);
    }

    public boolean checkPassword(String password, String storedHash) {
        return BCrypt.checkpw(password, storedHash);
    }

    public String logIn(String user, String enteredPassword) {
        String query = "SELECT password_hash FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user);
            try (ResultSet result = stmt.executeQuery()) {
                if (!result.next()) {
                    return "Username or password is incorrect!.\nIf you don't have an account please sign up.";
                }
                String storedPassword = result.getString("password_hash");
                if (!checkPassword(enteredPassword, storedPassword)) {
                    return "Username or password is incorrect!.\nIf you don't have an account please sign up.";
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository - Method logIn: ");
            System.out.println(e);
        }
        return "Logged in successfully";
    }

    public String signUp(String user, String enteredPassword) {
        String query =  "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user);

            String password = hashPassword(enteredPassword);
            stmt.setString(2, password);

            stmt.executeUpdate();

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

    public String getAUser(int userID){
        String query = "SELECT username FROM users WHERE id = ?";
        String username = "";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, userID);
            ResultSet results = preparedStatement.executeQuery();
            if (results.next()){username = results.getString("username"); }
        } catch (SQLException e) {
            System.out.println("UserRepository - Method getAUsers: ");
            System.out.println(e);
        }
        return username;
    }
}
