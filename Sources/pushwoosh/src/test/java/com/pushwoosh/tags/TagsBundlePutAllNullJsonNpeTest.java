package com.pushwoosh.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class TagsBundlePutAllNullJsonNpeTest {

    // Regression guard for crash-tagsbundle-putall-null-json. TagsBundle$Builder.putAll now null-guards
    // its receiver: a null JSONObject is skipped (logged + no-op) instead of crashing at json.keys() on
    // the app thread. Was: assertThrows(NullPointerException) reaching TagsBundle$Builder.putAll; now:
    // graceful skip -- putAll(null) returns the same builder (chaining preserved) and imports nothing.
    // Twin of crash-tagsbundle-putlist-null-list; here the null was the receiver, not a stored value.

    // Verifies that the direct builder entry with a null JSONObject is skipped gracefully (host case:
    // apiResponse.optJSONObject("user_tags") returned null). Chaining continues and nothing is imported.
    @Test
    public void builderPutAll_nullJson_isSkippedGracefully() {
        TagsBundle bundle =
                new TagsBundle.Builder().putString("keep", "yes").putAll(null).build();

        assertNotNull(bundle);
        assertEquals("chaining survives the null-skip", "yes", bundle.getString("keep"));
        assertEquals(
                "null JSON must import nothing beyond prior tags",
                1,
                bundle.getMap().size());
    }

    // Verifies that the public Tags.fromJson helper with a null JSONObject returns an empty bundle
    // instead of crashing on the host thread (host case: Tags.fromJson(apiResponse.optJSONObject(...))).
    @Test
    public void tagsFromJson_nullJson_isSkippedGracefully() {
        TagsBundle bundle = Tags.fromJson(null);

        assertNotNull("fromJson(null) must return an empty bundle, not crash", bundle);
        assertTrue("null JSON imports nothing", bundle.getMap().isEmpty());
    }

    // Boundary control (non-vacuous proof): an EMPTY but non-null JSONObject imports nothing -- putAll
    // iterates zero keys. Distinct from the null case; proves the guard is about json == null, not
    // "putAll is fragile on edge JSONObjects".
    @Test
    public void builderPutAll_emptyJson_doesNotThrow() {
        TagsBundle bundle = new TagsBundle.Builder().putAll(new JSONObject()).build();

        assertNotNull(bundle);
        assertTrue("empty JSON imports nothing", bundle.getMap().isEmpty());
    }

    // Negative control / discriminator: a populated JSONObject imports fine and reads back intact. The
    // necessary condition removed -- only a null receiver triggered the crash; the happy path is untouched.
    @Test
    public void builderPutAll_populatedJson_isImported() throws JSONException {
        JSONObject json = new JSONObject().put("name", "Jane").put("age", 32);

        TagsBundle bundle = new TagsBundle.Builder().putAll(json).build();

        assertEquals("Jane", bundle.getString("name"));
        assertEquals(32, bundle.getInt("age", -1));
    }
}
