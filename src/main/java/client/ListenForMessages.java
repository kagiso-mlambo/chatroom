package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ListenForMessages implements Runnable{
    private Socket clientSocket;
    private static  BufferedReader bufferedReader;

    public ListenForMessages(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            String response;
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while ((response = bufferedReader.readLine()) != null) {
                System.out.println();
                System.out.println(response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
