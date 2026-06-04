package client;

import common.ANSICodes;

import java.io.BufferedReader;
import java.io.IOException;

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
                System.out.print(ANSICodes.CLEAR.code());
                System.out.println(response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
