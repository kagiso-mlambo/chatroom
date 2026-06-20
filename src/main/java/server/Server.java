package server;

import client.Client;
import database.*;

import java.net.*;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static common.ANSICodes.*;

public class Server {
    private HashMap<String, ClientHandler> clients;
    private HashMap<String, Channel> channels;
    private UserRepository userRepo;
    private ChannelRepository channelRepo;
    private ChannelMembersRepository channelMembersRepo;



    public Server() throws SQLException {
        clients = new HashMap<>();
        channels = new HashMap<>();
        DatabaseInitialiser.initialise();
        Connection connection = DatabaseConnection.getConnection();
        userRepo = new UserRepository(connection);
        channelMembersRepo = new ChannelMembersRepository(connection);
        channelRepo = new ChannelRepository(connection, channelMembersRepo, userRepo);
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


    public Channel getAChannel(String channelName){
        return channels.get(channelName);
    }


    public ArrayList<String> getAllGroupChannels(String username) {
        int userID = -1;
        userID = userRepo.getUserId(username);
        return channelRepo.getUsersChannels(userID);
    }

    public synchronized void joinNewChannel(String username, String channelName) {
        String joinNotificationMessage = BOLD.code() + "\n" + "------------------------------" +
                "Server: " + username + " has joined the " + channelName +
                "------------------------------" + "\n" + RESET.code();

        int channelID = channelRepo.getChannelID(channelName);

        if (channelID > 0) {
            int userID = userRepo.getUserId(username);
            channelMembersRepo.addMemberToChannel(channelID, userID);
            clients.get(username).updateChannel(channelName);
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
        } else {
            clients.get(sender).sendMessage("This user does not exist!");
        }
    }


    public synchronized void broadcastAll(String message, String username, String channel){
//        ArrayList<ClientHandler> members = channel.members();
//         for (ClientHandler client: members){
//                if (!client.username().equals(username)) {
//                    String newMessage = " ".repeat(50) + message;
//                    client.sendMessage(newMessage);
//                }else { client.sendMessage(message); }
//         }
        clients.get(username).sendMessage("STILL NEED TO IMPLEMENT BROADCASTING!!!");
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
