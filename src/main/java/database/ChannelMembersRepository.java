package database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ChannelMembersRepository {
    private Connection connection;

    public ChannelMembersRepository(Connection connection){
        this.connection = connection;
    }

    public void addMemberToChannel(int channelId, int userId) {
        String query = "INSERT INTO channel_members (channel_id, user_id) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelId);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ChannelMemberRepository - Method addMemberToChannel: ");
            System.out.println(e);
        }
    }

    public void removeAllChannelMembers(int channelID) {
        String query = "DELETE FROM channel_members WHERE channel_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelID);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ChannelMemberRepository - removeALLChannelMembers: ");
            System.out.println(e);
        }
    }

}
