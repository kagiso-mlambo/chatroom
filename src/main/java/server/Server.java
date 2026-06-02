package server;

import java.net.*;
import java.io.*;

public class Server {

    static void main(String[] args) throws IOException{
        ServerSocket severSocket = new ServerSocket(8081);

        while(true) {
            Socket clientSocket = severSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }


    }
}
