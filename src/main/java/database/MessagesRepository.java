package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MessagesRepository {
    private Connection connection;

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
}
