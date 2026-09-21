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


    public String signUp(String user, String password) {
        return userRepo.signUp(user, password);
    }


    public void addClient(String clientName, ClientHandler client){
        clients.put(clientName, client);
    }


    public void removeClient(String clientName){
        clients.remove(clientName);
    }


    public ArrayList<String> getAllClients() throws SQLException{
        return userRepo.getAllUsers();
    }


    public void addGroupChannel(String channelName, String creator){
        channelRepo.createGroupChannel(channelName, creator);
        int channelID = channelRepo.getChannelID(channelName);
        int userID = userRepo.getUserId(creator);
        channelMembersRepo.addMemberToChannel(channelID, userID);
    }


    public void deleteChannel(String channelName, String username) {
        try {
            boolean deleted = channelRepo.deleteChannel(channelName, username);
            if (deleted) clients.get(username).sendMessage("Channel successfully deleted");
            else clients.get(username).sendMessage("Could not delete channel");
        } catch (SQLException e) {
            LOGGER.warning("Server - Method deleteChannel: " + e);
        }
    }


    public ArrayList<String> getAllGroupChannels(String username) {
        int userID = -1;
        userID = userRepo.getUserId(username);
        return channelRepo.getChannels(userID);
    }


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

        } else { clients.get("username").sendMessage("That group does not exist");}
    }


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


    private static ServerSocket createServerSocket(int port) throws Exception{
      char[] password = "daWorld09".toCharArray();
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
