package database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ChannelMembersRepositoryTest {

    private Connection connection;
    private UserRepository userRepository;
    private ChannelMembersRepository channelMembersRepository;

    private int aliceId;
    private int bobId;
    private int generalChannelId;
    private int randomChannelId;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DatabaseTestSupport.newInMemoryConnection();
        userRepository = new UserRepository(connection);
        channelMembersRepository = new ChannelMembersRepository(connection);

        userRepository.signUp("alice", "pw1");
        userRepository.signUp("bob", "pw2");
        aliceId = userRepository.getUserId("alice");
        bobId = userRepository.getUserId("bob");

        generalChannelId = insertChannel("general", "alice");
        randomChannelId = insertChannel("random", "alice");
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    private int insertChannel(String name, String creator) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO channels (name, type, creator) VALUES (?, 'group', ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, creator);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id FROM channels WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet results = stmt.executeQuery()) {
                results.next();
                return results.getInt("id");
            }
        }
    }

    @Test
    void addMemberToChannelRecordsTheMembership() {
        channelMembersRepository.addMemberToChannel(generalChannelId, aliceId);

        assertTrue(channelMembersRepository.doesMemberExistInChannel(aliceId, generalChannelId));
    }

    @Test
    void doesMemberExistInChannelIsFalseBeforeJoining() {
        assertFalse(channelMembersRepository.doesMemberExistInChannel(aliceId, generalChannelId));
    }

    @Test
    void addMemberToChannelSkipsAUserWhoAlreadyHasAMembershipRowElsewhere() {
        // addMemberToChannel's existence check only looks at user_id, not
        // the (channel_id, user_id) pair together, so a user who is
        // already a member of *any* channel is skipped when added to a
        // different one. This test pins down that current behaviour.
        channelMembersRepository.addMemberToChannel(generalChannelId, aliceId);

        channelMembersRepository.addMemberToChannel(randomChannelId, aliceId);

        assertTrue(channelMembersRepository.doesMemberExistInChannel(aliceId, generalChannelId));
        assertFalse(channelMembersRepository.doesMemberExistInChannel(aliceId, randomChannelId));
    }

    @Test
    void removeAllChannelMembersOnlyClearsTheGivenChannel() {
        channelMembersRepository.addMemberToChannel(generalChannelId, aliceId);
        channelMembersRepository.addMemberToChannel(randomChannelId, bobId);

        channelMembersRepository.removeAllChannelMembers(generalChannelId);

        assertFalse(channelMembersRepository.doesMemberExistInChannel(aliceId, generalChannelId));
        assertTrue(channelMembersRepository.doesMemberExistInChannel(bobId, randomChannelId));
    }

    @Test
    void getAllMembersResolvesUsernamesForTheGivenChannel() {
        channelMembersRepository.addMemberToChannel(generalChannelId, aliceId);
        channelMembersRepository.addMemberToChannel(randomChannelId, bobId);

        ArrayList<String> members = channelMembersRepository.getAllMembers(generalChannelId, userRepository);

        assertEquals(1, members.size());
        assertEquals("alice", members.get(0));
    }

    @Test
    void getAllMembersReturnsEmptyForAChannelWithNoMembers() {
        ArrayList<String> members = channelMembersRepository.getAllMembers(generalChannelId, userRepository);

        assertTrue(members.isEmpty());
    }
}
