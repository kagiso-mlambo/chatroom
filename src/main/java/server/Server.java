package server;

import com.google.common.collect.Multimap;
import common.Response;
import database.*;
import org.json.JSONObject;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.*;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static common.ANSICodes.*;
import static java.lang.Math.min;


/**
 * Core server class for the chat application.
 *
 * Responsible for accepting incoming client connections over a TLS socket,
 * managing connected clients and channels in memory, and delegating
 * persistence (users, channels, channel membership, and messages) to the
 * repository classes in the {@code database} package.
 *
 * A single {@code Server} instance is created in {@link #main(String[])},
 * which then spins up a new {@link ClientHandler} thread per accepted
 * connection.
 */
public class Server {
    private ConcurrentHashMap<String, ClientHandler> clients;
    private ConcurrentHashMap<String, Channel> channels;
    private UserRepository userRepo;
    private ChannelRepository channelRepo;
    private ChannelMembersRepository channelMembersRepo;
    private  MessagesRepository messagesRepo;
    private final double LOG_IN_TOKENS_LIMIT = 3;
    private final double TOKENS_PER_SECOND = (LOG_IN_TOKENS_LIMIT / 30.0);
    private ConcurrentHashMap<String, TokenBucket> userLogInCounter;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private final int padding = 50;


    /**
     * Constructs the server, initialising in-memory state, ensuring the
     * database schema exists, opening a database connection, and wiring up
     * all repositories that share that connection.
     *
     * @throws SQLException if the database cannot be initialised or the
     *                       connection cannot be established
     */
    public Server() throws SQLException {
        clients = new ConcurrentHashMap<>();
        channels = new ConcurrentHashMap<>();
        userLogInCounter = new ConcurrentHashMap<>();
        DatabaseInitialiser.initialise();
        Connection connection = DatabaseConnection.getConnection();
        userRepo = new UserRepository(connection);
        channelMembersRepo = new ChannelMembersRepository(connection);
        channelRepo = new ChannelRepository(connection, channelMembersRepo, userRepo);
        messagesRepo = new MessagesRepository(connection);
    }


    /**
     * Attempts to log a user in, subject to a token-bucket rate limit that
     * guards against repeated/rapid login attempts.
     *
     * Each user has a bucket that starts with {@link #LOG_IN_TOKENS_LIMIT}
     * tokens and refills over time at {@link #TOKENS_PER_SECOND}. Every
     * login attempt costs one token; if the bucket is empty the attempt is
     * rejected before credentials are even checked.
     *
     * @param user     the username attempting to log in
     * @param password the password supplied for that username
     * @return a message from {@link UserRepository#logIn} describing the
     *         result if the rate limit was not exceeded, otherwise a
     *         rate-limit message
     */
    public String logIn(String user, String password) {
        TokenBucket userTokenBucket;

        if (!userLogInCounter.containsKey(user)) {
            userTokenBucket = new TokenBucket(LOG_IN_TOKENS_LIMIT, Instant.now());
            userLogInCounter.put(user, userTokenBucket);
        } else { userTokenBucket = userLogInCounter.get(user); }

        if (userTokenBucket.tokens() < 1){
            return "Too many login attempt wait before trying again";
        }

        userTokenBucket.deductTokens();
        long elapsedSeconds = Duration.between(userTokenBucket.lastRefillTime(), Instant.now()).getSeconds();
        double tokensEarned = elapsedSeconds * TOKENS_PER_SECOND;
        userTokenBucket.addTokens(tokensEarned);
        userTokenBucket.updateLastRefillTime();

        return userRepo.logIn(user, password);
    }


    /**
     * Registers a new user account.
     *
     * @param user     the desired username
     * @param password the desired password
     * @return a message from {@link UserRepository#signUp} describing the
     *         result (success or the reason for failure)
     */
    public String signUp(String user, String password) {
        return userRepo.signUp(user, password);
    }


    /**
     * Registers a connected client's handler so the server can route
     * messages to it.
     *
     * @param clientName the username associated with the connection
     * @param client     the {@link ClientHandler} managing that connection
     */
    public void addClient(String clientName, ClientHandler client){
        clients.put(clientName, client);
    }


