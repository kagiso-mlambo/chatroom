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


    static void main(String[] args) throws IOException{
        System.out.println("Please enter your username: ");
        username = sc.nextLine();
        System.out.print("\033[H\033[2J");
        System.out.flush();

        clientSocket = new Socket("localhost",8081);
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        new Thread(new ListenForMessages(bufferedReader)).start();

        printWriter.println(username);

        String request;
        while (true){
            request = sc.nextLine();
            printWriter.println(request);
            System.out.print("\033[1A\033[2K");
            if (request.equalsIgnoreCase("quit")) { System.exit(0);}
        }
    }


}
