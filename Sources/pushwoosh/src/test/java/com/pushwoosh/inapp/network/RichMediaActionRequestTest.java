package com.pushwoosh.inapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class RichMediaActionRequestTest {

    // Restored from cross-check: pins the wire-level endpoint name. A typo / rename here silently
    // routes to a different backend method, just like the kept getMethod tests in sibling
    // RegisterDeviceRequestTest and RegisterEmailUserRequestTest.
    @Test
    public void getMethod_returnsRichMediaAction() {
        RichMediaActionRequest request = new RichMediaActionRequest("rm", "in", "hash", "{}", 1);

        assertEquals("richMediaAction", request.getMethod());
    }

    // The only subtle contract: empty/null string fields are omitted via TextUtils.isEmpty gate,
    // while action_type is always written verbatim. A refactor that swaps the gated puts for
    // unconditional ones would silently send empty keys to the backend.
    @Test
    public void buildParams_emptyOrNullFields_skipKeysButKeepsActionType() throws Exception {
        // (label, rm, inapp, hash, attrs, expectRm, expectInapp, expectHash, expectAttrs)
        List<Object[]> rows = Arrays.asList(
                new Object[] {"allNull", null, null, null, null, false, false, false, false},
                new Object[] {"allEmpty", "", "", "", "", false, false, false, false},
                new Object[] {"mixNullEmptyNonEmpty", null, "", "hash", "{\"k\":1}", false, false, true, true},
                new Object[] {"onlyRmNonEmpty", "rm", null, "", null, true, false, false, false},
                new Object[] {"onlyInappNonEmpty", null, "inapp", null, "", false, true, false, false},
                new Object[] {"onlyHashNonEmpty", "", null, "hash", "", false, false, true, false},
                new Object[] {"onlyAttrsNonEmpty", null, "", null, "{}", false, false, false, true});

        for (Object[] row : rows) {
            String label = (String) row[0];
            String rm = (String) row[1];
            String inapp = (String) row[2];
            String hash = (String) row[3];
            String attrs = (String) row[4];
            boolean expectRm = (Boolean) row[5];
            boolean expectInapp = (Boolean) row[6];
            boolean expectHash = (Boolean) row[7];
            boolean expectAttrs = (Boolean) row[8];

            RichMediaActionRequest request = new RichMediaActionRequest(rm, inapp, hash, attrs, 7);
            JSONObject params = new JSONObject();
            request.buildParams(params);

            assertEquals("case " + label + " rich_media_code presence", expectRm, params.has("rich_media_code"));
            assertEquals("case " + label + " inapp_code presence", expectInapp, params.has("inapp_code"));
            assertEquals("case " + label + " message_hash presence", expectHash, params.has("message_hash"));
            assertEquals("case " + label + " action_attributes presence", expectAttrs, params.has("action_attributes"));

            assertTrue("case " + label + " action_type must always be present", params.has("action_type"));
            assertEquals("case " + label + " action_type value", 7, params.getInt("action_type"));

            if (expectRm) {
                assertEquals("case " + label + " rich_media_code value", rm, params.getString("rich_media_code"));
            } else {
                assertFalse("case " + label + " rich_media_code must be absent", params.has("rich_media_code"));
            }
            if (expectInapp) {
                assertEquals("case " + label + " inapp_code value", inapp, params.getString("inapp_code"));
            } else {
                assertFalse("case " + label + " inapp_code must be absent", params.has("inapp_code"));
            }
            if (expectHash) {
                assertEquals("case " + label + " message_hash value", hash, params.getString("message_hash"));
            } else {
                assertFalse("case " + label + " message_hash must be absent", params.has("message_hash"));
            }
            if (expectAttrs) {
                assertEquals(
                        "case " + label + " action_attributes value", attrs, params.getString("action_attributes"));
            } else {
                assertFalse("case " + label + " action_attributes must be absent", params.has("action_attributes"));
            }
        }
    }
}
