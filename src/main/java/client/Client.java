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

public class Client {
    private String sessionId = null;
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    private static SSLSocket createSocket(int serverPort) throws Exception{
        // 1. Load the truststore file into a KeyStore object
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream fis = Client.class.getClassLoader().getResourceAsStream("client-truststore.p12")) {
            if (fis == null) {
                LOGGER.warning("client-truststore.p12 not found on classpath");
                throw new FileNotFoundException("client-truststore.p12 not found on classpath");
            }
            trustStore.load(fis, "daWorld09".toCharArray());
        }

        // 2. Create a TrustManagerFactory and initialize it with the truststore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); // usually "PKIX"
        tmf.init(trustStore);

        // 3. Build an SSLContext using those trust managers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory factory = sslContext.getSocketFactory();
        return (SSLSocket) factory.createSocket("localhost", serverPort);
    }

    private Client(){}

    public static void screenOutput(){
        System.out.println("1. Log In");
        System.out.println("2. Sign Up");
        System.out.print("Enter the number corresponding with you choice: ");
    }


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


            while (true) {
                screenOutput();

                String num = scanner.nextLine();
                String action;

                if (num.equals("1")) action = "LogIn"; else action = "SignUp";

                System.out.print("Enter your username: ");
                String username = scanner.nextLine();

                System.out.print("Enter your password: ");
                String password = scanner.nextLine();

                data = action + " " + username + " " + password;
                request = Request.formRequest(client.sessionId, data);

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
                    JSONObject sessionIdResponse = new JSONObject(reader.readLine());
                    client.sessionId = Response.getData(sessionIdResponse);
                    break;
                }
            }

            System.out.print("\033[H\033[2J");
            System.out.flush();
            new Thread(new ListenForMessages(reader)).start();


            while (true) {
                data = scanner.nextLine();
                request = Request.formRequest(client.sessionId, data);
                writer.println(request);
                System.out.print("\033[1A\033[2K");
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
