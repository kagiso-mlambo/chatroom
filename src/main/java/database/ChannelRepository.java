package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ChannelRepository {
    private Connection connection;
    private ChannelMembersRepository channelMembersRepo;
    private UserRepository userRepo;

    public ChannelRepository(Connection connection, ChannelMembersRepository channelMembersRepo, UserRepository userRepo){
        this.connection = connection;
        this.channelMembersRepo = channelMembersRepo;
        this.userRepo = userRepo;
    }

    public int getChannelID(String channelName){
        int channelID = -1;
        String query = "SELECT id FROM channels WHERE name = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            ResultSet results = preparedStatement.executeQuery();
            if (results.next()) channelID = results.getInt("id");
        } catch (SQLException e) {
            System.out.println("ChannelRepository - Method getChannelID: ");
            System.out.println(e);
        }

        return channelID;
    }

    public int privateChannel(String channelName, String sender, String receiver) {
        String selectQuery = "SELECT id FROM channels WHERE name = ?";
        int channelID = -1;
        int userID;

        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);){
            preparedStatement.setString(1, channelName);
            ResultSet result = preparedStatement.executeQuery();

            if (result.next()){ channelID = result.getInt("id"); }
            else{
                insertNewChannel(channelName);

                channelID = privateChannel(channelName, sender, receiver);
                int senderUserID = userRepo.getUserId(sender);
                int receiverUserID = userRepo.getUserId(receiver);

                channelMembersRepo.addMemberToChannel(channelID, senderUserID);
                channelMembersRepo.addMemberToChannel(channelID, receiverUserID);
            }

        } catch (SQLException e) {
            System.out.println("ChannelRepository - Method privateChannel: ");
            System.out.println(e);
        }
        return channelID;
    }

    private void insertNewChannel(String channelName) {
        String insertQuery = "INSERT INTO channels (name, type) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)){
            preparedStatement.setString(1, channelName);
            preparedStatement.setString(2, "private");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ChannelRepository - Method insertNewChannel: ");
            System.out.println(e);
        }
    }

    public ArrayList<String> getChannels(int userID) {
        ArrayList<String> channels =  new ArrayList<>();
        String channelQuery = "SELECT name FROM channels WHERE type = ?";

        try(PreparedStatement pStmt = connection.prepareStatement(channelQuery)){
            pStmt.setString(1, "group");
            ResultSet channelResults = pStmt.executeQuery();
            while (channelResults.next()){ channels.add(channelResults.getString("name")); }
        } catch (SQLException e){
            System.out.println("ChannelRepository - Method getUserChannels: ");
            System.out.println(e);
        }
        return channels;
    }

    public void createGroupChannel(String channelName, String creator) {
        String query = "INSERT INTO channels (name, type, creator) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            preparedStatement.setString(2, "group");
            preparedStatement.setString(3, creator);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ChannelRepository - Method createGroupChannel: ");
            System.out.println(e);
        }
    }

    private boolean checkCreator(String channelName, String username){
        String query = "SELECT * FROM channels WHERE name = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            ResultSet channelResults = preparedStatement.executeQuery();
            while (channelResults.next()){
                if (channelResults.getString("creator").equals(username)){ return true; }
            }
        } catch (SQLException e) {
            System.out.print("ChannelRepository - Method deleteChannel: ");
            System.out.println(e);
        }
        return false;
    }

    public boolean deleteChannel(String channelName, String username) throws SQLException{
        if (!checkCreator(channelName, username)) {return false; };

        String query = "DELETE FROM channels WHERE name = ?";

        int channelID = getChannelID(channelName);

        channelMembersRepo.removeAllChannelMembers(channelID);

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ChannelRepository - Method deleteChannel: ");
            System.out.println(e);
            return false;
        }
        return true;
    }
}
