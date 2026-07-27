package database;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MessagesRepository {
    private Connection connection;
    private final int MESSAGE_LIMIT = 20;

    public MessagesRepository(Connection connection){
        this.connection = connection;
    }

    public void addMessage(int channelID, int userID, String message){
        String query = "INSERT INTO messages (channel_id, user_id, content) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelID);
            preparedStatement.setInt(2, userID);
            preparedStatement.setString(3, message);
            preparedStatement.executeUpdate();
        }  catch (SQLException e) {
            System.out.println("MessagesRepository - addMessage: ");
            System.out.println(e);
        }
    }

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
            System.out.print("UserRepository - Method getAUsers: ");
            System.out.println(e);
        }
        return lastMessages;
    }
}
