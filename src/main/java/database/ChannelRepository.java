package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * Handles persistence for channels, both named group channels (created with
 * /create) and the private, two person channels used for direct messages.
 *
 * Depends on {@link ChannelMembersRepository} to keep channel membership in
 * sync whenever a channel is created or deleted, and on {@link UserRepository}
 * to resolve usernames to user ids for private channel creation.
 */
public class ChannelRepository {
    private Connection connection;
    private ChannelMembersRepository channelMembersRepo;
    private UserRepository userRepo;
    private static final Logger LOGGER = Logger.getLogger(ChannelRepository.class.getName());


    /**
     * Creates a repository backed by the given database connection and
     * collaborating repositories.
     *
     * @param connection an open JDBC connection to the chatroom database
     * @param channelMembersRepo used to add and remove channel membership rows
     * @param userRepo used to resolve usernames to user ids
     */
    public ChannelRepository(Connection connection, ChannelMembersRepository channelMembersRepo, UserRepository userRepo){
        this.connection = connection;
        this.channelMembersRepo = channelMembersRepo;
        this.userRepo = userRepo;
    }


    /**
     * Looks up a channel's id by name.
     *
     * @param channelName the channel's name
     * @return the channel's id, or -1 if no channel with that name exists
     */
    public int getChannelID(String channelName){
        int channelID = -1;
        String query = "SELECT id FROM channels WHERE name = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            ResultSet results = preparedStatement.executeQuery();
            if (results.next()) channelID = results.getInt("id");
        } catch (SQLException e) {
            LOGGER.warning("ChannelRepository - Method getChannelID: " + e.getMessage());
        }

        return channelID;
    }


    /**
     * Finds the id of the private channel between two users, creating it
     * (and adding both users as members) if it doesn't exist yet.
     *
     * @param channelName the deterministic name for this pair's private
     *                    channel, built from the two usernames
     * @param sender the user initiating or continuing the private message
     * @param receiver the user on the other end of the private message
     * @return the private channel's id
     */
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
            LOGGER.warning("ChannelRepository - Method privateChannel: " + e.getMessage());
        }
        return channelID;
    }


    /**
     * Inserts a new row into the channels table with type "private".
     * Used internally by {@link #privateChannel(String, String, String)}
     * when a private channel doesn't exist yet.
     *
     * @param channelName the name to give the new channel
     */
    private void insertNewChannel(String channelName) {
        String insertQuery = "INSERT INTO channels (name, type) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)){
            preparedStatement.setString(1, channelName);
            preparedStatement.setString(2, "private");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("ChannelRepository - Method insertNewChannel: " + e.getMessage());
        }
    }


    /**
     * Lists the names of every group channel.
     *
     * @param userID currently unused, the result is the same for every caller
     * @return the names of all channels of type "group"
     */
    public ArrayList<String> getChannels(int userID) {
        ArrayList<String> channels =  new ArrayList<>();
        String channelQuery = "SELECT name FROM channels WHERE type = ?";

        try(PreparedStatement pStmt = connection.prepareStatement(channelQuery)){
            pStmt.setString(1, "group");
            ResultSet channelResults = pStmt.executeQuery();
            while (channelResults.next()){ channels.add(channelResults.getString("name")); }
        } catch (SQLException e){
            LOGGER.warning("ChannelRepository - Method getUserChannels: " + e.getMessage());
        }
        return channels;
    }


    /**
     * Creates a new named group channel, recording who created it so
     * {@link #deleteChannel(String, String)} can later verify ownership.
     *
     * @param channelName the name for the new channel
     * @param creator the username of the user creating the channel
     */
    public void createGroupChannel(String channelName, String creator) {
        String query = "INSERT INTO channels (name, type, creator) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            preparedStatement.setString(2, "group");
            preparedStatement.setString(3, creator);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("ChannelRepository - Method createGroupChannel: " + e.getMessage());
        }
    }


    /**
     * Checks whether a given user is the creator of a given channel.
     * Used to authorize channel deletion, so only the creator can delete it.
     *
     * @param channelName the channel to check
     * @param username the username to check ownership for
     * @return true if the user created the channel, false otherwise or if
     *         the channel doesn't exist
     */
    private boolean checkCreator(String channelName, String username){
        String query = "SELECT * FROM channels WHERE name = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            ResultSet channelResults = preparedStatement.executeQuery();
            while (channelResults.next()){
                if (channelResults.getString("creator").equals(username)){ return true; }
            }
        } catch (SQLException e) {
            LOGGER.warning("ChannelRepository - Method deleteChannel: " + e.getMessage());
        }
        return false;
    }


    /**
     * Deletes a channel and all of its membership rows, but only if the
     * requesting user is the channel's creator.
     *
     * @param channelName the channel to delete
     * @param username the username requesting the deletion
     * @return true if the channel was deleted, false if the user isn't the
     *         creator or the deletion failed
     * @throws SQLException if a database error occurs
     */
    public boolean deleteChannel(String channelName, String username) throws SQLException{
        if (!checkCreator(channelName, username)) {return false; };

        String query = "DELETE FROM channels WHERE name = ?";

        int channelID = getChannelID(channelName);

        channelMembersRepo.removeAllChannelMembers(channelID);

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, channelName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("ChannelRepository - Method deleteChannel: " + e.getMessage());
            return false;
        }
        return true;
    }
}
