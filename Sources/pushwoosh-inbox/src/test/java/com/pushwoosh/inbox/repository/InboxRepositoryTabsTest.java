package com.pushwoosh.inbox.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.data.InboxTabs;
import com.pushwoosh.inbox.internal.InboxConfig;
import com.pushwoosh.inbox.internal.PushwooshInboxModule;
import com.pushwoosh.inbox.storage.db.DbInboxStorage;
import com.pushwoosh.inbox.storage.db.InboxDbHelper;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * What a consumer of the module sees: the tabs PushwooshInbox would hand out after a sync, after a
 * restart, after a network failure and after a wipe. The repository is assembled for real — real
 * DbInboxStorage, real prefs — and only the network is a seam.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class InboxRepositoryTabsTest {

    private static final String TWO_TABS = "{\"all_title\":\"All\",\"tabs\":["
            + "{\"id\":\"a1b2c3\",\"title\":\"Bills\"},{\"id\":\"d4e5f6\",\"title\":\"Promos\"}]}";
    private static final String ONE_TAB =
            "{\"all_title\":\"Everything\",\"tabs\":[" + "{\"id\":\"zz\",\"title\":\"Receipts\"}]}";
    private static final String MESSAGE = "{\"inbox_id\":\"m1\",\"order\":\"10\",\"rt\":\"99999999999\","
            + "\"text\":\"September statement\",\"action_type\":0,\"status\":1,"
            + "\"action_params\":\"{\\\"pw_inbox_tab\\\":\\\"a1b2c3\\\"}\"}";

    private SharedPreferences prefs;
    private long defaultUpdateTime;

    @Before
    public void setUp() {
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
        prefs = RuntimeEnvironment.getApplication().getSharedPreferences("pwInbox", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();

        RegistrationPrefs registrationPrefs = mock(RegistrationPrefs.class);
        when(registrationPrefs.userId()).thenReturn(new PreferenceStringValue(null, "userId", null));
        RepositoryModule.setRegistrationPreferences(registrationPrefs);

        defaultUpdateTime = InboxConfig.getInboxUpdateTime();
        InboxConfig.setInboxUpdateTime(0);
    }

    @After
    public void tearDown() throws Exception {
        InboxConfig.setInboxUpdateTime(defaultUpdateTime);
        NetworkModule.setRequestManager(null);
        RepositoryModule.setRegistrationPreferences(null);
        EventBus.clearSubscribersMap();
        for (String name : new String[] {"sInboxRepository", "sInboxStorage", "sInboxDbHelper", "sPrefs"}) {
            Field field = PushwooshInboxModule.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    private InboxRepository newRepository(SharedPreferences preferences) {
        return new InboxRepository(
                new DbInboxStorage(new InboxDbHelper(RuntimeEnvironment.getApplication())), preferences);
    }

    private void respondWith(String body) {
        RequestManager requestManager = mock(RequestManager.class);
        when(requestManager.sendRequestSync(any())).thenAnswer(invocation -> {
            PushRequest<?> request = invocation.getArgument(0);
            return Result.fromData(request.parseResponse(new JSONObject(body)));
        });
        NetworkModule.setRequestManager(requestManager);
    }

    private void respondWithNetworkError() {
        RequestManager requestManager = mock(RequestManager.class);
        when(requestManager.sendRequestSync(any())).thenReturn(Result.fromException(new NetworkException("offline")));
        NetworkModule.setRequestManager(requestManager);
    }

    private static String responseWith(String tabsField) {
        return "{\"messages\":[" + MESSAGE + "],\"deleted\":[],\"new_inbox\":1"
                + (tabsField == null ? "" : ",\"inbox_tabs\":" + tabsField) + "}";
    }

    /** The sync runs on the BackgroundExecutor pool and reports back through the main looper. */
    private void sync(InboxRepository repository) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        repository.loadMessages(result -> done.countDown(), null, -1);
        awaitOnMainLooper(done);
    }

    private void awaitOnMainLooper(CountDownLatch done) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (done.getCount() > 0 && System.currentTimeMillis() < deadline) {
            ShadowLooper.shadowMainLooper().idle();
            Thread.sleep(10);
        }
        assertTrue("inbox sync did not finish in time", done.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void successfulResponse_exposesTabsInServerOrder() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);

        sync(repository);

        InboxTabs tabs = repository.getTabs();
        assertNotNull(tabs);
        assertEquals("All", tabs.getAllTitle());
        assertEquals(2, tabs.getTabs().size());
        assertEquals("a1b2c3", tabs.getTabs().get(0).getId());
        assertEquals("d4e5f6", tabs.getTabs().get(1).getId());
    }

    @Test
    public void responseWithoutTabs_isNoTabs() throws Exception {
        respondWith(responseWith(null));
        InboxRepository repository = newRepository(prefs);

        sync(repository);

        assertNull(repository.getTabs());
    }

    /** Review Focus 3: a broken tabs payload costs the tabs, not the messages. */
    @Test
    public void tabsOfTheWrongType_isNoTabsButMessagesStillLoad() throws Exception {
        respondWith(responseWith("\"a string\""));
        InboxRepository repository = newRepository(prefs);

        sync(repository);

        assertNull(repository.getTabs());
        assertEquals(1, repository.loadCachedMessages(null, -1).size());
    }

    @Test
    public void nextSuccessfulResponse_replacesTheWholeList() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);
        sync(repository);

        respondWith(responseWith(ONE_TAB));
        sync(repository);

        InboxTabs tabs = repository.getTabs();
        assertNotNull(tabs);
        assertEquals("Everything", tabs.getAllTitle());
        assertEquals(1, tabs.getTabs().size());
        assertEquals("zz", tabs.getTabs().get(0).getId());
    }

    @Test
    public void responseWithoutTabsAfterTabs_clearsTheList() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);
        sync(repository);

        respondWith(responseWith(null));
        sync(repository);

        assertNull(repository.getTabs());
    }

    @Test
    public void networkError_keepsThePreviousList() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);
        sync(repository);

        respondWithNetworkError();
        sync(repository);

        assertNotNull("an offline sync must not blank the tab bar", repository.getTabs());
        assertEquals(2, repository.getTabs().getTabs().size());
    }

    @Test
    public void freshRepositoryOnTheSamePrefs_restoresTheList() throws Exception {
        respondWith(responseWith(TWO_TABS));
        sync(newRepository(prefs));

        InboxRepository afterRestart = newRepository(prefs);

        InboxTabs tabs = afterRestart.getTabs();
        assertNotNull("the cached list must survive a process restart", tabs);
        assertEquals(2, tabs.getTabs().size());
        assertEquals("Bills", tabs.getTabs().get(0).getTitle());
    }

    @Test
    public void clearAllInboxMessages_dropsTheList() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);
        sync(repository);

        repository.clearAllInboxMessages();

        assertNull(repository.getTabs());
        assertNull("the cache must be dropped too", newRepository(prefs).getTabs());
    }

    /** Review Focus 1: prefs may be unavailable; tabs then live in memory only. */
    @Test
    public void prefsUnavailable_keepsTabsInMemoryWithoutCrashing() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(null);

        sync(repository);

        assertNotNull(repository.getTabs());
        assertEquals(2, repository.getTabs().getTabs().size());
        assertNull(
                "nothing was written, so nothing is restored",
                newRepository(prefs).getTabs());
    }

    @Test
    public void tabsAreVisibleBeforeTheLoadCallbackReturns() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);

        CountDownLatch done = new CountDownLatch(1);
        final InboxTabs[] seenInCallback = new InboxTabs[1];
        repository.loadMessages(
                result -> {
                    seenInCallback[0] = repository.getTabs();
                    done.countDown();
                },
                null,
                -1);
        awaitOnMainLooper(done);

        assertNotNull("the callback must already see the fresh tabs", seenInCallback[0]);
    }

    @Test
    public void throttledSync_keepsThePreviousList() throws Exception {
        InboxConfig.setInboxUpdateTime(TimeUnit.HOURS.toMillis(1));
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);
        sync(repository);

        respondWith(responseWith(null));
        sync(repository);

        assertNotNull(
                "a throttled sync never reached the network, so it must not clear the list", repository.getTabs());
        assertEquals(2, repository.getTabs().getTabs().size());
    }

    @Test
    public void inboxModuleInitWithAnotherAppCode_dropsTheCachedList() throws Exception {
        respondWith(responseWith(TWO_TABS));
        sync(newRepository(prefs));
        prefs.edit().putString("appId", "OLD00-00000").commit();

        PushwooshInboxModule.init(
                new InboxDbHelper(RuntimeEnvironment.getApplication()), AndroidPlatformModule.getPrefsProvider());

        assertNull(
                "tabs of the previous application must not survive an app code change",
                newRepository(prefs).getTabs());
    }

    @Test
    public void messageTabIdSurvivesStorage() throws Exception {
        respondWith(responseWith(TWO_TABS));
        InboxRepository repository = newRepository(prefs);

        sync(repository);

        assertEquals(
                "a1b2c3",
                repository.loadCachedMessages(null, -1).iterator().next().getTabId());
    }
}
