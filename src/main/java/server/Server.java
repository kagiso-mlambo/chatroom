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
//        Channel mainChannel = new Channel("general", "server");
//        channels.put(mainChannel.name(), mainChannel);
        DatabaseInitialiser.initialise();
        Connection connection = DatabaseConnection.getConnection();
        userRepo = new UserRepository(connection);
        channelMembersRepo = new ChannelMembersRepository(connection);
        channelRepo = new ChannelRepository(connection, channelMembersRepo, userRepo);
    }

    public String logIn(String[] userCredentials) throws SQLException {
        return userRepo.logIn(userCredentials);
    }

    public String signUp(String[] userCredentials) throws SQLException {
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


    public void addGroupChannel(String channelName, String creator) throws SQLException{
        channelRepo.createGroupChannel(channelName, creator);
    }


    public void deleteChannel(String channelName, String username) throws SQLException{
        boolean deleted = channelRepo.deleteChannel(channelName);
        if (deleted) clients.get(username).sendMessage("Channel successfully deleted");
        else clients.get(username).sendMessage("Could not delete channel");
    }


    public Channel getAChannel(String channelName){
        return channels.get(channelName);
    }


    public ArrayList<String> getAllGroupChannels(String username) throws SQLException{
        ArrayList<String> usersChannels = new ArrayList<>();
        int userID = userRepo.getUserId(username);
        usersChannels = channelRepo.getUsersChannels(userID);

        return usersChannels;
    }

    public synchronized void joinNewChannel(String username, String channelName) throws SQLException{
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


    public synchronized void privateMessage(String receiver, String sender) throws SQLException{
        if (userRepo.checkIfUserExists(receiver)){
            String channel;
            if (sender.compareTo(receiver) <= 0) { channel = sender + "_" + receiver; }
            else { channel = receiver + "_" + sender; }

            channelRepo.PrivateChannel(channel, sender, receiver);

            clients.get(sender).updateChannel(channel);
            clients.get(sender).sendMessage("Private Message with " + receiver);
        } else {clients.get(sender).sendMessage("This user does not exist!");}
    }


    public synchronized void broadcastAll(String message, String username, String channel){
//        ArrayList<ClientHandler> members = channel.members();
//         for (ClientHandler client: members){
//                if (!client.username().equals(username)) {
//                    String newMessage = " ".repeat(50) + message;
//                    client.sendMessage(newMessage);
//                }else { client.sendMessage(message); }
//         }
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
