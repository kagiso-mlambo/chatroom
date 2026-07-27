package database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ChannelMembersRepository {
    private Connection connection;

    public ChannelMembersRepository(Connection connection){
        this.connection = connection;
    }

    public void addMemberToChannel(int channelId, int userId) {
        String userIDQuery = "SELECT user_id FROM channel_members WHERE user_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(userIDQuery)){
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){ return; }
        } catch (SQLException e) {
            System.out.println("ChannelMemberRepository - Method addMemberToChannel: ");
            System.out.println(e);
        }

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

    public ArrayList<String> getAllMembers(int channelID, UserRepository userRepo){
        ArrayList <String> members = new ArrayList<>();
        String userIDQuery = "SELECT user_id FROM channel_members WHERE channel_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(userIDQuery)){
            preparedStatement.setInt(1, channelID);
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){ members.add(userRepo.getAUser(results.getInt("user_id"))); }

        }catch (SQLException e) {
            System.out.println("ChannelMemberRepository - getAllMembers: ");
            System.out.println(e);
        }
        return members;
    }

}
