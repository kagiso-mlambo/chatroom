package server;

import common.Request;
import common.Response;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.time.*;
import java.time.format.DateTimeFormatter;

import static common.ANSICodes.*;
import static java.lang.Math.min;

public class ClientHandler implements  Runnable{
    private String username;
    private String sessionId;
    private Socket clientSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private final Server server;
    private String channel;
    private CommandProcessor commandProcessor;
    private final double MESSAGE_TOKENS_LIMIT = 10;

    public ClientHandler(Server server, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.channel = null;

        try {
            printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.out.println("ClientHandler - Constructor: ");
            System.out.println(e);
        }
    }


    public String username(){ return username; }


    public void updateChannel(String channel){
        this.channel = channel;
    }


    public void sendMessage(String message){
        JSONObject response = Response.formResponse("OK", message);
        printWriter.println(response);
    }


    public void closeClient() {
        server.removeClient(username);

        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("ClientHandler - Method closeClient: ");
            System.out.println(e);
        }
    }


    public boolean validSessionId(String sessionId){
        return this.sessionId.equals(sessionId);
    }


    @Override
    public void run() {
        try {
            commandProcessor = new CommandProcessor(server, this);
            JSONObject request;
            JSONObject response;
            String data;

            String[] user_credentials;
            String command;
            String user = "";
            String password;

            int faliedLogins =0;
            while (faliedLogins < 3) {
                request = new JSONObject(bufferedReader.readLine());
                user_credentials = Request.getData(request).split(" ");

                command = user_credentials[0].toLowerCase();
                user = user_credentials[1];
                password = user_credentials[2];


                if (command.equals("login")) {
                    data = server.logIn(user, password);
                    response = Response.formResponse("OK", data);
                } else if (command.equals("signup")) {
                    data = server.signUp(user, password);
                    response = Response.formResponse("OK", data);
                }  else {
                    data = "Invalid choice input please enter 1 or 2";
                    response = Response.formResponse("ERROR", data);
                }

                printWriter.println(response);

                if (data.equals("Logged in successfully") || data.equals("Sign up successful!")) {
                    UUID uuid = UUID.randomUUID();
                    sessionId = uuid.toString();
                    response = Response.formResponse("OK", sessionId);
                    printWriter.println(response);
                    break;
                } else { faliedLogins++; }
            }

            if (faliedLogins == 3){
                response = Response.formResponse("ERROR", "Too many failed please try again later!");
                printWriter.println(response);
                clientSocket.close();
            }

            username = user;
            commandProcessor.handleCommand("/users");
            commandProcessor.handleCommand("/groups");

            server.addClient(username, this);

            TokenBucket messageTokenBucket = new TokenBucket(MESSAGE_TOKENS_LIMIT, Instant.now());

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String stringRequest;
            while ((stringRequest = bufferedReader.readLine()) != null){

                if (messageTokenBucket.tokens() < 1){
                    response = Response.formResponse("ERROR", "Too many request sent to the server");
                    printWriter.println(response);
                    messageTokenBucket.deductTokens();
                }

                long elapsedSeconds = Duration.between(messageTokenBucket.lastRefillTime(), Instant.now()).getSeconds();
                double tokensEarned = elapsedSeconds * (10 / 60);
                messageTokenBucket.addTokens(tokensEarned);
                messageTokenBucket.updateLastRefillTime();

                request = new JSONObject(stringRequest);
                String givenSessionId = Request.getSessionId(request);
                data = Request.getData(request);

                if (!validSessionId(givenSessionId)) {
                    response = Response.formResponse("ERROR", "401 Unauthorized");
                    printWriter.println(response);
                }
                if (!data.startsWith("/")) {
                    String message = ITALICS.code() + "[" + LocalTime.now().format(timeFormatter) + "] " + RESET.code()
                            + username + ": " + data;
                    server.broadcastAll(message, username, channel);
                }
                else{
                    commandProcessor.handleCommand(data);
                }
            }

        } catch (IOException e) {
            System.out.println("ClientHandler - Method run: ");
            System.out.println(e);
        }
    }
}
