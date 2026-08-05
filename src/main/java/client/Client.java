package client;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.Scanner;

public class Client {
    private static SSLSocket createSocket(int serverPort) throws Exception{
        // 1. Load the truststore file into a KeyStore object
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream("client-truststore.p12")) {
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
    public static void main(String[] args) throws Exception {
        try {
            Scanner scanner = new Scanner(System.in);
            String request;
            SSLSocket clientSocket = createSocket(8081);
            clientSocket.startHandshake();

            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            while (true) {
                System.out.println("1. Log In");
                System.out.println("2. Sign Up");
                System.out.print("Enter the number corresponding with you choice: ");
                String num = scanner.nextLine();
                String action;

                if (num.equals("1")) action = "LogIn";
                else action = "SignUp";

                System.out.print("Enter your username: ");
                String username = scanner.nextLine();
                System.out.print("Enter your password: ");
                String password = scanner.nextLine();

                request = action + " " + username + " " + password;
                writer.println(request);

                String response = null;
                try {
                    response = reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(response);

                if (response.equals("Logged in successfully") || response.equals("Sign up successful!")) {
                    break;
                }
            }

            System.out.print("\033[H\033[2J");
            System.out.flush();
            new Thread(new ListenForMessages(reader)).start();


            while (true) {
                request = scanner.nextLine();
                writer.println(request);
                System.out.print("\033[1A\033[2K");
                if (request.equalsIgnoreCase("/quit")) {
                    System.exit(0);
                }
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
            System.out.println("The Server is currently unavailable");
        }
    }
}
