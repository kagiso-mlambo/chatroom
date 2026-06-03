package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientHandler implements  Runnable{
    private String username;
    private Socket clientSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final Server server;
    private Channel channel;


    String BOLD = "\u001B[1m";
    String RESET = "\u001B[0m";
    String ITALICS = "\u001b[3m";
    String UNDERLINE = "\u001B[4m";


    public ClientHandler(Server server, Socket clientSocket, Channel channel) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
        this.channel = channel;
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }


    public String username(){ return username; }


    public void updateChannel(Channel channel){
        this.channel.removeMembers(this);
        this.channel = channel;
    }


    public void sendMessage(String message){ printWriter.println(message); }


    private void closeClient() throws IOException{
        clientSocket.close();

        String exitNotificationMessage = BOLD + "------------------------------" + "\u001B[0m" +
                "\u001B[1m" + "Server: " + username + " has left the chat!" + "\u001B[0m" +
                "\u001B[1m" + "------------------------------" + "\u001B[0m\n";

        server.broadcastAll(exitNotificationMessage, username, channel);
    }


    private void commands(String command) throws IOException{
        HashMap<String, ClientHandler> clients = server.clients();

        switch (command){
            case "/quit": { closeClient(); break; }

            case "/users":{
                sendMessage(UNDERLINE + BOLD + "Online Users:" + RESET);
                for (ClientHandler client: clients.values()){if (!this.username.equals(client.username())) sendMessage(client.username()); }
                sendMessage("\n");
                break;
            }

            default:
            {
                command = command.replace("/", "");
                if (clients.containsKey(command.toLowerCase())){
                    server.privateMessage(command, username);
                    sendMessage("starting a private chat with " + command);
                } else { sendMessage("This user does not exist");}
            }

        }
    }


    @Override
    public void run() {
        try {

            String request;
            username = bufferedReader.readLine().toLowerCase();
            sendMessage(BOLD + ITALICS + LocalDate.now() + RESET + "\n");

            String joinNotificationMessage = BOLD + "\n" + "------------------------------" +
                    "Server: " + username + " has joined the chat!"+
                    "------------------------------" + "\n" + RESET;
            server.broadcastAll(joinNotificationMessage, username, channel);

            server.addClient(username.toLowerCase(), this);
            server.addChannel(username.toLowerCase());
            channel.addMembers(this);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            while ((request = bufferedReader.readLine()) != null){

                if (!request.startsWith("/")) {
                    String message = ITALICS + "[" + LocalTime.now().format(timeFormatter) + "] " + RESET +
                            username + ": " + request;
                    server.broadcastAll(message, username, channel);
                }
                else{
                    commands(request);
                }
            }

        } catch (IOException e) {
            System.out.println( username + " has disconnected");
        }
    }
}
