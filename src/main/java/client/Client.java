package client;

import common.Request;
import common.Response;
import org.json.JSONObject;
import server.Server;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Entry point for a chat client.
 *
 * Connects to {@link server.Server} over a TLS encrypted socket, handles the
 * login/signup flow, and then spins up a background thread
 * ({@link ListenForMessages}) to print incoming messages while the main
 * thread reads and sends whatever the user types.
 *
 * A session ID issued by the server after a successful login or signup is
 * stored on the instance and attached to every subsequent request so the
 * server can verify the client's identity without trusting a client supplied
 * username on each message.
 */
public class Client {
    private String sessionId = null;
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());


    /**
     * Opens a TLS encrypted socket to the server.
     *
     * Loads a local truststore (a PKCS12 keystore file bundled as a resource)
     * containing the server's self signed certificate, so the handshake
     * succeeds only if the server presents that specific certificate. The
     * truststore's password is read from the {@code TRUSTSTORE_PASSWORD}
     * environment variable rather than being hardcoded, so it never ends up
     * committed to source control.
     *
     * @param serverPort the port the server is listening on
     * @return an {@link SSLSocket} connected to {@code localhost} on the given port
     * @throws Exception if the truststore resource is missing, the
     *         {@code TRUSTSTORE_PASSWORD} environment variable is not set, or
     *         the TLS handshake setup otherwise fails
     */
    private static SSLSocket createSocket(int serverPort) throws Exception{
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream fis = Client.class.getClassLoader().getResourceAsStream("client-truststore.p12")) {
            if (fis == null) {
                LOGGER.warning("client-truststore.p12 not found on classpath");
                throw new FileNotFoundException("client-truststore.p12 not found on classpath");
            }

            String truststorePassword = System.getenv("TRUSTSTORE_PASSWORD");

            if (truststorePassword == null) {
                throw new IllegalStateException("TRUSTSTORE_PASSWORD environment variable is not set");
            }

            trustStore.load(fis, truststorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); // usually "PKIX"
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory factory = sslContext.getSocketFactory();
        return (SSLSocket) factory.createSocket("localhost", serverPort);
    }


    /**
     * Private, no argument constructor. {@code Client} is only ever
     * instantiated internally by {@link #main(String[])} to hold the session
     * ID for the duration of the run; it exposes no public API of its own.
     */
    private Client(){}


    /**
     * Prints the login/signup menu to standard output.
     */
    private static void screenOutput(){
        System.out.println("1. Log In");
        System.out.println("2. Sign Up");
        System.out.print("Enter the number corresponding with you choice: ");
    }


    /**
     * Application entry point.
     *
     * Establishes a TLS connection to the server, repeatedly prompts for
     * login or signup credentials until authentication succeeds and a
     * session ID is received, then starts a background thread to print
     * incoming messages and enters a loop reading the user's input and
     * sending it to the server as chat messages or commands.
     *
     * Typing {@code /quit} (case insensitive) sends the quit command to the
     * server and exits the application.
     *
     * @param args not used
     * @throws Exception if the initial socket/TLS setup fails
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Client Application started");
        Client client = new Client();

        try {
            Scanner scanner = new Scanner(System.in);
            String data;
            JSONObject response;
            JSONObject request;

            SSLSocket clientSocket = createSocket(8081);
            clientSocket.startHandshake();

            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Authentication loop: keep prompting for login/signup until the
            // server confirms success and sends back a session ID.
            while (true) {
                screenOutput();

                String num = scanner.nextLine();
                String action;

                if (num.equals("1")) action = "LogIn"; else action = "SignUp";

                System.out.print("Enter your username: ");
                String username = scanner.nextLine();

                System.out.print("Enter your password: ");
                String password = scanner.nextLine();

                request = Request.formAuthenticationRequest(action, username, password);

                writer.println(request);

                try {
                    response = new JSONObject(reader.readLine());
                } catch (IOException e) {
                    LOGGER.warning("Client: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                String message = Response.getData(response);
                System.out.println(message);

                if (message.equals("Logged in successfully") || message.equals("Sign up successful!")) {
                    // A successful login/signup is followed by a second
                    // server message carrying the session ID to use for
                    // every request from here on.
                    JSONObject sessionIdResponse = new JSONObject(reader.readLine());
                    client.sessionId = Response.getData(sessionIdResponse);
                    break;
                }
            }

            // Clear the terminal (ANSI: move cursor home, then clear screen)
            // before switching from the auth prompts to the live chat view.
            System.out.print("\033[H\033[2J");
            System.out.flush();
            new Thread(new ListenForMessages(reader)).start();

            // Main chat loop: every line typed is sent as either a chat
            // message or a "/command", tagged with the session ID so the
            // server can verify who it's actually coming from.
            while (true) {
                data = scanner.nextLine();
                request = Request.formRequest(client.sessionId, data);
                writer.println(request);
                System.out.print("\033[1A\033[2K"); // move up one line and clear it, so the raw input line doesn't linger on screen
                if (data.equalsIgnoreCase("/quit")) {
                    System.exit(0);
                }
            }
        } catch(Exception e) {
            LOGGER.warning("Client: " + e.getMessage());
            System.out.println("The Server is currently unavailable");
        }
    }
}
