package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static server.ANSICodes.*;

public class ClientHandler implements  Runnable{
    private String username;
    private Socket clientSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final Server server;
    private Channel channel;

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

        String exitNotificationMessage = BOLD.code() + "------------------------------" +
                 "Server: " + username + " has left the chat!" +
                 "------------------------------" + RESET.code() + "\n";

        server.broadcastAll(exitNotificationMessage, username, channel);
    }


    private void commands(String command) throws IOException{
        HashMap<String, ClientHandler> clients = server.clients();
        HashMap<String, Channel> channels = server.channels();

        String[] args = command.split(" ");
        switch (args[0]){
            case "/quit": { closeClient(); break; }

            case "/users":{
                sendMessage(UNDERLINE.code() + BOLD.code() + "Online Users:" + RESET.code());
                for (ClientHandler client: clients.values()){if (!this.username.equals(client.username())) sendMessage(client.username()); }
                sendMessage("\n");
                break;
            }

            case "/create":{
                server.addChannel(args[1], username);
                sendMessage(BOLD.code() + "You've created the channel \"" + args[1] + "\" use /join to enter" + RESET.code());
                break;
            }

            case "/join":{
                server.joinNewChannel(username, args[1]);
                break;
            }

            case "/delete":{
                if (channels.containsKey(args[1]) && (channels.get(args[1]).creator().equals(username))){
                    channels.remove(args[1]);
                }
            }

            case "/rooms": {
                sendMessage(UNDERLINE.code() + BOLD.code() + "Available Rooms:" + RESET.code());
                for (Channel channel: channels.values()){ if (!clients.containsKey(channel.name())) { sendMessage(channel.name());} }
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
            sendMessage(BOLD.code() + ITALICS.code() + LocalDate.now() + RESET.code() + "\n");

            String joinNotificationMessage = BOLD.code() + "\n" + "------------------------------" +
                    "Server: " + username + " has joined the chat!"+
                    "------------------------------" + "\n" + RESET.code();
            server.broadcastAll(joinNotificationMessage, username, channel);

            server.addClient(username, this);
            server.addChannel(username.toLowerCase(), username);
            channel.addMembers(this);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            while ((request = bufferedReader.readLine()) != null){

                if (!request.startsWith("/")) {
                    String message = ITALICS.code() + "[" + LocalTime.now().format(timeFormatter) + "] " + RESET.code() +
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
