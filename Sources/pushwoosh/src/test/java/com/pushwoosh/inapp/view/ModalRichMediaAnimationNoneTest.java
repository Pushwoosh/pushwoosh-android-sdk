package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Regression guards for modal rich media with animation type NONE on both paths.
 *
 * <p>NONE has no switch case in {@code ModalRichMediaWindowUtils} and falls to the default branch,
 * which returns a null animator. The present consumer ({@code onPageLoaded()}) used to dereference
 * that null (NPE swallowed as "Failed to show modal rich media", and the in-app view stat lost);
 * the dismiss consumer ({@code close()}) null-guarded the dereference but hid {@code
 * window.dismiss()} inside the animator's onAnimationEnd, so a NONE dismiss never closed the window.
 * Both paths now treat NONE as "act instantly, no animation". Each test below fails if its guard is
 * reverted.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class ModalRichMediaAnimationNoneTest {

    @Mock
    ModalRichMediaWindow window;

    @Mock
    ModalRichmediaConfig config;

    @Mock
    Resource resource;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        lenient().when(config.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Present path: with NONE the present animator is null. onPageLoaded must NOT throw+swallow the
    // NPE (no "Failed to show modal rich media" log) and must STILL post InAppViewEvent (the stat the
    // swallowed NPE used to eat). Reverting the null guard makes this fail.
    @Test
    public void onPageLoaded_withPresentAnimationNONE_doesNotLogFailure_andSendsInAppViewEvent() {
        when(config.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.NONE);

        // onPageLoaded only runs its body when resourceWebView != null; inject a no-op stand-in.
        ResourceWebView webView = mock(ResourceWebView.class);
        WhiteboxHelper.setInternalState(window, "resourceWebView", webView);
        WhiteboxHelper.setInternalState(window, "config", config);
        WhiteboxHelper.setInternalState(window, "resource", resource);

        doCallRealMethod().when(window).onPageLoaded();

        try (MockedStatic<PushwooshPlatform> platformStatic = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<EventBus> eventBus = Mockito.mockStatic(EventBus.class);
                MockedStatic<PWLog> pwLog = Mockito.mockStatic(PWLog.class)) {
            // window is a mock with only onPageLoaded() real, so showAtLocationOrSubscribeToActivityBroughtOnTop
            // is a no-op stub (its real EventBus.subscribe never runs). topActivity == null only makes the real
            // getParentViewAsync invoke its callback synchronously (no decorView.post), so onPageLoaded falls
            // through to the present-animator/setDuration consumer and the sendEvent under test.
            PushwooshPlatform platform = mock(PushwooshPlatform.class);
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platform);
            when(platform.getTopActivity()).thenReturn(null);

            window.onPageLoaded();

            // The present-path NPE must no longer be thrown and swallowed.
            pwLog.verify(
                    () -> PWLog.error(
                            nullable(String.class), eq("Failed to show modal rich media"), any(Throwable.class)),
                    never());
            // The in-app view stat must still fire even with no present animation, carrying the resource.
            ArgumentCaptor<InAppViewEvent> event = ArgumentCaptor.forClass(InAppViewEvent.class);
            eventBus.verify(() -> EventBus.sendEvent(event.capture()));
            assertSame(resource, event.getValue().getResource());
        }
    }

    // Dismiss path: with NONE the dismiss animator is null. close() must still dismiss the window
    // instantly. window.dismiss() lives inside the animator's onAnimationEnd, so removing the
    // else-branch makes a null animator skip it and this fails.
    @Test
    public void close_withDismissAnimationNONE_dismissesWindowInstantly() {
        when(config.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.NONE);

        ResourceWebView webView = mock(ResourceWebView.class);
        WhiteboxHelper.setInternalState(window, "resourceWebView", webView);
        WhiteboxHelper.setInternalState(window, "config", config);

        doCallRealMethod().when(window).close();

        window.close();

        verify(window).dismiss();
    }
}
