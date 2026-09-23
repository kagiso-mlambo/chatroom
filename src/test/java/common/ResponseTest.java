package common;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    void formResponseCarriesStatusAndData() {
        JSONObject response = Response.formResponse("OK", "Logged in successfully");

        assertTrue(Response.getStatus(response));
        assertEquals("Logged in successfully", Response.getData(response));
    }

    @Test
    void getStatusIsCaseInsensitive() {
        JSONObject response = Response.formResponse("ok", "fine");

        assertTrue(Response.getStatus(response));
    }

    @Test
    void getStatusIsFalseForAnythingOtherThanOk() {
        JSONObject response = Response.formResponse("ERROR", "Something went wrong");

        assertFalse(Response.getStatus(response));
    }
}
