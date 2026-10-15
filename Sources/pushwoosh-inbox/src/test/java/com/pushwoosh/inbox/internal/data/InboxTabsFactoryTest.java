package com.pushwoosh.inbox.internal.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.pushwoosh.inbox.data.InboxTab;
import com.pushwoosh.inbox.data.InboxTabs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;

/**
 * The tabs list is read by exactly one parser, shared by the network response and the prefs cache.
 * Everything the backend may send that is not a usable list collapses to "no tabs" instead of an
 * exception, because the same JSON travels inside a response that must still deliver its messages.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class InboxTabsFactoryTest {

    private static final String CONTRACT_JSON = "{\"all_title\":\"All\",\"tabs\":["
            + "{\"id\":\"a1b2c3\",\"title\":\"Bills\"},"
            + "{\"id\":\"d4e5f6\",\"title\":\"Promos\"}]}";

    @Test
    public void contractPayload_keepsServerOrderAndAllTitle() {
        InboxTabs tabs = InboxTabsFactory.fromRawJson(CONTRACT_JSON);

        assertNotNull(tabs);
        assertEquals("All", tabs.getAllTitle());
        assertEquals(2, tabs.getTabs().size());
        assertEquals("a1b2c3", tabs.getTabs().get(0).getId());
        assertEquals("Bills", tabs.getTabs().get(0).getTitle());
        assertEquals("d4e5f6", tabs.getTabs().get(1).getId());
        assertEquals("Promos", tabs.getTabs().get(1).getTitle());
    }

    @Test
    public void emptyTabsArray_isNoTabs() {
        assertNull(InboxTabsFactory.fromRawJson("{\"all_title\":\"All\",\"tabs\":[]}"));
    }

    @Test
    public void missingOrBlankAllTitle_isNoTabs() {
        assertNull(InboxTabsFactory.fromRawJson("{\"tabs\":[{\"id\":\"a\",\"title\":\"Bills\"}]}"));
        assertNull(InboxTabsFactory.fromRawJson("{\"all_title\":\"\",\"tabs\":[{\"id\":\"a\",\"title\":\"Bills\"}]}"));
        assertNull(InboxTabsFactory.fromRawJson("{\"all_title\":null,\"tabs\":[{\"id\":\"a\",\"title\":\"Bills\"}]}"));
        assertNull(InboxTabsFactory.fromRawJson("{\"all_title\":7,\"tabs\":[{\"id\":\"a\",\"title\":\"Bills\"}]}"));
    }

    @Test
    public void brokenEntriesAreSkipped_butGoodOnesSurvive() {
        InboxTabs tabs = InboxTabsFactory.fromRawJson("{\"all_title\":\"All\",\"tabs\":["
                + "{\"title\":\"no id\"},"
                + "{\"id\":\"\",\"title\":\"blank id\"},"
                + "{\"id\":\"b\",\"title\":\"\"},"
                + "{\"id\":7,\"title\":\"id is a number\"},"
                + "\"a bare string\","
                + "{\"id\":\"ok\",\"title\":\"Bills\"}]}");

        assertNotNull(tabs);
        assertEquals(1, tabs.getTabs().size());
        assertEquals("ok", tabs.getTabs().get(0).getId());
    }

    @Test
    public void duplicateId_keepsTheFirstOccurrence() {
        InboxTabs tabs = InboxTabsFactory.fromRawJson("{\"all_title\":\"All\",\"tabs\":["
                + "{\"id\":\"dup\",\"title\":\"First\"},"
                + "{\"id\":\"dup\",\"title\":\"Second\"}]}");

        assertNotNull(tabs);
        assertEquals(1, tabs.getTabs().size());
        assertEquals("First", tabs.getTabs().get(0).getTitle());
    }

    @Test
    public void everyEntryBroken_isNoTabs() {
        assertNull(InboxTabsFactory.fromRawJson("{\"all_title\":\"All\",\"tabs\":[{\"title\":\"no id\"}]}"));
    }

    @Test
    public void garbageInput_isNoTabs() {
        assertNull(InboxTabsFactory.fromRawJson(null));
        assertNull(InboxTabsFactory.fromRawJson(""));
        assertNull(InboxTabsFactory.fromRawJson("not json at all"));
        assertNull(InboxTabsFactory.fromRawJson("[1,2,3]"));
        assertNull(InboxTabsFactory.fromRawJson("{\"all_title\":\"All\",\"tabs\":\"not an array\"}"));
        assertNull(InboxTabsFactory.fromRawJson("{}"));
    }

    @Test
    public void returnedListIsImmutable() {
        InboxTabs tabs = InboxTabsFactory.fromRawJson(CONTRACT_JSON);

        assertNotNull(tabs);
        assertThrows(UnsupportedOperationException.class, () -> tabs.getTabs().clear());
    }

    @Test
    public void constructedValueObjectsCompareByContent() {
        assertEquals(
                new InboxTabs("All", Collections.singletonList(new InboxTab("a", "Bills"))),
                new InboxTabs("All", Collections.singletonList(new InboxTab("a", "Bills"))));
    }
}
