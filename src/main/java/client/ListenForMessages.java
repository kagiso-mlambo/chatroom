package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ListenForMessages implements Runnable{
    private BufferedReader bufferedReader;

    public ListenForMessages(BufferedReader bufferedReader){
        this.bufferedReader = bufferedReader;
    }

    @Override
    public void run() {
        try {
            String response;

            while ((response = bufferedReader.readLine()) != null) {
                System.out.print("\r\033[K");
                System.out.println(response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
