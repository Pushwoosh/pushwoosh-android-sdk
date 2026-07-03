package com.pushwoosh.internal.richmedia;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

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

/**
 * Regression guard for crash-resourceaction-getinstance-null-inbox.
 *
 * ResourceAction.performRichMediaAction used to dereference PushwooshPlatform.getInstance()
 * without a null check. getInstance() returns the static field PushwooshPlatform.instance,
 * which is null before Builder.build() returns and stays null on a failed init. When the
 * host app taps a RICH_MEDIA inbox message while the SDK is not (fully) initialized, this
 * was an unguarded NPE on the main looper.
 *
 * The fix guards getInstance() and early-returns when null. This test pins the graceful
 * behavior: no exception, and the method short-circuits before reading prefs / touching the
 * controller. getInstance() being null is exactly the state of a failed init that threw after
 * RepositoryModule.init() but before the constructor returned and assigned `instance`.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class ResourceActionTest {

    @Mock
    private NotificationPrefs notificationPrefs;

    @Mock
    private PreferenceIntValue richMediaDelayMs;

    private AutoCloseable mocks;
    private MockedStatic<PushwooshPlatform> pushwooshPlatformStatic;
    private MockedStatic<RepositoryModule> repositoryModuleStatic;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Prefs are stubbed non-null so that, were the old (unfixed) code to reach the prefs
        // chain, it would not crash there — isolating the test to the getInstance() path. After
        // the fix the guard short-circuits before the prefs chain, so these stubs go unused
        // (the test asserts richMediaDelayMs() is never called).
        repositoryModuleStatic = Mockito.mockStatic(RepositoryModule.class);
        repositoryModuleStatic
                .when(RepositoryModule::getNotificationPreferences)
                .thenReturn(notificationPrefs);
        when(notificationPrefs.richMediaDelayMs()).thenReturn(richMediaDelayMs);
        when(richMediaDelayMs.get()).thenReturn(0);

        // Trigger: SDK not (fully) initialized -> getInstance() == null. Pre-fix this was an
        // unguarded NPE; the fix early-returns instead.
        pushwooshPlatformStatic = Mockito.mockStatic(PushwooshPlatform.class);
        pushwooshPlatformStatic.when(PushwooshPlatform::getInstance).thenReturn(null);
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

    /**
     * Non-empty richMedia passes the :49 guard. With getInstance() null, the method must
     * early-return gracefully (no NPE) and short-circuit before reading prefs.
     */
    @Test
    public void performRichMediaAction_sdkNotInitialized_returnsGracefullyWithoutThrowing() {
        // No assertThrows: must not throw. If the NPE regressed, this call would crash the test.
        ResourceAction.performRichMediaAction("{\"richmedia\":\"r-123\"}");

        // The guard returns before the ResourceWrapper build, so prefs are never read.
        verify(notificationPrefs, never()).richMediaDelayMs();
    }
}
