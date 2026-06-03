package server;

import java.net.*;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static HashMap<String, ClientHandler> clients;

    public Server(){
        clients = new HashMap<>();
    }

    public void addClient(ClientHandler client){ clients.put(client.username(), client); }

    public Map<String, ClientHandler> clients() { return Collections.unmodifiableMap(clients); }

    public synchronized void broadcastMessage(String message, String username){
         for (ClientHandler client: clients.values()){
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
            ClientHandler clientHandler = new ClientHandler(server, clientSocket);
            new Thread(clientHandler).start();


        }


    }
}
