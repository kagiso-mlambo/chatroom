package database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ChannelRepositoryTest {

    private Connection connection;
    private UserRepository userRepository;
    private ChannelMembersRepository channelMembersRepository;
    private ChannelRepository channelRepository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DatabaseTestSupport.newInMemoryConnection();
        userRepository = new UserRepository(connection);
        channelMembersRepository = new ChannelMembersRepository(connection);
        channelRepository = new ChannelRepository(connection, channelMembersRepository, userRepository);

        userRepository.signUp("alice", "pw1");
        userRepository.signUp("bob", "pw2");
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void getChannelIdReturnsMinusOneForAnUnknownChannel() {
        assertEquals(-1, channelRepository.getChannelID("nowhere"));
    }

    @Test
    void createGroupChannelCanBeFoundAfterwards() {
        channelRepository.createGroupChannel("general", "alice");

        assertTrue(channelRepository.getChannelID("general") > 0);
    }

    @Test
    void getChannelsListsOnlyGroupChannelsNotPrivateOnes() {
        channelRepository.createGroupChannel("general", "alice");
        channelRepository.createGroupChannel("random", "alice");
        channelRepository.privateChannel("alice_bob", "alice", "bob");

        ArrayList<String> channels = channelRepository.getChannels(-1);

        assertEquals(2, channels.size());
        assertTrue(channels.contains("general"));
        assertTrue(channels.contains("random"));
        assertFalse(channels.contains("alice_bob"));
    }

    @Test
    void privateChannelCreatesTheChannelAndAddsBothUsers() {
        int channelId = channelRepository.privateChannel("alice_bob", "alice", "bob");

        assertTrue(channelId > 0);
        int aliceId = userRepository.getUserId("alice");
        int bobId = userRepository.getUserId("bob");
        assertTrue(channelMembersRepository.doesMemberExistInChannel(aliceId, channelId));
        assertTrue(channelMembersRepository.doesMemberExistInChannel(bobId, channelId));
    }

    @Test
    void privateChannelIsIdempotentForTheSamePair() {
        int firstCallId = channelRepository.privateChannel("alice_bob", "alice", "bob");

        int secondCallId = channelRepository.privateChannel("alice_bob", "alice", "bob");

        assertEquals(firstCallId, secondCallId);
    }

    @Test
    void deleteChannelFailsWhenTheRequesterIsNotTheCreator() throws SQLException {
        channelRepository.createGroupChannel("general", "alice");

        boolean deleted = channelRepository.deleteChannel("general", "bob");

        assertFalse(deleted);
        assertTrue(channelRepository.getChannelID("general") > 0);
    }

    @Test
    void deleteChannelFailsForAnUnknownChannel() throws SQLException {
        assertFalse(channelRepository.deleteChannel("ghost", "alice"));
    }

    @Test
    void deleteChannelRemovesTheChannelAndItsMembersWhenRequestedByItsCreator() throws SQLException {
        channelRepository.createGroupChannel("general", "alice");
        int channelId = channelRepository.getChannelID("general");
        int aliceId = userRepository.getUserId("alice");
        channelMembersRepository.addMemberToChannel(channelId, aliceId);

        boolean deleted = channelRepository.deleteChannel("general", "alice");

        assertTrue(deleted);
        assertEquals(-1, channelRepository.getChannelID("general"));
        assertFalse(channelMembersRepository.doesMemberExistInChannel(aliceId, channelId));
    }
}
