package server;

import com.google.common.collect.Multimap;
import database.*;

import java.net.*;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static common.ANSICodes.*;

public class Server {
    private HashMap<String, ClientHandler> clients;
    private HashMap<String, Channel> channels;
    private UserRepository userRepo;
    private ChannelRepository channelRepo;
    private ChannelMembersRepository channelMembersRepo;
    private  MessagesRepository messagesRepo;


    public Server() throws SQLException {
        clients = new HashMap<>();
        channels = new HashMap<>();
        DatabaseInitialiser.initialise();
        Connection connection = DatabaseConnection.getConnection();
        userRepo = new UserRepository(connection);
        channelMembersRepo = new ChannelMembersRepository(connection);
        channelRepo = new ChannelRepository(connection, channelMembersRepo, userRepo);
        messagesRepo = new MessagesRepository(connection);
    }


    public String logIn(String[] userCredentials) {
        return userRepo.logIn(userCredentials);
    }


    public String signUp(String[] userCredentials) {
        return userRepo.signUp(userCredentials);
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
            boolean deleted = channelRepo.deleteChannel(channelName);
            if (deleted) clients.get(username).sendMessage("Channel successfully deleted");
            else clients.get(username).sendMessage("Could not delete channel");
        } catch (SQLException e) {
            System.out.println("Server - Method deleteChannel: ");
            System.out.println(e);
        }
    }


    public ArrayList<String> getAllGroupChannels(String username) {
        int userID = -1;
        userID = userRepo.getUserId(username);
        return channelRepo.getChannels(userID);
    }


    public void displayPreviousMessages(int channelID, String username){
        Multimap<Integer, String> messages = messagesRepo.getLastMessages(channelID);
        System.out.println("Previous Messages" + messages);
        ClientHandler client = clients.get(username);
        int userID = userRepo.getUserId(username);
        for (Map.Entry<Integer, String> message : messages.entries()){
            if (!message.getKey().equals(userID)) {
                String newMessage = " ".repeat(50) + message.getValue();
                client.sendMessage(newMessage);
            } else { client.sendMessage(message.getValue()); }
        }
    }


    public synchronized void joinNewChannel(String username, String channelName) {
        String joinNotificationMessage = BOLD.code() + "\n" + "------------------------------" +
                "Server: " + username + " has joined " + channelName +
                "------------------------------" + "\n" + RESET.code();

        int channelID = channelRepo.getChannelID(channelName);

        if (channelID > 0) {
            int userID = userRepo.getUserId(username);
            channelMembersRepo.addMemberToChannel(channelID, userID);
            clients.get(username).updateChannel(channelName);
            displayPreviousMessages(channelID, username);
            broadcastAll(joinNotificationMessage, username, channelName);
        }
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
            System.out.println("Enter a chat to send a message");
            return;
        }
        int channelID = channelRepo.getChannelID(channel);
        int userID =  userRepo.getUserId(username);

        String exitNotificationMessage = BOLD.code() + "------------------------------" +
                "Server: " + username + " has left the chat!" +
                "------------------------------" + RESET.code() + "\n";

        if (!message.equals(exitNotificationMessage)) {
            messagesRepo.addMessage(channelID, userID, message);
        }

        ArrayList<String> members = channelMembersRepo.getAllMembers(channelID, userRepo);


        for(String member : members){
            if (clients.containsKey(member)) {
                ClientHandler client = clients.get(member);
                if (!client.username().equals(username)) {
                    String newMessage = " ".repeat(50) + message;
                    client.sendMessage(newMessage);
                }
            }
        }
        clients.get(username).sendMessage(message);
    }


    public static void main(String[] args) throws IOException, SQLException {
        Server server = new Server();
        ServerSocket severSocket = new ServerSocket(8081);

        while(true) {
            Socket clientSocket = severSocket.accept();
            ClientHandler clientHandler = new ClientHandler(server, clientSocket);
            new Thread(clientHandler).start();


        }


    }
}
