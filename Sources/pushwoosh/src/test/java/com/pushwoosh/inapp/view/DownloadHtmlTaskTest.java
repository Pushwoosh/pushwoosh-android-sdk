package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceJsonObjectValue;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class DownloadHtmlTaskTest {

    private AutoCloseable mocks;
    private MockedStatic<BackgroundExecutor> backgroundExecutor;
    private MockedStatic<InAppModule> inAppModule;
    private NotificationPrefs originalNotificationPrefs;
    private PreferenceJsonObjectValue tagsPref;

    @Mock
    private InAppRepository inAppRepository;

    @Mock
    private DownloadHtmlTask.DownloadListener downloadListener;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Run the pool and main hops inline so execute() completes synchronously.
        backgroundExecutor = Mockito.mockStatic(BackgroundExecutor.class);
        backgroundExecutor
                .when(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });
        backgroundExecutor
                .when(() -> BackgroundExecutor.main(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });

        // Both collaborators are read in the constructor, so they must be in place before the task is built.
        inAppModule = Mockito.mockStatic(InAppModule.class);
        inAppModule.when(InAppModule::getInAppRepository).thenReturn(inAppRepository);

        // RepositoryModule prefs are process-global; snapshot and restore so the tag cache neither leaks
        // into nor is inherited from other tests in the same JVM fork.
        originalNotificationPrefs = RepositoryModule.getNotificationPreferences();
        NotificationPrefs notificationPrefs = mock(NotificationPrefs.class);
        tagsPref = mock(PreferenceJsonObjectValue.class);
        when(notificationPrefs.tags()).thenReturn(tagsPref);
        when(notificationPrefs.isCollectingDeviceOsVersionAllowed()).thenReturn(mock(PreferenceBooleanValue.class));
        when(notificationPrefs.isCollectingDeviceModelAllowed()).thenReturn(mock(PreferenceBooleanValue.class));
        RepositoryModule.setNotificationPreferences(notificationPrefs);
    }

    @After
    public void tearDown() throws Exception {
        RepositoryModule.setNotificationPreferences(originalNotificationPrefs);
        inAppModule.close();
        backgroundExecutor.close();
        mocks.close();
    }

    // Snapshots the resource's tags at the moment it is handed to the mapper: that is the only instant at
    // which they matter, because ResourceMapper reads getTags() to feed the substitution engine. Capturing
    // here (rather than after execute()) also pins the ordering — tags must be set BEFORE mapping.
    private Map<String, String> tagsSeenByMapper() {
        Map<String, String> seen = new HashMap<>();
        when(inAppRepository.mapToHtmlData(any(Resource.class))).thenAnswer(invocation -> {
            seen.putAll(((Resource) invocation.getArgument(0)).getTags());
            return Result.fromData(new HtmlData("code1", "https://example.com/", "<html/>"));
        });
        return seen;
    }

    // Verifies that the cached device tags reach the resource before it is mapped to HTML, with values
    // coerced to String.
    @Test
    public void execute_cachedTags_resourceCarriesTagsAtMapTime() throws Exception {
        when(tagsPref.get()).thenReturn(new JSONObject("{\"UserName\":\"Alexey\",\"Timezone\":25200}"));
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        Map<String, String> seen = tagsSeenByMapper();

        new DownloadHtmlTask(resource, downloadListener).execute();

        assertEquals("Alexey", seen.get("UserName"));
        assertEquals("25200", seen.get("Timezone"));
    }

    // Verifies that a failed cache read leaves the resource with NO tags instead of falling back to the tags
    // that arrived in the push payload. Rich Media used to keep the payload tags in this case only because
    // the catch sat around setTags(); the fallback is deliberately not carried over (spec decision 3), so a
    // broken cache now means placeholder defaults on both paths.
    @Test
    public void execute_brokenTagCache_payloadTagsAreNotUsedAsFallback() {
        when(tagsPref.get()).thenThrow(new RuntimeException("broken cache"));
        Map<String, Object> payloadTags = new HashMap<>();
        payloadTags.put("UserName", "FromPayload");
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, payloadTags, false, 0);
        Map<String, String> seen = tagsSeenByMapper();

        new DownloadHtmlTask(resource, downloadListener).execute();

        verify(inAppRepository).mapToHtmlData(resource);
        assertTrue("payload tags must not survive a failed cache read, got " + seen, seen.isEmpty());
    }
}
