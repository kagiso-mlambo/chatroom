package client;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private String username;
    private Socket clientSocket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Client() throws IOException{
        writer = new PrintWriter(clientSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void setClientSocket(Socket clientSocket){ this.clientSocket = clientSocket; }

    public void setUsername(String username){ this.username = username; }

    public String username(){ return username; }

    public PrintWriter writer(){ return writer; }

    public BufferedReader reader(){ return reader; }


    public static void main(String[] args) {
        try {
            Client client = new Client();
            Scanner scanner = new Scanner(System.in);
            String request;

            System.out.println("Please enter your username: ");
            request = scanner.nextLine();
            client.setUsername(request);
            System.out.print("\033[H\033[2J");
            System.out.flush();

            Socket clientSocket = new Socket("localhost", 8081);
            client.setClientSocket(clientSocket);

            PrintWriter writer = client.writer();
            BufferedReader reader = client.reader();
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
