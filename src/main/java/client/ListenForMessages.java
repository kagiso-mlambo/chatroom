package client;

import common.ANSICodes;
import common.Response;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * Background listener that continuously reads incoming server messages and
 * prints them to the terminal.
 *
 * Runs on its own thread (started from {@link Client#main(String[])}) so
 * that incoming chat messages, join notifications, and command responses
 * can be displayed at any time, independently of the main thread, which is
 * busy blocking on user keyboard input.
 */
public class ListenForMessages implements Runnable{
    private BufferedReader bufferedReader;
    private static final Logger LOGGER = Logger.getLogger(ListenForMessages.class.getName());


    /**
     * Creates a listener that reads from the given stream.
     *
     * @param bufferedReader the reader wrapping the client's socket input
     *                       stream, shared with the main thread's connection
     *                       to the server
     */
    public ListenForMessages(BufferedReader bufferedReader){
        this.bufferedReader = bufferedReader;
    }


    /**
     * Reads one server message at a time for as long as the connection stays
     * open, and prints each one to standard output.
     *
     * Each incoming line is parsed as a JSON response, the screen is cleared
     * before printing so old messages don't build up behind new ones, and
     * the message content is extracted with {@link Response#getData(JSONObject)}
     * before being printed. The loop ends, and the underlying
     * {@link RuntimeException} is thrown, when the stream closes or an
     * I/O error occurs, for example if the server disconnects.
     */
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
