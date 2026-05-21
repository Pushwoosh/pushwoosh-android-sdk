package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class LockScreenMediaStorageImplTest {

    private static final String VALID_RICH_MEDIA =
            "{\"url\":\"https://richmedia.pushwoosh.com/A/B/CODE1.zip?ts=1\",\"ts\":1}";
    private static final String VALID_RICH_MEDIA_2 =
            "{\"url\":\"https://richmedia.pushwoosh.com/C/D/CODE2.zip?ts=2\",\"ts\":2}";

    private PlatformTestManager platformTestManager;
    private LockScreenMediaStorageImpl storage;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        Application application = RuntimeEnvironment.application;
        storage = new LockScreenMediaStorageImpl(application);
    }

    @After
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
        platformTestManager.tearDown();
    }

    private PushMessage pushMessageWithRichMedia(String richMediaJson, String sound) {
        Bundle bundle = new Bundle();
        bundle.putString("rm", richMediaJson);
        bundle.putString("s", sound);
        return new PushMessage(bundle);
    }

    // Verifies that cacheResource round-trips rich-media + sound through SQLite with correct column mapping.
    @Test
    public void cacheResource_roundTripsRichMediaAndSound() {
        storage.cacheResource(pushMessageWithRichMedia(VALID_RICH_MEDIA, "chime"));

        List<ResourceWrapper> cached = storage.getCachedResourcesList();
        assertEquals(1, cached.size());
        ResourceWrapper wrapper = cached.get(0);
        assertEquals("chime", wrapper.getSound());
        assertNotNull(wrapper.getResource());
        assertTrue(wrapper.isLockScreen());
    }

    // Verifies that cacheRemoteUrl round-trips a URI through SQLite.
    @Test
    public void cacheRemoteUrl_roundTripsUri() {
        Uri url = Uri.parse("https://example.com/media.png");
        storage.cacheRemoteUrl(url);

        List<Uri> cached = storage.getCachedRemoteUrls();
        assertEquals(1, cached.size());
        assertEquals("https://example.com/media.png", cached.get(0).toString());
    }

    // Verifies that multiple cacheResource calls accumulate in the resources table.
    @Test
    public void cacheResource_multipleCalls_accumulateRows() {
        storage.cacheResource(pushMessageWithRichMedia(VALID_RICH_MEDIA, "one"));
        storage.cacheResource(pushMessageWithRichMedia(VALID_RICH_MEDIA_2, "two"));

        assertEquals(2, storage.getCachedResourcesList().size());
    }

    // Verifies that clearResources empties only the resources table, not the remote-URLs table.
    @Test
    public void clearResources_clearsOnlyResourcesTable() {
        storage.cacheResource(pushMessageWithRichMedia(VALID_RICH_MEDIA, "chime"));
        storage.cacheRemoteUrl(Uri.parse("https://example.com/media.png"));

        storage.clearResources();

        assertEquals(0, storage.getCachedResourcesList().size());
        assertEquals(1, storage.getCachedRemoteUrls().size());
    }

    // Verifies that clearRemoteUrls empties only the remote-URLs table, not the resources table.
    @Test
    public void clearRemoteUrls_clearsOnlyRemoteUrlsTable() {
        storage.cacheResource(pushMessageWithRichMedia(VALID_RICH_MEDIA, "chime"));
        storage.cacheRemoteUrl(Uri.parse("https://example.com/media.png"));

        storage.clearRemoteUrls();

        assertEquals(0, storage.getCachedRemoteUrls().size());
        assertEquals(1, storage.getCachedResourcesList().size());
    }

    // Verifies that cacheRemoteUrl with an empty URI is silently skipped (TextUtils.isEmpty early-return).
    @Test
    public void cacheRemoteUrl_withEmptyUri_skipsInsert() {
        storage.cacheRemoteUrl(Uri.EMPTY);

        assertEquals(0, storage.getCachedRemoteUrls().size());
    }

    // Verifies that onUpgrade drops and recreates both tables, wiping prior rows but leaving tables usable.
    @Test
    public void onUpgrade_dropsAndRecreatesBothTables_wipingExistingRows() {
        storage.cacheResource(pushMessageWithRichMedia(VALID_RICH_MEDIA, "chime"));
        storage.cacheRemoteUrl(Uri.parse("https://example.com/media.png"));
        assertEquals(1, storage.getCachedResourcesList().size());
        assertEquals(1, storage.getCachedRemoteUrls().size());

        SQLiteDatabase db = storage.getWritableDatabase();
        storage.onUpgrade(db, 1, 2);

        assertEquals(0, storage.getCachedResourcesList().size());
        assertEquals(0, storage.getCachedRemoteUrls().size());

        storage.cacheResource(pushMessageWithRichMedia(VALID_RICH_MEDIA_2, "fresh"));
        storage.cacheRemoteUrl(Uri.parse("https://example.com/fresh.png"));
        assertEquals(1, storage.getCachedResourcesList().size());
        assertEquals(1, storage.getCachedRemoteUrls().size());
    }
}
