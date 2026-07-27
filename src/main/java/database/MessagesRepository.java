package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public Map<Integer, String> getLastMessages(int channelId){
        Map<Integer, String> lastMessages = new HashMap<>();
        String query = "SELECT * FROM messages WHERE channel_id = ? ORDER BY sent_at DESC LIMIT 20";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelId);
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){
                int userID = results.getInt("user_id");
                String message = results.getString("content");
                lastMessages.put(userID, message);
            }
        } catch (SQLException e) {
            System.out.print("UserRepository - Method getAUsers: ");
            System.out.println(e);
        }
        return lastMessages;
    }
}
