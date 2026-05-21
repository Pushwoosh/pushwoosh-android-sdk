package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Application;
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

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class SilentRichMediaStorageImplTest {

    private static final String VALID_RICH_MEDIA =
            "{\"url\":\"https://richmedia.pushwoosh.com/A/B/CODE1.zip?ts=1\",\"ts\":1}";
    private static final String VALID_RICH_MEDIA_2 =
            "{\"url\":\"https://richmedia.pushwoosh.com/C/D/CODE2.zip?ts=2\",\"ts\":2}";

    private PlatformTestManager platformTestManager;
    private SilentRichMediaStorageImpl storage;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        Application application = RuntimeEnvironment.application;
        storage = new SilentRichMediaStorageImpl(application);
    }

    @After
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
        platformTestManager.tearDown();
    }

    private PushMessage pushMessage(String richMediaJson, String sound) {
        Bundle bundle = new Bundle();
        if (richMediaJson != null) {
            bundle.putString("rm", richMediaJson);
        }
        if (sound != null) {
            bundle.putString("s", sound);
        }
        return new PushMessage(bundle);
    }

    // Verifies that replaceResource persists richMedia + sound and getResourceWrapper returns them.
    @Test
    public void replaceResource_roundTripsRichMediaAndSound() {
        storage.replaceResource(pushMessage(VALID_RICH_MEDIA, "chime"));

        ResourceWrapper wrapper = storage.getResourceWrapper();

        assertNotNull(wrapper);
        assertEquals("chime", wrapper.getSound());
        assertNotNull(wrapper.getResource());
    }

    // Verifies that a second replaceResource call overwrites the previously stored row.
    @Test
    public void replaceResource_calledTwice_keepsOnlyLastPayload() {
        storage.replaceResource(pushMessage(VALID_RICH_MEDIA, "first"));
        storage.replaceResource(pushMessage(VALID_RICH_MEDIA_2, "second"));

        ResourceWrapper wrapper = storage.getResourceWrapper();

        assertNotNull(wrapper);
        assertEquals("second", wrapper.getSound());
    }

    // Verifies that getResourceWrapper has pop semantics: a second read after a successful read returns null.
    @Test
    public void getResourceWrapper_secondCall_returnsNullAfterSuccessfulRead() {
        storage.replaceResource(pushMessage(VALID_RICH_MEDIA, "chime"));

        ResourceWrapper first = storage.getResourceWrapper();
        ResourceWrapper second = storage.getResourceWrapper();

        assertNotNull(first);
        assertNull(second);
    }

    // Verifies that getResourceWrapper returns null when no row has been stored.
    @Test
    public void getResourceWrapper_emptyTable_returnsNull() {
        assertNull(storage.getResourceWrapper());
    }

    // Verifies that replaceResource without richMedia stores sound and returns a wrapper whose Resource is null.
    @Test
    public void replaceResource_missingRichMedia_returnsWrapperWithoutResource() {
        storage.replaceResource(pushMessage(null, "chime"));

        ResourceWrapper wrapper = storage.getResourceWrapper();

        assertNotNull(wrapper);
        assertNull(wrapper.getResource());
        assertEquals("chime", wrapper.getSound());
    }
}
