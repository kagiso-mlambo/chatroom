package database;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;


/**
 * Handles persistence for chat messages: saving new ones and retrieving
 * recent history for a channel.
 *
 * Used both when a message is broadcast (to save it) and when a client
 * joins a channel or opens a private message (to replay recent history).
 */
public class MessagesRepository {
    private Connection connection;
    private final int MESSAGE_LIMIT = 20;
    private static final Logger LOGGER = Logger.getLogger(MessagesRepository.class.getName());


    /**
     * Creates a repository backed by the given database connection.
     *
     * @param connection an open JDBC connection to the chatroom database
     */
    public MessagesRepository(Connection connection){
        this.connection = connection;
    }


    /**
     * Saves a message to a channel.
     *
     * @param channelID the id of the channel the message was sent to
     * @param userID the id of the user who sent the message
     * @param message the message content
     */
    public void addMessage(int channelID, int userID, String message){
        String query = "INSERT INTO messages (channel_id, user_id, content) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelID);
            preparedStatement.setInt(2, userID);
            preparedStatement.setString(3, message);
            preparedStatement.executeUpdate();
        }  catch (SQLException e) {
            LOGGER.warning("MessagesRepository - addMessage: " + e.getMessage());
        }
    }


    /**
     * Retrieves the most recent messages in a channel, oldest first, so they
     * can be replayed to a client in the order they were originally sent.
     *
     * @param channelId the id of the channel to retrieve history for
     * @return the channel's recent messages, keyed by the sender's user id
     */
    public Multimap<Integer, String> getLastMessages(int channelId){
        Multimap<Integer, String> lastMessages = LinkedListMultimap.create();
        String query = "SELECT * FROM messages WHERE channel_id = ? ORDER BY sent_at ASC LIMIT 20";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelId);
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){
                System.out.println("The Message id is {ID: " + results.getInt("id") + "}" );
                int userID = results.getInt("user_id");
                System.out.println("The user id is {UserId: " + userID + "}" );
                String message = results.getString("content");
                System.out.println("The message is {Message: " + userID + "}" );
                System.out.println();
                lastMessages.put(userID, message);
            }
        } catch (SQLException e) {
            LOGGER.warning("UserRepository - Method getAUsers: " + e.getMessage());
        }
        return lastMessages;
    }
}
