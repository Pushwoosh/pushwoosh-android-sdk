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

    // Verifies that a non-live push falls through (returns false) and never touches the renderer.
    @Test
    public void testNonLivePushReturnsFalseAndLeavesRendererUntouched() {
        Bundle b = new Bundle();
        b.putString("title", "regular push");

        assertFalse(handler.preHandleMessage(b));
        verifyNoInteractions(renderer);
    }

    // Verifies that a START push is consumed and dispatched to renderer.render.
    @Test
    public void testStartOperationCallsRenderAndReturnsTrue() {
        Bundle b = liveBundle("OPERATION_START", "order_1");

        assertTrue(handler.preHandleMessage(b));
        verify(renderer).render(any());
    }

    // Verifies that an UPDATE push is consumed and dispatched to renderer.render (shares the START
    // branch of the switch — guards against a refactor that splits the fall-through and breaks UPDATE).
    @Test
    public void testUpdateOperationCallsRenderAndReturnsTrue() {
        Bundle b = liveBundle("OPERATION_UPDATE", "order_1");

        assertTrue(handler.preHandleMessage(b));
        verify(renderer).render(any());
    }

    // Verifies that an END push is consumed and dispatched to renderer.dismiss with the activityId.
    @Test
    public void testEndOperationCallsDismissAndReturnsTrue() {
        Bundle b = liveBundle("OPERATION_END", "order_1");

        assertTrue(handler.preHandleMessage(b));
        verify(renderer).dismiss("order_1");
    }

    // Verifies that a malformed live-update payload is consumed (returns true) without rendering.
    @Test
    public void testMalformedPayloadReturnsTrueWithoutRender() {
        Bundle b = new Bundle();
        b.putString("pw_live", "{\"op\":\"OPERATION_START\"}"); // missing id → parse returns null

        assertTrue(handler.preHandleMessage(b));
        verifyNoInteractions(renderer);
    }

    // Verifies that a malformed payload short-circuits BEFORE the renderer provider is queried — the
    // null-state guard returns early instead of falling through to rendererProvider.get(). Uses a mock
    // provider (not the () -> renderer lambda) so get() invocations can be asserted.
    @Test
    public void testMalformedPayloadDoesNotQueryRendererProvider() {
        LiveUpdatePushHandler.RendererProvider provider = mock(LiveUpdatePushHandler.RendererProvider.class);
        when(provider.get()).thenReturn(renderer);
        LiveUpdatePushHandler h = new LiveUpdatePushHandler(provider);

        Bundle b = new Bundle();
        b.putString("pw_live", "{\"op\":\"OPERATION_START\"}"); // missing id → parse returns null

        assertTrue(h.preHandleMessage(b));
        verify(provider, never()).get();
        verifyNoInteractions(renderer);
    }

    // Verifies that an exception from the renderer is swallowed and the push is still consumed.
    @Test
    public void testRendererThrowsIsSwallowedAndReturnsTrue() {
        doThrow(new RuntimeException("boom")).when(renderer).render(any());
        Bundle b = liveBundle("OPERATION_START", "order_1");

        assertTrue(handler.preHandleMessage(b));
    }

    // Verifies that a null renderer triggers the "no renderer installed" warn-log and consumes the
    // push without reaching the render switch.
    @Test
    public void testNullRendererLogsWarnAndConsumesPush() {
        LiveUpdatePushHandler h = new LiveUpdatePushHandler(() -> null);
        Bundle b = liveBundle("OPERATION_START", "order_1");

        try (MockedStatic<PWLog> log = mockStatic(PWLog.class)) {
            assertTrue(h.preHandleMessage(b));
            log.verify(() -> PWLog.warn(eq("LiveUpdatePushHandler"), contains("no renderer")));
        }
    }

    // Verifies the below-API-36 path on Robolectric sdk 35: a valid live push parses and reaches the
    // null-renderer guard without loading any API-36 type, and is consumed (returns true). The
    // null-renderer test above exercises the same guard but only on sdk 36; this closes that gap.
    // Live updates are suppressed below API 36 (all Huawei), so the handler must run safely there —
    // a stray API-36 reference before the guard would VerifyError/NoClassDefFound on a real device.
    @Config(sdk = 35)
    @Test
    public void testApi35ValidLivePushConsumedWithNullRenderer() {
        LiveUpdatePushHandler h = new LiveUpdatePushHandler(() -> null);
        Bundle b = liveBundle("OPERATION_START", "order_1");

        assertTrue(h.preHandleMessage(b));
    }

    private static Bundle liveBundle(String op, String id) {
        Bundle b = new Bundle();
        b.putString("pw_live", "{\"op\":\"" + op + "\",\"id\":\"" + id + "\"}");
        return b;
    }
}
