package client;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            String request;
            Socket clientSocket = new Socket("localhost", 8081);
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            new Thread(new ListenForMessages(reader)).start();

            System.out.println("1. Log In");
            System.out.println("2. Sign Up");
            System.out.print("Enter the number corresponding with you choice: ");
            String num = scanner.nextLine();
            String action;

            if (num.equals("1")) action = "LogIn"; else action = "SignUp";

            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            System.out.print("Enter your password: ");
            String password = scanner.nextLine();

            request = action + " " + username + " " + password;
            writer.println(request);

            System.out.print("\033[H\033[2J");
            System.out.flush();


            while (true) {
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
