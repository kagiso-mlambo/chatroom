package database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private Connection connection;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DatabaseTestSupport.newInMemoryConnection();
        userRepository = new UserRepository(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void signUpCreatesANewAccount() {
        String result = userRepository.signUp("alice", "hunter2");

        assertEquals("Sign up successful!", result);
        assertTrue(userRepository.checkIfUserExists("alice"));
    }

    @Test
    void signUpRejectsADuplicateUsername() {
        userRepository.signUp("alice", "hunter2");

        String result = userRepository.signUp("alice", "differentPassword");

        assertEquals("That username is already taken, please choose another.", result);
    }

    @Test
    void checkIfUserExistsIsFalseForAnUnknownUsername() {
        assertFalse(userRepository.checkIfUserExists("nobody"));
    }

    @Test
    void logInSucceedsWithTheCorrectPassword() {
        userRepository.signUp("alice", "hunter2");

        String result = userRepository.logIn("alice", "hunter2");

        assertEquals("Logged in successfully", result);
    }

    @Test
    void logInFailsWithAnIncorrectPassword() {
        userRepository.signUp("alice", "hunter2");

        String result = userRepository.logIn("alice", "wrongPassword");

        assertEquals("Username or password is incorrect!.\nIf you don't have an account please sign up.", result);
    }

    @Test
    void logInFailsForAnUnknownUsername() {
        String result = userRepository.logIn("nobody", "whatever");

        assertEquals("Username or password is incorrect!.\nIf you don't have an account please sign up.", result);
    }

    @Test
    void passwordsAreNeverStoredAsPlaintext() throws SQLException {
        userRepository.signUp("alice", "hunter2");

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT password_hash FROM users WHERE username = ?")) {
            stmt.setString(1, "alice");
            try (ResultSet results = stmt.executeQuery()) {
                assertTrue(results.next());
                assertNotEquals("hunter2", results.getString("password_hash"));
            }
        }
    }

    @Test
    void getUserIdReturnsAPositiveIdForARegisteredUser() {
        userRepository.signUp("alice", "hunter2");

        assertTrue(userRepository.getUserId("alice") > 0);
    }

    @Test
    void getUserIdReturnsMinusOneForAnUnknownUsername() {
        assertEquals(-1, userRepository.getUserId("nobody"));
    }

    @Test
    void getAllUsersListsEveryRegisteredUsername() {
        userRepository.signUp("alice", "pw1");
        userRepository.signUp("bob", "pw2");

        ArrayList<String> users = userRepository.getAllUsers();

        assertEquals(2, users.size());
        assertTrue(users.contains("alice"));
        assertTrue(users.contains("bob"));
    }

    @Test
    void getAUserReturnsTheUsernameForAKnownId() {
        userRepository.signUp("alice", "hunter2");
        int id = userRepository.getUserId("alice");

        assertEquals("alice", userRepository.getAUser(id));
    }

    @Test
    void getAUserReturnsEmptyStringForAnUnknownId() {
        assertEquals("", userRepository.getAUser(999));
    }
}
