package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;


/**
 * Handles persistence and verification of user accounts.
 *
 * Passwords are never stored or compared as plaintext; {@link #hashPassword(String)}
 * and {@link #checkPassword(String, String)} wrap BCrypt hashing and
 * verification, chosen over a bare hash like MD5 or SHA-256 because of its
 * adaptive cost factor and built in salt.
 */
public class UserRepository {
    private final Connection connection;
    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());

    /**
     * Creates a repository backed by the given database connection.
     *
     * @param connection an open JDBC connection to the chatroom database
     */
    public UserRepository(Connection connection) {
        this.connection = connection;
    }


    /**
     * Hashes a plaintext password with BCrypt, ready to be stored.
     *
     * @param password the plaintext password to hash
     * @return the salted BCrypt hash of the password
     */
    private String hashPassword(String password) {
        int logRounds = 12;
        String salt = BCrypt.gensalt(logRounds);
        return BCrypt.hashpw(password, salt);
    }


    /**
     * Checks a plaintext password attempt against a stored BCrypt hash.
     *
     * @param password the plaintext password attempt
     * @param storedHash the BCrypt hash previously stored for the account
     * @return true if the password matches the hash, false otherwise
     */
    private boolean checkPassword(String password, String storedHash) {
        return BCrypt.checkpw(password, storedHash);
    }


    /**
     * Verifies a login attempt against the stored, hashed password for that
     * username.
     *
     * @param user the username attempting to log in
     * @param enteredPassword the plaintext password attempt
     * @return a message describing the outcome, either a success message or
     *         a generic "incorrect" message that deliberately doesn't reveal
     *         whether the username or the password was the problem
     */
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
            LOGGER.warning("UserRepository - Method logIn: " + e.getMessage());
        }
        return "Logged in successfully";
    }


    /**
     * Creates a new user account with a BCrypt hashed password.
     *
     * @param user the desired username
     * @param enteredPassword the plaintext password to hash and store
     * @return a message describing the outcome: success, or an explanation
     *         if the username is already taken
     */
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
                LOGGER.warning("UserRepository - Method signUp: " + e.getMessage());
        }
        return "Sign up successful!";
    }


    /**
     * Checks whether a username is already registered.
     *
     * @param username the username to check
     * @return true if an account with that username exists, false otherwise
     */
    public boolean checkIfUserExists(String username) {
        String query = "SELECT * FROM users WHERE username = ?";

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, username);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()){
                return true;
            }
        } catch (SQLException e) {
            LOGGER.warning("UserRepository - Method checkIfUserExists: " + e.getMessage());
        }
        return false;
    }


    /**
     * Looks up a user's id by username.
     *
     * @param username the username to look up
     * @return the user's id, or -1 if no user with that username exists
     */
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
            LOGGER.warning("UserRepository - Method getUserId: " + e.getMessage());
        }
        return id;
    }


    /**
     * Lists every registered username.
     *
     * @return the usernames of every registered user
     */
    public ArrayList<String> getAllUsers() {
        ArrayList<String> users = new ArrayList<>();
        String query = "SELECT username FROM users";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){
                users.add(results.getString("username"));
            }
        } catch (SQLException e) {
            LOGGER.warning("UserRepository - Method getAllUsers: " + e.getMessage());
        }
        return users;
    }


    /**
     * Looks up a username by user id.
     *
     * @param userID the id of the user to look up
     * @return the matching username, or an empty string if no user with
     *         that id exists
     */
    public String getAUser(int userID){
        String query = "SELECT username FROM users WHERE id = ?";
        String username = "";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, userID);
            ResultSet results = preparedStatement.executeQuery();
            if (results.next()){username = results.getString("username"); }
        } catch (SQLException e) {
            LOGGER.warning("UserRepository - Method getAUsers: " + e.getMessage());
        }
        return username;
    }
}
