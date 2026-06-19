package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChannelRepository {
    private Connection connection;
    private ChannelMembersRepository channelMembersRepo;
    private UserRepository userRepo;

    public ChannelRepository(Connection connection, ChannelMembersRepository channelMembersRepo, UserRepository userRepo){
        this.connection = connection;
        this.channelMembersRepo = channelMembersRepo;
        this.userRepo = userRepo;
    }

    public int getChannelId(String channelName, String type, String sender, String receiver) throws SQLException {
        String selectQuery = "SELECT id FROM channel WHERE name = ?";
        int channelID;
        int userID;

        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);){
            preparedStatement.setString(1, channelName);
            ResultSet result = preparedStatement.executeQuery();

            if (result.next()){ channelID = result.getInt("id"); }
            else{
                insertNewChannel(channelName, type);

                channelID = getChannelId(channelName, type, sender, receiver);
                int senderUserID = userRepo.getUserId(sender);
                int receiverUserID = userRepo.getUserId(receiver);

                channelMembersRepo.addMemberToChannel(channelID, senderUserID);
                channelMembersRepo.addMemberToChannel(channelID, receiverUserID);
            }

        } catch (SQLException e) {
            throw e;
        }
        return channelID;
    }

    private void insertNewChannel(String channelName, String type) throws SQLException{
        String insertQuery = "INSERT INTO channel (name, type) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)){
            preparedStatement.setString(1, channelName);
            preparedStatement.setString(2, type);
            preparedStatement.executeUpdate();
        }
    }
}
