package client;

import common.ANSICodes;
import common.Response;
import org.json.JSONObject;

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
            JSONObject response;
            String stringResponse;

            while ((stringResponse = bufferedReader.readLine()) != null) {
                response = new JSONObject(stringResponse);
                System.out.print(ANSICodes.CLEAR.code());
                String data = Response.getData(response);
                System.out.println(data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
