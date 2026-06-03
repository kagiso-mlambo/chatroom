package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements  Runnable{
    private String username;
    private Socket clientSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final Server server;

    String BOLD = "\u001B[1m";
    String RESET = "\u001B[0m";
    String ITALICS = "\u001b[3m";

    public ClientHandler(Server server, Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String username(){ return username; }

    public void sendMessage(String message){ printWriter.println(message); }

    private void closeClient() throws IOException{
        clientSocket.close();
        String exitNotificationMessage = BOLD + "------------------------------" + "\u001B[0m" +
                "\u001B[1m" + "Server: " + username + " has left the chat!" + "\u001B[0m" +
                "\u001B[1m" + "------------------------------" + "\u001B[0m\n";
        server.broadcastMessage(exitNotificationMessage, username);
    }

    private void commands(String command){

    }

    @Override
    public void run() {
        try {

            String request;
            username = bufferedReader.readLine();
            sendMessage(BOLD + ITALICS + LocalDate.now() + RESET + "\n");

            String joinNotificationMessage = BOLD + "\n" + "------------------------------" +
                    "Server: " + username + " has joined the chat!"+
                    "------------------------------" + "\n" + RESET;
            server.broadcastMessage(joinNotificationMessage, username);

            server.addClient(this);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            while ((request = bufferedReader.readLine()) != null){

                if (!request.startsWith("/")) {
                    String message = ITALICS + "[" + LocalTime.now().format(timeFormatter) + "] " + RESET +
                            username + ": " + request;
                    server.broadcastMessage(message, username);
                }
                else{
                    closeClient();

                }
            }

        } catch (IOException e) {
            System.out.println( username + " has disconnected");
        }
    }
}
