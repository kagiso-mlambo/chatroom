package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * Handles persistence for channel membership, the many to many relationship
 * between users and the channels they belong to.
 *
 * Used both when a user joins a channel and when the server needs to know
 * who to deliver a broadcast message to, or whether a given user is allowed
 * to send one in the first place.
 */
public class ChannelMembersRepository {
    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger(ChannelMembersRepository.class.getName());


    /**
     * Creates a repository backed by the given database connection.
     *
     * @param connection an open JDBC connection to the chatroom database
     */
    public ChannelMembersRepository(Connection connection){
        this.connection = connection;
    }


    /**
     * Adds a user as a member of a channel, if they aren't already recorded
     * as a member.
     *
     * @param channelId the id of the channel to add the user to
     * @param userId the id of the user to add
     */
    public void addMemberToChannel(int channelId, int userId) {
        String userIDQuery = "SELECT user_id FROM channel_members WHERE user_id = ? AND channel_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(userIDQuery)){
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, channelId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){ return; }
        } catch (SQLException e) {
            LOGGER.warning("ChannelMemberRepository - Method addMemberToChannel: " + e.getMessage());
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


    /**
     * Removes every member from a channel. Intended to be called when a
     * channel is deleted, so no orphaned membership rows are left behind.
     *
     * @param channelID the id of the channel to clear
     */
    public void removeAllChannelMembers(int channelID) {
        String query = "DELETE FROM channel_members WHERE channel_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, channelID);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("ChannelMemberRepository - removeALLChannelMembers: " + e.getMessage());
        }
    }


    /**
     * Looks up the usernames of every member of a channel.
     *
     * @param channelID the id of the channel to look up members for
     * @param userRepo used to resolve each stored user id back to a username
     * @return the usernames of every member of the channel, in no particular order
     */
    public ArrayList<String> getAllMembers(int channelID, UserRepository userRepo){
        ArrayList <String> members = new ArrayList<>();
        String userIDQuery = "SELECT user_id FROM channel_members WHERE channel_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(userIDQuery)){
            preparedStatement.setInt(1, channelID);
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){ members.add(userRepo.getAUser(results.getInt("user_id"))); }

        }catch (SQLException e) {
            LOGGER.warning("ChannelMemberRepository - getAllMembers: " + e.getMessage());
        }
        return members;
    }


    /**
     * Checks whether a given user is a member of a given channel. Used to
     * authorize actions like broadcasting a message, so only actual members
     * of a channel can send to it.
     *
     * @param userID the id of the user to check
     * @param channelID the id of the channel to check membership in
     * @return true if the user is a member of the channel, false otherwise
     */
    public boolean doesMemberExistInChannel(int userID, int channelID){
        ArrayList <Integer> members = new ArrayList<>();
        String userIDQuery = "SELECT user_id FROM channel_members WHERE channel_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(userIDQuery)){
            preparedStatement.setInt(1, channelID);
            ResultSet results = preparedStatement.executeQuery();
            while (results.next()){ members.add(results.getInt("user_id")); }
        }catch (SQLException e) {
            LOGGER.warning("ChannelMemberRepository - getAllMembers: " + e.getMessage());
        }
        return members.contains(userID);
    }

}
