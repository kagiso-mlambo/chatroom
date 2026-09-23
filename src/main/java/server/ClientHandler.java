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
import java.util.logging.Logger;

import static common.ANSICodes.*;
import static java.lang.Math.min;


/**
 * Handles one connected client's entire lifecycle, from authentication
 * through to disconnect, running on its own thread.
 *
 * Authenticates the client (login or signup), issues and verifies a session
 * ID on every subsequent request, rate limits how fast the client can send
 * messages using a token bucket, and delegates anything starting with "/"
 * to {@link CommandProcessor}. Everything else is treated as a chat message
 * and broadcast to the client's current channel.
 */
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
    private final double TOKENS_PER_SECOND = MESSAGE_TOKENS_LIMIT / 60.0;
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());


    /**
     * Wraps a newly accepted socket, setting up the reader/writer used for
     * the rest of this client's session.
     *
     * @param server the server this client is connected to
     * @param clientSocket the accepted socket for this client
     */
    public ClientHandler(Server server, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.channel = null;

        try {
            printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            LOGGER.warning("ClientHandler - Constructor: " + e.getMessage());
        }
    }


    /**
     * @return this client's authenticated username, or null before login/signup completes
     */
    public String username(){ return username; }


    /**
     * Sets which channel this client is currently active in, so a plain
     * (non command) message from them is broadcast to the right place.
     *
     * @param channel the name of the channel to switch to
     */
    public void updateChannel(String channel){
        this.channel = channel;
    }


    /**
     * Sends a message directly to this client.
     *
     * @param message the text to send
     */
    public void sendMessage(String message){
        JSONObject response = Response.formResponse("OK", message);
        printWriter.println(response);
    }


    /**
     * Disconnects this client: removes them from the server's connected
     * client list and closes the underlying socket.
     */
    public void closeClient() {
        server.removeClient(username);

        try {
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.warning("ClientHandler - Method closeClient: " + e.getMessage());
        }
    }


    /**
     * Checks whether a session ID matches the one issued to this client at
     * login, so a request can be verified as actually coming from them
     * rather than trusting a client supplied username.
     *
     * @param sessionId the session ID to check
     * @return true if it matches this client's session ID, false otherwise
     */
    public boolean validSessionId(String sessionId){
        return this.sessionId.equals(sessionId);
    }


    /**
     * Runs this client's session: first an authentication loop that repeats
     * until login or signup succeeds and a session ID is issued, then a
     * message loop that rate limits, verifies, and processes everything the
     * client sends until they disconnect.
     */
    @Override
    public void run() {
        try {
            commandProcessor = new CommandProcessor(server, this);
            JSONObject request;
            JSONObject response;
            String data;

            JSONObject user_credentials;
            String command;
            String user = "";
            String password;

            while (true) {
                request = new JSONObject(bufferedReader.readLine());
                user_credentials = Request.getAuthenticationData(request);

                command = user_credentials.getString("command").toLowerCase();
                user = user_credentials.getString("username");
                password = user_credentials.getString("password");


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
                }
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
                }
                messageTokenBucket.deductTokens();
                long elapsedSeconds = Duration.between(messageTokenBucket.lastRefillTime(), Instant.now()).getSeconds();
                double tokensEarned = elapsedSeconds * TOKENS_PER_SECOND;
                messageTokenBucket.addTokens(tokensEarned);
                messageTokenBucket.updateLastRefillTime();

                request = new JSONObject(stringRequest);
                String givenSessionId = Request.getSessionId(request);
                data = Request.getData(request);

                if (!validSessionId(givenSessionId) || sessionId == null) {
                    response = Response.formResponse("ERROR", "401 Unauthorized");
                    printWriter.println(response);
                    continue;
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
            LOGGER.warning("ClientHandler - Method run: " + e.getMessage());
        }
    }
}
