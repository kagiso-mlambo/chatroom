package client;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private static final Scanner sc = new Scanner(System.in);
    private static String username;
    private static Socket clientSocket;
    private static PrintWriter printWriter;
    private static BufferedReader bufferedReader;


    public static void main() throws IOException{
        System.out.println("Please enter your username: ");
        username = sc.nextLine();

        clientSocket = new Socket("localhost",8081);
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        new Thread(new ListenForMessages(clientSocket)).start();

        printWriter.println(username);

        String request;
        while (true){
            System.out.println(username + ": ");
            request = sc.nextLine();
            printWriter.println(request);
            if (request.equalsIgnoreCase("quit")) { System.exit(0);}
        }
    }


}
