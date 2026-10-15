package com.pushwoosh.inbox.network.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.pushwoosh.inbox.data.InboxTabs;
import com.pushwoosh.inbox.internal.data.InboxTabsFactory;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * A tabs payload the SDK cannot read must cost the response its tabs and nothing else: the
 * messages and the deleted list still arrive. Before tabs, any JSONException in this constructor
 * failed the whole getInboxMessages request.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class GetInboxMessagesResponseTabsTest {

    private static final String MESSAGE = "{\"inbox_id\":\"m1\",\"order\":\"10\",\"rt\":\"99999999999\","
            + "\"text\":\"September statement\",\"action_type\":0,\"status\":1,"
            + "\"action_params\":\"{\\\"pw_inbox_tab\\\":\\\"a1b2c3\\\"}\"}";

    private static GetInboxMessagesResponse parse(String tabsField) throws Exception {
        String json = "{\"messages\":[" + MESSAGE + "],\"deleted\":[\"gone\"],\"new_inbox\":1"
                + (tabsField == null ? "" : ",\"inbox_tabs\":" + tabsField) + "}";
        return new GetInboxMessagesResponse(new JSONObject(json));
    }

    @Test
    public void contractPayload_isCacheableTextTheParserReads() throws Exception {
        GetInboxMessagesResponse response =
                parse("{\"all_title\":\"All\",\"tabs\":[{\"id\":\"a1b2c3\",\"title\":\"Bills\"}]}");

        InboxTabs tabs = InboxTabsFactory.fromRawJson(response.getInboxTabsJson());
        assertNotNull(tabs);
        assertEquals("All", tabs.getAllTitle());
        assertEquals("a1b2c3", tabs.getTabs().get(0).getId());
    }

    @Test
    public void noTabsField_isNoTabsAndNoRawJson() throws Exception {
        GetInboxMessagesResponse response = parse(null);

        assertNull(response.getInboxTabsJson());
        assertEquals(1, response.getMessages().size());
    }

    @Test
    public void tabsFieldOfTheWrongType_losesTabsButKeepsMessagesAndDeleted() throws Exception {
        for (String broken : new String[] {"\"a string\"", "[1,2,3]", "null", "17"}) {
            GetInboxMessagesResponse response = parse(broken);

            assertNull("raw tabs must be absent for payload " + broken, response.getInboxTabsJson());
            assertEquals(
                    "messages must survive payload " + broken,
                    1,
                    response.getMessages().size());
            assertEquals(
                    "deleted must survive payload " + broken,
                    1,
                    response.getDeleted().size());
            assertEquals(1, response.getNewInboxCount());
        }
    }

    @Test
    public void tabsObjectWithoutUsableEntries_losesTabsButKeepsRawNull() throws Exception {
        GetInboxMessagesResponse response = parse("{\"all_title\":\"All\",\"tabs\":[]}");

        assertNotNull("an object is still cacheable raw text", response.getInboxTabsJson());
        assertNull(InboxTabsFactory.fromRawJson(response.getInboxTabsJson()));
        assertEquals(1, response.getMessages().size());
    }
}
