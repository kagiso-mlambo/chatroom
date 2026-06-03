package server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import static server.ANSICodes.*;

public class Server {
    private HashMap<String, ClientHandler> clients;
    private HashMap<String, Channel> channels;



    public Server(){
        clients = new HashMap<>();
        channels = new HashMap<>();
        Channel mainChannel = new Channel("general", "server");
        channels.put(mainChannel.name(), mainChannel);
    }


    public HashMap<String, ClientHandler> clients() { return clients; }


    public void addClient(String clientName, ClientHandler client){ clients.put(clientName, client); }


    public void removeClient(String clientName){ clients.remove(clientName); }


    public void addChannel(String channelName, String creator){ channels.put(channelName, new Channel(channelName, creator)); }


    public HashMap<String, Channel> channels() { return channels; }


    public synchronized void privateMessage(String receiver, String sender){
        Channel channel = channels.get(receiver);
        ClientHandler member;

        member = clients.get(receiver);
        channel.addMembers(member);
        member.updateChannel(channel);
        member.sendMessage("starting a private chat with " + sender);

        member = clients.get(sender);
        channel.addMembers(member);
        member.updateChannel(channel);
    }


    public synchronized void joinNewChannel(String username, String channelName){
        Channel channel = channels.get(channelName);
        ClientHandler member = clients.get(username);

        channel.addMembers(member);
        member.updateChannel(channel);
        member.sendMessage("You are now in " + channelName);

        String joinNotificationMessage = BOLD.code() + "\n" + "------------------------------" +
                "Server: " + username + " has joined the " + channelName +
                "------------------------------" + "\n" + RESET.code();

        broadcastAll(joinNotificationMessage, username, channel);
    }


    public synchronized void broadcastAll(String message, String username, Channel channel){
        ArrayList<ClientHandler> members = channel.members();
         for (ClientHandler client: members){
                if (!client.username().equals(username)) {
                    String newMessage = " ".repeat(50) + message;
                    client.sendMessage(newMessage);
                }else { client.sendMessage(message); }
         }
    }


    public static void main(String[] args) throws IOException{
        Server server = new Server();
        ServerSocket severSocket = new ServerSocket(8081);

        while(true) {
            Socket clientSocket = severSocket.accept();
            Channel general = server.channels().get("general");
            ClientHandler clientHandler = new ClientHandler(server, clientSocket, general);
            new Thread(clientHandler).start();


        }


    }
}
