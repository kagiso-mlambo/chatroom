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

    public ClientHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String request;
            username = bufferedReader.readLine();
            System.out.println( username + " has joined the chat!");

            while ((request = bufferedReader.readLine()) != null){
                System.out.println(username + ": " + request);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
