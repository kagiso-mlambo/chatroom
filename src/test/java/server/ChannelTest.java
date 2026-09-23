package server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelTest {

    @Test
    void nameAndCreatorReturnConstructorValues() {
        Channel channel = new Channel("general", "alice");

        assertEquals("general", channel.name());
        assertEquals("alice", channel.creator());
    }

    @Test
    void newChannelHasNoMembers() {
        Channel channel = new Channel("general", "alice");

        assertTrue(channel.members().isEmpty());
    }

    @Test
    void addMembersAddsTheGivenClient() {
        Channel channel = new Channel("general", "alice");
        ClientHandler alice = mock(ClientHandler.class);

        channel.addMembers(alice);

        assertEquals(1, channel.members().size());
        assertSame(alice, channel.members().get(0));
    }

    @Test
    void addMembersAppendsRatherThanReplaces() {
        Channel channel = new Channel("general", "alice");
        ClientHandler alice = mock(ClientHandler.class);
        ClientHandler bob = mock(ClientHandler.class);

        channel.addMembers(alice);
        channel.addMembers(bob);

        assertEquals(2, channel.members().size());
    }

    @Test
    void removeMembersRemovesOnlyTheMatchingUsername() {
        Channel channel = new Channel("general", "alice");
        ClientHandler alice = mock(ClientHandler.class);
        when(alice.username()).thenReturn("alice");
        ClientHandler bob = mock(ClientHandler.class);
        when(bob.username()).thenReturn("bob");
        channel.addMembers(alice);
        channel.addMembers(bob);

        channel.removeMembers(alice);

        assertEquals(1, channel.members().size());
        assertSame(bob, channel.members().get(0));
    }

    @Test
    void removeMembersIsANoOpWhenTheUsernameIsNotAMember() {
        Channel channel = new Channel("general", "alice");
        ClientHandler alice = mock(ClientHandler.class);
        when(alice.username()).thenReturn("alice");
        channel.addMembers(alice);

        ClientHandler stranger = mock(ClientHandler.class);
        when(stranger.username()).thenReturn("stranger");
        channel.removeMembers(stranger);

        assertEquals(1, channel.members().size());
    }
}