    /**
     * Removes a client from the in-memory registry, typically called when a
     * connection is closed or the client disconnects.
     *
     * @param clientName the username to remove
     */
    public void removeClient(String clientName){
        clients.remove(clientName);
    }


    /**
     * Retrieves the usernames of every registered user.
     *
     * @return a list of all usernames known to the system
     * @throws SQLException if the underlying query fails
     */
    public ArrayList<String> getAllClients() throws SQLException{
        return userRepo.getAllUsers();
    }


    /**
     * Creates a new group channel and adds its creator as the first member.
     *
     * @param channelName the name of the channel to create
     * @param creator     the username of the user creating the channel
     */
    public void addGroupChannel(String channelName, String creator){
        channelRepo.createGroupChannel(channelName, creator);
        int channelID = channelRepo.getChannelID(channelName);
        int userID = userRepo.getUserId(creator);
        channelMembersRepo.addMemberToChannel(channelID, userID);
    }


    /**
     * Deletes a channel on behalf of a user and notifies that user whether
     * the deletion succeeded. Any {@link SQLException} raised during the
     * deletion is logged rather than propagated.
     *
     * @param channelName the name of the channel to delete
     * @param username    the user requesting the deletion (also the
     *                    recipient of the outcome message)
     */
    public void deleteChannel(String channelName, String username) {
        try {
            boolean deleted = channelRepo.deleteChannel(channelName, username);
            if (deleted) clients.get(username).sendMessage("Channel successfully deleted");
            else clients.get(username).sendMessage("Could not delete channel");
        } catch (SQLException e) {
            LOGGER.warning("Server - Method deleteChannel: " + e);
        }
    }


    /**
     * Retrieves the names of every group channel a given user belongs to.
     *
     * @param username the user whose channels should be listed
     * @return a list of channel names the user is a member of
     */
    public ArrayList<String> getAllGroupChannels(String username) {
        int userID = -1;
        userID = userRepo.getUserId(username);
        return channelRepo.getChannels(userID);
    }


    /**
     * Sends a user the message history of a channel, replaying it so that
     * their own past messages appear flush-left and other users' messages
     * appear indented by {@link #padding} spaces.
     *
     * @param channelID the ID of the channel whose history should be shown
     * @param username  the user who should receive the history
     */
    public void displayPreviousMessages(int channelID, String username){
        Multimap<Integer, String> messages = messagesRepo.getLastMessages(channelID);
        ClientHandler client = clients.get(username);
        int userID = userRepo.getUserId(username);
        for (Map.Entry<Integer, String> message : messages.entries()){
            if (!message.getKey().equals(userID)) {
                String newMessage = " ".repeat(padding) + message.getValue();
                client.sendMessage(newMessage);
            } else { client.sendMessage(message.getValue()); }
        }
    }


    /**
     * Moves a user into an existing group channel: records the new
     * membership, updates the client's active channel, replays the
     * channel's message history to them, and broadcasts a join
     * notification to the rest of the channel.
     *
     * Synchronized to avoid race conditions when multiple users join
     * channels concurrently.
     *
     * @param username    the user joining the channel
     * @param channelName the name of the channel to join
     */
    public synchronized void joinNewChannel(String username, String channelName) {
        String gap = "                              ";
        String joinNotificationMessage = BOLD.code() + "\n" + gap + "Server: " + username + " has joined " +
                channelName + gap + "\n" + RESET.code();

        int channelID = channelRepo.getChannelID(channelName);

        if (channelID > 0) {
            int userID = userRepo.getUserId(username);
            channelMembersRepo.addMemberToChannel(channelID, userID);
            clients.get(username).updateChannel(channelName);
            displayPreviousMessages(channelID, username);
            broadcastAll(joinNotificationMessage, username, channelName);

        } else {
            clients.get(username).sendMessage("That group does not exist");
        }
    }


