package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandProcessorTest {

    @Mock
    private Server server;

    @Mock
    private ClientHandler client;

    private CommandProcessor commandProcessor;

    @BeforeEach
    void setUp() {
        commandProcessor = new CommandProcessor(server, client);
    }

    @Test
    void quitClosesTheClient() {
        commandProcessor.handleCommand("/quit");

        verify(client).closeClient();
        verifyNoInteractions(server);
    }

    @Test
    void usersListsEveryOtherConnectedClientButNotTheCaller() throws SQLException {
        when(client.username()).thenReturn("alice");
        when(server.getAllClients()).thenReturn(new ArrayList<>(List.of("alice", "bob")));

        commandProcessor.handleCommand("/users");

        verify(client).sendMessage("bob");
        verify(client, never()).sendMessage("alice");
    }

    @Test
    void usersSwallowsASqlExceptionFromTheServer() throws SQLException {
        when(server.getAllClients()).thenThrow(new SQLException("db down"));

        assertDoesNotThrow(() -> commandProcessor.handleCommand("/users"));
    }

    @Test
    void createAsksTheServerToAddTheChannelAndConfirmsToTheClient() {
        when(client.username()).thenReturn("alice");

        commandProcessor.handleCommand("/create general");

        verify(server).addGroupChannel("general", "alice");
        verify(client).sendMessage(contains("general"));
    }

    @Test
    void joinAsksTheServerToJoinTheChannel() {
        when(client.username()).thenReturn("alice");

        commandProcessor.handleCommand("/join general");

        verify(server).joinNewChannel("alice", "general");
    }

    @Test
    void deleteAsksTheServerToDeleteTheChannel() {
        when(client.username()).thenReturn("alice");

        commandProcessor.handleCommand("/delete general");

        verify(server).deleteChannel("general", "alice");
    }

    @Test
    void groupsListsEveryGroupChannelReturnedByTheServer() {
        when(client.username()).thenReturn("alice");
        when(server.getAllGroupChannels("alice")).thenReturn(new ArrayList<>(List.of("general", "random")));

        commandProcessor.handleCommand("/groups");

        verify(client).sendMessage("general");
        verify(client).sendMessage("random");
    }

    @Test
    void anythingElseIsTreatedAsAPrivateMessageRequestWithTheSlashStripped() {
        when(client.username()).thenReturn("alice");

        commandProcessor.handleCommand("/bob");

        verify(server).privateMessage("bob", "alice");
    }
}
