package com.pushwoosh.liveupdates.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

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
public class LiveUpdatePushHandlerTest {

    private LiveUpdateNotificationRenderer renderer;
    private LiveUpdatePushHandler handler;

    @Before
    public void setUp() {
        // LiveUpdateStateParser.parse() reads header/message/largeIcon via PushBundleDataProvider,
        // which falls back to AppInfoProvider when those keys are absent. The platform module
        // must be initialized for unit tests so this fallback works.
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
        renderer = mock(LiveUpdateNotificationRenderer.class);
        handler = new LiveUpdatePushHandler(() -> renderer);
    }

    @Test
    public void nonLivePush_returnsFalse_rendererUntouched() {
        Bundle b = new Bundle();
        b.putString("title", "regular push");

        assertFalse(handler.preHandleMessage(b));
        verifyNoInteractions(renderer);
    }

    @Test
    public void startOperation_callsRender_returnsTrue() {
        Bundle b = liveBundle("start", "order_1");
        assertTrue(handler.preHandleMessage(b));
        verify(renderer).render(any());
    }

    @Test
    public void updateOperation_callsRender_returnsTrue() {
        Bundle b = liveBundle("update", "order_1");
        assertTrue(handler.preHandleMessage(b));
        verify(renderer).render(any());
    }

    @Test
    public void endOperation_callsDismiss_returnsTrue() {
        Bundle b = liveBundle("end", "order_1");
        assertTrue(handler.preHandleMessage(b));
        verify(renderer).dismiss("order_1");
    }

    @Test
    public void malformedPayload_returnsTrue_noRender() {
        Bundle b = new Bundle();
        b.putString("pw_live_op", "start"); // missing pw_live_id
        assertTrue(handler.preHandleMessage(b));
        verifyNoInteractions(renderer);
    }

    @Test
    public void rendererThrows_swallowedAndReturnsTrue() {
        doThrow(new RuntimeException("boom")).when(renderer).render(any());
        Bundle b = liveBundle("start", "order_1");
        assertTrue(handler.preHandleMessage(b));
    }

    // Verifies that a malformed live-update payload short-circuits before touching the renderer
    // provider — i.e. the null-state guard exits early instead of falling through to get().
    @Test
    public void malformedPayload_doesNotCallRendererProvider() {
        LiveUpdatePushHandler.RendererProvider provider = mock(LiveUpdatePushHandler.RendererProvider.class);
        when(provider.get()).thenReturn(renderer);
        LiveUpdatePushHandler h = new LiveUpdatePushHandler(provider);

        Bundle b = new Bundle();
        b.putString("pw_live_op", "start"); // missing pw_live_id — parser returns null

        assertTrue(h.preHandleMessage(b));
        verify(provider, never()).get();
        verifyNoInteractions(renderer);
    }

    // Verifies that a null renderer triggers the "no renderer installed" warn-log and skips the
    // render path entirely — the null-renderer guard must short-circuit before the switch.
    @Test
    public void nullRenderer_logsWarnAndDoesNotInvokeRenderer() {
        LiveUpdatePushHandler h = new LiveUpdatePushHandler(() -> null);
        Bundle b = liveBundle("start", "order_1");

        try (MockedStatic<PWLog> log = mockStatic(PWLog.class)) {
            assertTrue(h.preHandleMessage(b));
            log.verify(() -> PWLog.warn(eq("LiveUpdatePushHandler"), contains("no renderer")));
        }
    }

    // Verifies the below-API-36 path on the OLD SDK classes (Robolectric sdk 35): a valid live push
    // parses and hits the null-renderer guard without touching any API-36 type, and is consumed.
    // The nullRenderer_… test above covers the same branch but only on sdk 36 — this closes that gap.
    @Config(sdk = 35)
    @Test
    public void api35_validLivePush_consumedWithNullRenderer() {
        LiveUpdatePushHandler h = new LiveUpdatePushHandler(() -> null);
        Bundle b = liveBundle("start", "order_1");

        assertTrue(h.preHandleMessage(b));
    }

    private static Bundle liveBundle(String op, String id) {
        Bundle b = new Bundle();
        b.putString("pw_live_op", op);
        b.putString("pw_live_id", id);
        return b;
    }
}
