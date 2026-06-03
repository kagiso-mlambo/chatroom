package client;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            String request;

            System.out.println("Please enter your username: ");
            request = scanner.nextLine();
            System.out.print("\033[H\033[2J");
            System.out.flush();

            Socket clientSocket = new Socket("localhost", 8081);

            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            new Thread(new ListenForMessages(reader)).start();

            writer.println(request);

            while (request != null) {
                request = scanner.nextLine();
                writer.println(request);
                System.out.print("\033[1A\033[2K");
                if (request.equalsIgnoreCase("quit")) {
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            System.out.println("The Server is currently unavailable");
        }
    }


}
