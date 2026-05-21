package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class ActionTest {

    // Verifies that a full JSON parses into Action with all getters populated.
    @Test
    public void constructor_fullJson_populatesAllFields() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "ACTIVITY");
        json.put("title", "Open");
        json.put("icon", "ic_action");
        json.put("action", "android.intent.action.VIEW");
        json.put("url", "https://example.com");
        json.put("class", "java.lang.String");
        json.put("extras", new JSONObject().put("k", "v"));

        Action action = new Action(json);

        assertEquals(Action.Type.ACTIVITY, action.getType());
        assertEquals("Open", action.getTitle());
        assertEquals("ic_action", action.getIcon());
        assertEquals("android.intent.action.VIEW", action.getIntentAction());
        assertEquals("https://example.com", action.getUrl());
        assertEquals(String.class, action.getActionClass());
        assertNotNull(action.getExtras());
        assertEquals("v", action.getExtras().getString("k"));
    }

    // Verifies that BROADCAST type is parsed correctly via enum mapping.
    @Test
    public void constructor_broadcastType_parsesType() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "BROADCAST");
        json.put("title", "Broadcast");

        Action action = new Action(json);

        assertEquals(Action.Type.BROADCAST, action.getType());
    }

    // Verifies that SERVICE type is parsed correctly via enum mapping.
    @Test
    public void constructor_serviceType_parsesType() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "SERVICE");
        json.put("title", "Service");

        Action action = new Action(json);

        assertEquals(Action.Type.SERVICE, action.getType());
    }

    // Verifies that only mandatory fields produce empty strings for optionals and null for class/extras.
    @Test
    public void constructor_onlyMandatoryFields_optionalsAreEmptyOrNull() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "ACTIVITY");
        json.put("title", "Hi");

        Action action = new Action(json);

        assertEquals("", action.getIcon());
        assertEquals("", action.getIntentAction());
        assertEquals("", action.getUrl());
        assertNull(action.getActionClass());
        assertNull(action.getExtras());
    }

    // Verifies that an empty class string skips Class.forName and leaves actionClass null.
    @Test
    public void constructor_emptyClassString_skipsClassResolution() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "ACTIVITY");
        json.put("title", "Hi");
        json.put("class", "");

        Action action = new Action(json);

        assertNull(action.getActionClass());
    }

    // Verifies that an unknown type value is wrapped as JSONException via custom error mapping.
    @Test
    public void constructor_unknownType_throwsJSONException() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "UNKNOWN");
        json.put("title", "Hi");

        assertThrows(JSONException.class, () -> new Action(json));
    }

    // Verifies that missing type field results in JSONException.
    @Test
    public void constructor_missingType_throwsJSONException() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("title", "Hi");

        assertThrows(JSONException.class, () -> new Action(json));
    }

    // Verifies that missing title field results in JSONException from getString.
    @Test
    public void constructor_missingTitle_throwsJSONException() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "ACTIVITY");

        assertThrows(JSONException.class, () -> new Action(json));
    }

    // Verifies silent recovery when class name does not resolve: object is built, actionClass null.
    @Test
    public void constructor_unknownClassName_silentRecoveryAndOtherFieldsKept() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "ACTIVITY");
        json.put("title", "Hi");
        json.put("class", "com.does.not.Exist");
        json.put("icon", "ic");

        Action action = new Action(json);

        assertNull(action.getActionClass());
        assertEquals("Hi", action.getTitle());
        assertEquals("ic", action.getIcon());
    }

    // Verifies that a non-object extras value is silently ignored leaving extras null.
    @Test
    public void constructor_extrasNotObject_silentIgnore() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "ACTIVITY");
        json.put("title", "Hi");
        json.put("extras", "not-an-object");

        Action action = new Action(json);

        assertNull(action.getExtras());
        assertEquals("Hi", action.getTitle());
    }
}
