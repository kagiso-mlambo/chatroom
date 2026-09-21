package common;

import org.json.JSONObject;

public class Request {
    public static JSONObject formRequest(String sessionId, Object data){
        JSONObject request = new JSONObject();
        request.put("sessionId", sessionId);
        request.put("data", data);
        return request;
    }

    public static JSONObject formAuthenticationRequest(String command, String username, String password){
        JSONObject request = new JSONObject();

        JSONObject data = new JSONObject();
        data.put("command", command);
        data.put("username", username);
        data.put("password", password);

        request.put("data", data);
        return request;
    }

    public static JSONObject getAuthenticationData(JSONObject givenRequest) {
        return givenRequest.getJSONObject("data");
    }

    public static String getSessionId(JSONObject givenRequest){
        return givenRequest.getString("sessionId");
    }

    public static String getData(JSONObject givenRequest){
        return givenRequest.getString("data");
    }
}
