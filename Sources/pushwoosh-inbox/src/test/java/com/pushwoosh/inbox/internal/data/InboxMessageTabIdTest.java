package com.pushwoosh.inbox.internal.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.os.Bundle;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * A message carries its tab in pw_inbox_tab, in the same place as the other system payload keys:
 * the root of action_params for a getInboxMessages record, the root of the bundle for a push.
 * Custom data (u / userdata) is marketer space and is never searched.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class InboxMessageTabIdTest {

    private static InboxMessageInternal fromService(String actionParams) throws Exception {
        JSONObject json = new JSONObject()
                .put("inbox_id", "m1")
                .put("order", "10")
                .put("rt", "99999999999")
                .put("text", "September statement")
                .put("action_type", 0)
                .put("status", 1);
        if (actionParams != null) {
            json.put("action_params", actionParams);
        }
        return new InboxMessageInternal.Builder().setJsonObject(json).build();
    }

    private static InboxMessageInternal fromPush(Bundle bundle) {
        return new InboxMessageInternal.Builder().setPushBundle(bundle).build();
    }

    private static Bundle inboxPush() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_inbox", "m1");
        bundle.putString("title", "September statement");
        bundle.putString("inbox_params", "{\"rt\":\"99999999999\"}");
        return bundle;
    }

    @Test
    public void serviceMessage_readsTabFromActionParamsRoot() throws Exception {
        assertEquals(
                "a1b2c3",
                fromService("{\"l\":\"https://x\",\"pw_inbox_tab\":\"a1b2c3\"}").getTabId());
    }

    @Test
    public void pushMessage_readsTabFromBundleRoot() {
        Bundle bundle = inboxPush();
        bundle.putString("pw_inbox_tab", "a1b2c3");

        assertEquals("a1b2c3", fromPush(bundle).getTabId());
    }

    @Test
    public void noKey_isNoTab() throws Exception {
        assertNull(fromService("{\"l\":\"https://x\"}").getTabId());
        assertNull(fromService(null).getTabId());
        assertNull(fromPush(inboxPush()).getTabId());
    }

    @Test
    public void keyOnlyInCustomData_isNoTab() throws Exception {
        assertNull(fromService("{\"u\":{\"pw_inbox_tab\":\"a1b2c3\"}}").getTabId());
        assertNull(fromService("{\"userdata\":{\"pw_inbox_tab\":\"a1b2c3\"}}").getTabId());
    }

    @Test
    public void nonStringValue_isNoTab() throws Exception {
        assertNull(fromService("{\"pw_inbox_tab\":7}").getTabId());
        assertNull(fromService("{\"pw_inbox_tab\":null}").getTabId());
        assertNull(fromService("{\"pw_inbox_tab\":\"\"}").getTabId());
        assertNull(fromService("{\"pw_inbox_tab\":{\"id\":\"a\"}}").getTabId());
    }

    /** Review Focus 2: action_params that is not JSON at all must not throw on the main thread. */
    @Test
    public void actionParamsThatIsNotJson_isNoTab() throws Exception {
        assertNull(fromService("not json at all").getTabId());
        assertNull(fromService("").getTabId());
    }
}
