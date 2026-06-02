package server;

import java.net.*;
import java.io.*;
import java.util.HashMap;

public class Server {
    private static HashMap<String, ClientHandler> clients;

    public Server(){
        clients = new HashMap<>();
    }

    public synchronized void broadcastMessage(String message){
         for (ClientHandler client: clients.values()){
                client.sendMessage(message);
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
