package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeFormatter;

import static common.ANSICodes.*;

public class ClientHandler implements  Runnable{
    private String username;
    private Socket clientSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final Server server;
    private Channel channel;
    private CommandProcessor commandProcessor;

    public ClientHandler(Server server, Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
//        this.channel = channel;
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }


    public String username(){ return username; }


    public void updateChannel(Channel channel){
        this.channel.removeMembers(this);
        this.channel = channel;
    }


    public void sendMessage(String message){ printWriter.println(message); }


    public void closeClient() throws IOException{
        clientSocket.close();

        server.removeClient(username);

        String exitNotificationMessage = BOLD.code() + "------------------------------" +
                 "Server: " + username + " has left the chat!" +
                 "------------------------------" + RESET.code() + "\n";

        server.broadcastAll(exitNotificationMessage, username, channel);
    }


    @Override
    public void run() {
        try {
            commandProcessor = new CommandProcessor(server, this);
            String request;
            String[] user_credentials = bufferedReader.readLine().toLowerCase().split(" ");

            if (user_credentials[0].equals("logIn")) printWriter.println(server.logIn(user_credentials));
            else printWriter.println(server.signUp(user_credentials));

            username = user_credentials[1];

            commandProcessor.handleCommand("/users");
            commandProcessor.handleCommand("/rooms");

            server.addClient(username, this);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            while ((request = bufferedReader.readLine()) != null){

                if (!request.startsWith("/")) {
                    String message = ITALICS.code() + "[" + LocalTime.now().format(timeFormatter) + "] " + RESET.code() +
                            username + ": " + request;
                    server.broadcastAll(message, username, channel);
                }
                else{
                    commandProcessor.handleCommand(request);
                }
            }

        } catch (IOException e) {
            System.out.println( username + " has disconnected");
        }
    }
}
