package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;

import static common.ANSICodes.*;

public class ClientHandler implements  Runnable{
    private String username;
    private Socket clientSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final Server server;
    private String channel;
    private CommandProcessor commandProcessor;

    public ClientHandler(Server server, Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
//        this.channel = channel;
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }


    public String username(){ return username; }


    public void updateChannel(String channel){
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
            String[] user_credentials = null;

            while (true) {
                user_credentials = bufferedReader.readLine().split(" ");
                user_credentials[0] = user_credentials[0].toLowerCase();

                String response;
                if (user_credentials[0].equals("login")) response = server.logIn(user_credentials);
                else response = server.signUp(user_credentials);

                printWriter.println(response);

                if (response.equals("Logged in successfully") || response.equals("Sign up successful!")) {
                    break;
                }
            }

            username = user_credentials[1];

            commandProcessor.handleCommand("/users");
            commandProcessor.handleCommand("/rooms");

            server.addClient(username, this);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            request = bufferedReader.readLine();

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

        } catch (IOException | SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
