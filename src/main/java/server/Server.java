package server;

import java.net.*;
import java.io.*;
import java.util.HashMap;

public class Server {
    private static HashMap<String, ClientHandler> clients;

    public Server(){
        clients = new HashMap<>();
    }

    public synchronized void broadcastMessage(String message, String username){
         for (ClientHandler client: clients.values()){
                if (!client.username().equals(username)) {
                    String newMessage = String.format("%50s", message);
                    client.sendMessage(newMessage);
                }else { client.sendMessage(message); }
         }
    }

    public void addClient(ClientHandler client){ clients.put(client.username(), client); }

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
