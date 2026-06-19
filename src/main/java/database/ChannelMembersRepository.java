package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ChannelMembersRepository {
    private Connection connection;

    public ChannelMembersRepository(Connection connection){
        this.connection = connection;
    }

    public void addMemberToChannel(int channelId, int userId) throws SQLException {
        String query = "INSERT INTO channel_members (channel_id, user_id) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelId);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        }
    }
}
