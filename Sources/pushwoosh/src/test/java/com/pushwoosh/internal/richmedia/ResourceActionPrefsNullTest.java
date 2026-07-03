package com.pushwoosh.internal.richmedia;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class ResourceActionPrefsNullTest {

    private AutoCloseable mocks;
    private MockedStatic<PushwooshPlatform> pushwooshPlatformStatic;
    private MockedStatic<RepositoryModule> repositoryModuleStatic;
    private RichMediaController richMediaController;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Crash precondition: RepositoryModule not initialized -> getNotificationPreferences()
        // returns null. The fix must treat this as "SDK not ready" and skip the action.
        repositoryModuleStatic = Mockito.mockStatic(RepositoryModule.class);
        repositoryModuleStatic
                .when(RepositoryModule::getNotificationPreferences)
                .thenReturn(null);

        // getInstance() is NON-null and yields a controller. If the early-return is missing the
        // method would proceed and call showResourceWrapper; the regression assertion below
        // verifies the controller is never touched, proving the prefs-null path bailed out early.
        richMediaController = mock(RichMediaController.class);
        PushwooshPlatform platform = mock(PushwooshPlatform.class);
        Mockito.when(platform.getRichMediaController()).thenReturn(richMediaController);
        pushwooshPlatformStatic = Mockito.mockStatic(PushwooshPlatform.class);
        pushwooshPlatformStatic.when(PushwooshPlatform::getInstance).thenReturn(platform);
    }

    @After
    public void tearDown() throws Exception {
        if (pushwooshPlatformStatic != null) {
            pushwooshPlatformStatic.close();
        }
        if (repositoryModuleStatic != null) {
            repositoryModuleStatic.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    // Verifies that null notification prefs (SDK not initialized) cause a graceful early return
    // without showing Rich Media. Regression guard: before the fix this threw NPE at the
    // RepositoryModule.getNotificationPreferences().richMediaDelayMs() deref on the main looper.
    @Test
    public void performRichMediaAction_repositoryNotInitialized_returnsGracefullyWithoutShowing() {
        // No assertThrows: a graceful early-return must not raise. If the deref were still present
        // this call would throw NullPointerException and fail the test.
        ResourceAction.performRichMediaAction("{\"richmedia\":\"r-123\"}");

        // The early-return happens before any ResourceWrapper is built, so the controller is never
        // asked to show anything.
        verify(richMediaController, never()).showResourceWrapper(Mockito.any());
    }
}
