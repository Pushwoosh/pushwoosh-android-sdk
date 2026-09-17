package com.pushwoosh.liveupdates;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.liveupdates.internal.DefaultProgressStyleProvider;
import com.pushwoosh.liveupdates.internal.LiveUpdateNotificationRenderer;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(sdk = 36)
public class PushwooshLiveUpdatesTest {

    private MockedStatic<AndroidPlatformModule> platformMock;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        platformMock = mockStatic(AndroidPlatformModule.class);
        platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
        ResourceProvider resourceProvider = mock(ResourceProvider.class);
        when(resourceProvider.getIdentifier(anyString(), anyString())).thenReturn(0);
        platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ManagerProvider managerProvider = mock(ManagerProvider.class);
        when(managerProvider.getNotificationManager()).thenReturn(nm);
        platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
        PushwooshLiveUpdates.installForTest(new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()));

        // PushBundleDataProvider.getHeader() falls back to this when the bundle carries no "header"
        // key, which livePushBundle() doesn't.
        AppInfoProvider appInfoProvider = mock(AppInfoProvider.class);
        when(appInfoProvider.getApplicationLabel()).thenReturn("Test App");
        platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);

        // PushMessage's constructor reads RepositoryModule.getNotificationPreferences().iconBackgroundColor()
        // unconditionally when the bundle carries no "ibc" key; stub it so livePushBundle() doesn't NPE.
        NotificationPrefs prefs = mock(NotificationPrefs.class);
        when(prefs.iconBackgroundColor()).thenReturn(mock(PreferenceIntValue.class));
        RepositoryModule.setNotificationPreferences(prefs);
    }

    @After
    public void tearDown() {
        PushwooshLiveUpdates.installForTest(null);
        RepositoryModule.setNotificationPreferences(null);
        platformMock.close();
    }

    private static Bundle livePushBundle(String id) {
        Bundle bundle = new Bundle();
        bundle.putString("pw_live", "{\"op\":\"OPERATION_START\",\"id\":\"" + id + "\"}");
        return bundle;
    }

    @Test
    public void endLiveUpdate_callsRendererDismiss() {
        LiveUpdateNotificationRenderer renderer = mock(LiveUpdateNotificationRenderer.class);
        PushwooshLiveUpdates.installForTest(renderer);

        PushwooshLiveUpdates.endLiveUpdate("order_99");

        verify(renderer).dismiss("order_99");
    }

    @Test
    public void getActiveIds_returnsTagsOfLiveUpdateNotifications() {
        LiveUpdateNotificationRenderer renderer =
                new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider());
        PushwooshLiveUpdates.installForTest(renderer);

        LiveUpdateState a = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("A")
                .build();
        LiveUpdateState b = new LiveUpdateState.Builder("order_2", LiveUpdateOperation.START)
                .title("B")
                .build();
        renderer.render(a, livePushBundle("order_1"));
        renderer.render(b, livePushBundle("order_2"));

        java.util.List<String> ids = PushwooshLiveUpdates.getActiveIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("order_1"));
        assertTrue(ids.contains("order_2"));
    }

    @Test
    public void endAllLiveUpdates_dismissesAllActiveIds() {
        LiveUpdateNotificationRenderer renderer =
                new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider());
        PushwooshLiveUpdates.installForTest(renderer);

        renderer.render(
                new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                        .title("A")
                        .build(),
                livePushBundle("order_1"));
        renderer.render(
                new LiveUpdateState.Builder("order_2", LiveUpdateOperation.START)
                        .title("B")
                        .build(),
                livePushBundle("order_2"));

        PushwooshLiveUpdates.endAllLiveUpdates();

        assertTrue(PushwooshLiveUpdates.getActiveIds().isEmpty());
    }

    @Test
    public void getLiveUpdateActivityId_returnsIdForLiveUpdatePush() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_live", "{\"op\":\"OPERATION_START\",\"id\":\"order_42\"}");

        assertEquals("order_42", PushwooshLiveUpdates.getLiveUpdateActivityId(new PushMessage(bundle)));
    }

    @Test
    public void getLiveUpdateActivityId_returnsNullForRegularPush() {
        Bundle bundle = new Bundle();
        bundle.putString("header", "Hello");
        bundle.putString("title", "A regular push");

        assertNull(PushwooshLiveUpdates.getLiveUpdateActivityId(new PushMessage(bundle)));
    }

    @Test
    public void getLiveUpdateActivityId_returnsNullForMalformedLiveUpdatePayload() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_live", "{\"op\":\"OPERATION_START\"}");

        assertNull(PushwooshLiveUpdates.getLiveUpdateActivityId(new PushMessage(bundle)));
    }
}
