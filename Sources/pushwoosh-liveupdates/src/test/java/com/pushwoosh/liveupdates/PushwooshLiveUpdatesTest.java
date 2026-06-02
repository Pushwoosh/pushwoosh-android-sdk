package com.pushwoosh.liveupdates;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.NotificationManager;
import android.content.Context;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.liveupdates.internal.DefaultProgressStyleProvider;
import com.pushwoosh.liveupdates.internal.LiveUpdateNotificationRenderer;

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
    }

    @After
    public void tearDown() {
        PushwooshLiveUpdates.installForTest(null);
        platformMock.close();
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
        renderer.render(a);
        renderer.render(b);

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

        renderer.render(new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("A")
                .build());
        renderer.render(new LiveUpdateState.Builder("order_2", LiveUpdateOperation.START)
                .title("B")
                .build());

        PushwooshLiveUpdates.endAllLiveUpdates();

        assertTrue(PushwooshLiveUpdates.getActiveIds().isEmpty());
    }
}
