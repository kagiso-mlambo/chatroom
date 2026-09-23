package common;

import org.json.JSONObject;


/**
 * Builds and reads the JSON request shapes sent from client to server.
 *
 * There are two distinct request shapes handled by this class:
 *
 * An authentication request (before login/signup), built by
 * {@link #formAuthenticationRequest(String, String, String)} and read
 * back with {@link #getAuthenticationData(JSONObject)}. This shape has
 * no session ID, since one hasn't been issued yet.
 *
 * An ordinary request (chat messages and commands, sent after a
 * session ID has been issued), built by
 * {@link #formRequest(String, Object)} and read back with
 * {@link #getSessionId(JSONObject)} and {@link #getData(JSONObject)}.
 * The session ID lets the server verify the sender's identity on every
 * request rather than trusting a client supplied username.
 */
public class Request {

    /**
     * Builds an ordinary, post login request: a chat message or a command,
     * tagged with the session ID issued at login so the server can verify
     * who it's actually from.
     *
     * @param sessionId the session ID issued by the server after login/signup
     * @param data the chat message text or {@code /command} string
     * @return a JSON object with {@code sessionId} and {@code data} fields
     */
    public static JSONObject formRequest(String sessionId, Object data){
        JSONObject request = new JSONObject();
        request.put("sessionId", sessionId);
        request.put("data", data);
        return request;
    }


    /**
     * Builds a login or signup request. Sent before any session ID exists,
     * so unlike {@link #formRequest(String, Object)} this carries the raw
     * credentials directly rather than a session ID.
     *
     * @param command either {@code "LogIn"} or {@code "SignUp"} (matched
     *                case insensitively by the server)
     * @param username the username to log in or sign up with
     * @param password the plaintext password, only ever sent over the TLS
     *                 encrypted connection and never stored as-is server side
     * @return a JSON object with a nested {@code data} object containing
     *         {@code command}, {@code username}, and {@code password}
     */
    public static JSONObject formAuthenticationRequest(String command, String username, String password){
        JSONObject request = new JSONObject();

        JSONObject data = new JSONObject();
        data.put("command", command);
        data.put("username", username);
        data.put("password", password);

        request.put("data", data);
        return request;
    }


    /**
     * Extracts the nested credentials object from a login/signup request.
     *
     * @param givenRequest a request built by
     *        {@link #formAuthenticationRequest(String, String, String)}
     * @return the {@code data} object containing {@code command},
     *         {@code username}, and {@code password}
     */
    public static JSONObject getAuthenticationData(JSONObject givenRequest) {
        return givenRequest.getJSONObject("data");
    }


    /**
     * Extracts the session ID from an ordinary request.
     *
     * @param givenRequest a request built by {@link #formRequest(String, Object)}
     * @return the session ID the client attached to the request
     */
    public static String getSessionId(JSONObject givenRequest){
        return givenRequest.getString("sessionId");
    }


    /**
     * Extracts the message/command text from an ordinary request.
     *
     * @param givenRequest a request built by {@link #formRequest(String, Object)}
     * @return the chat message text or {@code /command} string
     */
    public static String getData(JSONObject givenRequest){
        return givenRequest.getString("data");
    }
}
