package common;

import org.json.JSONObject;

public class Response {

    public static JSONObject formResponse(String status, Object data){
        JSONObject response = new JSONObject();
        response.put("status", status);
        response.put("data", data);
        return response;
    }

    public static boolean getStatus(JSONObject givenResponse){
        return givenResponse.getString("status").equalsIgnoreCase("OK");
    }

    public static String getData(JSONObject givenResponse){
        return givenResponse.getString("data");
    }
}