    /**
     * Opens (or resumes) a private, one-on-one channel between two users.
     *
     * The private channel's name is derived deterministically from both
     * usernames (sorted lexicographically and joined with an underscore) so
     * the same channel is reused on repeat calls regardless of who
     * initiates. If the receiver does not exist, the sender is notified
     * instead.
     *
     * Synchronized to avoid race conditions when private channels are
     * created concurrently.
     *
     * @param receiver the username the sender wants to message
     * @param sender   the user initiating the private message
     */
    public synchronized void privateMessage(String receiver, String sender) {
        if (userRepo.checkIfUserExists(receiver)) {
            String channel;
            if (sender.compareTo(receiver) <= 0) {
                channel = sender + "_" + receiver;
            } else {
                channel = receiver + "_" + sender;
            }

            channelRepo.privateChannel(channel, sender, receiver);

            clients.get(sender).updateChannel(channel);
            clients.get(sender).sendMessage("Private Message with " + receiver);
            int channelId = channelRepo.getChannelID(channel);
            displayPreviousMessages(channelId, sender);
        } else {
            clients.get(sender).sendMessage("This user does not exist!");
        }
    }


    /**
     * Broadcasts a message from {@code username} to every member of
     * {@code channel}, persists it, and echoes it back to the sender.
     *
     * Guards against sending with no active channel selected and against
     * sending to a channel the user is not a member of. Messages from other
     * members are indented by {@link #padding} spaces when delivered to a
     * given recipient; the sender's own copy is sent unindented.
     *
     * Synchronized to keep message ordering and persistence consistent
     * when multiple threads broadcast concurrently.
     *
     * @param message  the message text to broadcast
     * @param username the sender's username
     * @param channel  the name of the channel to broadcast to, or
     *                 {@code null} if the user has no active channel
     */
    public synchronized void broadcastAll(String message, String username, String channel){
        if (channel == null){
            message = "Enter a chat to send a message";
            clients.get(username).sendMessage(message);
            return;
        }
        int channelID = channelRepo.getChannelID(channel);
        int userID =  userRepo.getUserId(username);

        if (!channelMembersRepo.doesMemberExistInChannel(userID, channelID)){
            ClientHandler client = clients.get(username);
            client.sendMessage("You are not a member of this group");
            return;
        }

        messagesRepo.addMessage(channelID, userID, message);

        ArrayList<String> members = channelMembersRepo.getAllMembers(channelID, userRepo);

        for(String member : members){
            if (clients.containsKey(member)) {
                ClientHandler client = clients.get(member);
                if (!client.username().equals(username)) {
                    String newMessage = " ".repeat(padding) + message;
                    client.sendMessage(newMessage);
                }
            }
        }
        clients.get(username).sendMessage(message);
    }


    /**
     * Builds a TLS-secured {@link ServerSocket} bound to the given port.
     *
     * The keystore password is read from the {@code KEYSTORE_PASSWORD}
     * environment variable, and the PKCS12 keystore itself
     * ({@code server.p12}) is loaded from the classpath.
     *
     * @param port the TCP port to listen on
     * @return an {@link SSLServerSocket} ready to accept TLS connections
     * @throws Exception if the environment variable is unset, the keystore
     *                    resource cannot be found, or any part of the TLS
     *                    context setup fails
     */
    private static ServerSocket createServerSocket(int port) throws Exception{
        String keystorePassword = System.getenv("KEYSTORE_PASSWORD");

        if (keystorePassword == null) {
            throw new IllegalStateException("KEYSTORE_PASSWORD environment variable is not set");
        }
        char[] password = keystorePassword.toCharArray();

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream ks = Server.class.getClassLoader().getResourceAsStream("server.p12")) {
            if (ks == null) {
                throw new FileNotFoundException("server.p12 not found on classpath");
            }
            keyStore.load(ks, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        return (SSLServerSocket) factory.createServerSocket(port);
    }


    /**
     * Application entry point. Starts the server, opens a TLS listening
     * socket on port 8081, and continuously accepts incoming connections,
     * spawning a new {@link ClientHandler} thread for each one.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if the server or its listening socket cannot be
     *                    initialised
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Server has started.");
        Server server = new Server();
        ServerSocket severSocket = createServerSocket(8081);

        while(true) {
            Socket clientSocket = severSocket.accept();
            ClientHandler clientHandler = new ClientHandler(server, clientSocket);
            new Thread(clientHandler).start();


        }


    }
}
