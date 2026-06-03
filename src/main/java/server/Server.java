package server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static HashMap<String, ClientHandler> clients;
    private static HashMap<String, Channel> channels;


    public Server(){
        clients = new HashMap<>();
        channels = new HashMap<>();
        Channel mainChannel = new Channel("general");
        channels.put(mainChannel.name(), mainChannel);
    }


    public void addClient(String clientName, ClientHandler client){ clients.put(clientName, client); }


    public HashMap<String, ClientHandler> clients() { return clients; }


    public void addChannel(String channelName){ channels.put(channelName, new Channel(channelName)); }


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


    public synchronized void broadcastAll(String message, String username, Channel channel){
        ArrayList<ClientHandler> members = channel.members();
         for (ClientHandler client: members){
                if (!client.username().equals(username)) {
                    String newMessage = " ".repeat(50) + message;
                    client.sendMessage(newMessage);
                }else { client.sendMessage(message); }
         }
    }


    static void main(String[] args) throws IOException{
        Server server = new Server();
        ServerSocket severSocket = new ServerSocket(8081);

        while(true) {
            Socket clientSocket = severSocket.accept();
            Channel general = channels.get("general");
            ClientHandler clientHandler = new ClientHandler(server, clientSocket, general);
            new Thread(clientHandler).start();


        }


    }
}
