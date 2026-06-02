package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements  Runnable{
    private String username;
    private Socket clientSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final Server server;

    public ClientHandler(Server server, Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String username(){ return username; }

    public void sendMessage(String message){
        printWriter.println(message);
    }

    @Override
    public void run() {
        try {
            String request;
            username = bufferedReader.readLine();
            server.broadcastMessage( username + " has joined the chat!");
            server.addClient(this);

            while ((request = bufferedReader.readLine()) != null){
                server.broadcastMessage(username + ": " + request);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
