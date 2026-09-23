package common;

import org.json.JSONObject;


/**
 * Builds and reads the JSON response shapes sent from server to client.
 *
 * Every response carries a status ("OK" or an error status) alongside the
 * actual payload, so the client can tell at a glance whether a request
 * succeeded before looking at the data itself.
 */
public class Response {

    /**
     * Builds a response to send back to a client.
     *
     * @param status the outcome of the request, typically "OK" or "ERROR"
     * @param data the payload to send back, for example a chat message,
     *             an error string, or a session ID
     * @return a JSON object with "status" and "data" fields
     */
    public static JSONObject formResponse(String status, Object data){
        JSONObject response = new JSONObject();
        response.put("status", status);
        response.put("data", data);
        return response;
    }


    /**
     * Checks whether a response indicates success.
     *
     * @param givenResponse a response built by {@link #formResponse(String, Object)}
     * @return true if the response's status is "OK" (case insensitive), false otherwise
     */
    public static boolean getStatus(JSONObject givenResponse){
        return givenResponse.getString("status").equalsIgnoreCase("OK");
    }


    /**
     * Extracts the payload from a response.
     *
     * @param givenResponse a response built by {@link #formResponse(String, Object)}
     * @return the data field's contents, for example a chat message or error string
     */
    public static String getData(JSONObject givenResponse){
        return givenResponse.getString("data");
    }
}
