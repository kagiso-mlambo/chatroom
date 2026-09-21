package client;

import common.ANSICodes;
import common.Response;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

public class ListenForMessages implements Runnable{
    private BufferedReader bufferedReader;
    private static final Logger LOGGER = Logger.getLogger(ListenForMessages.class.getName());


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
            LOGGER.warning("ListenForMessages: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
