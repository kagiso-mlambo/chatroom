package database;

import com.google.common.collect.Multimap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class MessagesRepositoryTest {

    private Connection connection;
    private MessagesRepository messagesRepository;
    private int channelOneId;
    private int channelTwoId;
    private int userId;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DatabaseTestSupport.newInMemoryConnection();
        messagesRepository = new MessagesRepository(connection);

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {
            stmt.setString(1, "alice");
            stmt.setString(2, "hash");
            stmt.executeUpdate();
        }
        userId = lastInsertedId();

        channelOneId = insertChannel("general");
        channelTwoId = insertChannel("random");
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    private int insertChannel(String name) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO channels (name, type) VALUES (?, 'group')")) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        }
        return lastInsertedId();
    }

    private int lastInsertedId() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet results = stmt.executeQuery("SELECT last_insert_rowid() AS id")) {
            results.next();
            return results.getInt("id");
        }
    }

    @Test
    void addMessagePersistsItForRetrieval() {
        messagesRepository.addMessage(channelOneId, userId, "hello there");

        Multimap<Integer, String> messages = messagesRepository.getLastMessages(channelOneId);

        assertEquals(1, messages.size());
        assertTrue(messages.get(userId).contains("hello there"));
    }

    @Test
    void getLastMessagesOnlyReturnsMessagesForTheGivenChannel() {
        messagesRepository.addMessage(channelOneId, userId, "in general");
        messagesRepository.addMessage(channelTwoId, userId, "in random");

        Multimap<Integer, String> generalMessages = messagesRepository.getLastMessages(channelOneId);

        assertEquals(1, generalMessages.size());
        assertTrue(generalMessages.get(userId).contains("in general"));
        assertFalse(generalMessages.get(userId).contains("in random"));
    }

    @Test
    void getLastMessagesReturnsEmptyForAChannelWithNoHistory() {
        Multimap<Integer, String> messages = messagesRepository.getLastMessages(channelOneId);

        assertTrue(messages.isEmpty());
    }

    @Test
    void getLastMessagesCapsAtTwentyMessages() {
        for (int i = 0; i < 25; i++) {
            messagesRepository.addMessage(channelOneId, userId, "message " + i);
        }

        Multimap<Integer, String> messages = messagesRepository.getLastMessages(channelOneId);

        assertEquals(20, messages.size());
    }
}
