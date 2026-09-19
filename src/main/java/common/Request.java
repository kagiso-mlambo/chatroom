package common;

import org.json.JSONObject;

public class Request {
    public static JSONObject formRequest(String sessionId, Object data){
        JSONObject request = new JSONObject();
        request.put("sessionId", sessionId);
        request.put("data", data);
        return request;
    }

    public static String getSessionId(JSONObject givenRequest){
        return givenRequest.getString("sessionId");
    }

    public static String getData(JSONObject givenRequest){
        return givenRequest.getString("data");
    }
}
