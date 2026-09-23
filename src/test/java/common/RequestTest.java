package common;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void formRequestCarriesTheSessionIdAndData() {
        JSONObject request = Request.formRequest("session-123", "hello world");

        assertEquals("session-123", Request.getSessionId(request));
        assertEquals("hello world", Request.getData(request));
    }

    @Test
    void formRequestSupportsCommandStrings() {
        JSONObject request = Request.formRequest("session-123", "/join general");

        assertEquals("/join general", Request.getData(request));
    }

    @Test
    void formAuthenticationRequestNestsCredentialsUnderData() {
        JSONObject request = Request.formAuthenticationRequest("LogIn", "alice", "hunter2");

        JSONObject data = Request.getAuthenticationData(request);

        assertEquals("LogIn", data.getString("command"));
        assertEquals("alice", data.getString("username"));
        assertEquals("hunter2", data.getString("password"));
    }

    @Test
    void authenticationRequestHasNoSessionIdField() {
        JSONObject request = Request.formAuthenticationRequest("SignUp", "bob", "pw");

        assertFalse(request.has("sessionId"));
    }
}
