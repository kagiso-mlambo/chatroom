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

    public Collection<ClientHandler> getAllClients(){
        return clients.values();
    }


    public void addChannel(String channelName, String creator){
        channels.put(channelName, new Channel(channelName, creator));
    }


    public void deleteChannel(String channelName, String username){
        if (channels.containsKey(channelName) && (channels.get(channelName).creator().equals(username))){
            channels.remove(channelName);
        }
    }


    public Channel getAChannel(String channelName){
        return channels.get(channelName);
    }


    public ArrayList<String> getAllChats(){
        ArrayList<String> usersChannels = new ArrayList<>();
        //Get users id
        // Get all group channels user is a member of
        // return an Arraylist of the names

        return new ArrayList<>();
    }

    public synchronized void joinNewChannel(String username, String channelName){
//        Channel channel = channels.get(channelName);
//        ClientHandler member = clients.get(username);
//
//        channel.addMembers(member);
//        member.updateChannel(channel);
//        member.sendMessage("You are now in " + channelName);
//
//        String joinNotificationMessage = BOLD.code() + "\n" + "------------------------------" +
//                "Server: " + username + " has joined the " + channelName +
//                "------------------------------" + "\n" + RESET.code();
//
//        broadcastAll(joinNotificationMessage, username, channel);
    }


    public synchronized void privateMessage(String receiver, String sender) throws SQLException{
        if (userRepo.checkIfUserExists(receiver)){
            String channel;
            if (sender.compareTo(receiver) <= 0) { channel = sender + "_" + receiver; }
            else { channel = receiver + "_" + sender; }

            int channelID = channelRepo.getChannelId(channel, "private", sender, receiver);
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
//            Channel general = server.getAChannel("general");
            ClientHandler clientHandler = new ClientHandler(server, clientSocket);
            new Thread(clientHandler).start();


        }


    }
}
