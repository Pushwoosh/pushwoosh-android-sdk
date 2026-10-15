package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PushPermissionStatusTrackerTest {
    private PlatformTestManager platformTestManager;
    private MockedStatic<PushwooshPlatform> platformMock;
    private MockedStatic<Pushwoosh> pushwooshMock;
    private DeviceRegistrar deviceRegistrar;
    private Pushwoosh pushwoosh;
    private Application.ActivityLifecycleCallbacks resumeCallbacks;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        SdkStateProvider.getInstance().setReady();
        PushPermissionStatusTracker.resetForTesting();

        deviceRegistrar = mock(DeviceRegistrar.class);
        PushwooshPlatform platform = mock(PushwooshPlatform.class);
        when(platform.deviceRegistrar()).thenReturn(deviceRegistrar);
        platformMock = Mockito.mockStatic(PushwooshPlatform.class);
        platformMock.when(PushwooshPlatform::getInstance).thenReturn(platform);

        pushwoosh = mock(Pushwoosh.class);
        pushwooshMock = Mockito.mockStatic(Pushwoosh.class);
        pushwooshMock.when(Pushwoosh::getInstance).thenReturn(pushwoosh);

        notificationsEnabled(true);
        resumeCallbacks = registerTracker(RuntimeEnvironment.getApplication());
    }

    @After
    public void tearDown() {
        pushwooshMock.close();
        platformMock.close();
        platformTestManager.tearDown();
        PushPermissionStatusTracker.resetForTesting();
    }

    // Verifies that a prompt answer (also a dismissal or a no-UI targetSdk < 33 answer) sends no applicationOpen.
    @Test
    public void promptResultReportsThroughRegistrationNotAppOpen() {
        lastSentStatus(PushPermissionStatus.DENIED);

        PushPermissionStatusTracker.onPermissionResult(newActivity(), true);

        verify(deviceRegistrar).updateRegistrationNow(any(Runnable.class));
        verify(pushwoosh, never()).sendAppOpen();
    }

    // Verifies that a denial after the rationale was shown is the user's choice, so the SDK stops prompting.
    @Test
    public void deniedByUserStopsFurtherPrompts() {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        prefs.pushPermissionRationaleSeen().set(true);
        prefs.hasUserDeniedNotificationPermission().set(false);
        PushPermissionStatusTracker.onPermissionResult(newActivity(), false);

        assertTrue(prefs.hasUserDeniedNotificationPermission().get());
    }

    // Verifies that with targetSdk < 33 a denial, answered by the OS without UI, stops the SDK from prompting again.
    @Test
    public void legacyTargetSdkDenialStopsFurtherPrompts() {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        prefs.pushPermissionRationaleSeen().set(false);
        prefs.hasUserDeniedNotificationPermission().set(false);
        Activity activity = newActivity();
        int targetSdk = activity.getApplicationInfo().targetSdkVersion;
        activity.getApplicationInfo().targetSdkVersion = 32;
        try {
            PushPermissionStatusTracker.onPermissionResult(activity, false);
        } finally {
            activity.getApplicationInfo().targetSdkVersion = targetSdk;
        }

        assertTrue(prefs.hasUserDeniedNotificationPermission().get());
    }

    // Verifies that a prompt dismissed without a choice keeps the SDK allowed to prompt again.
    @Test
    public void dismissedPromptKeepsPromptingAllowed() {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        prefs.pushPermissionRationaleSeen().set(false);
        prefs.hasUserDeniedNotificationPermission().set(false);
        PushPermissionStatusTracker.onPermissionResult(newActivity(), false);

        assertFalse(prefs.hasUserDeniedNotificationPermission().get());
    }

    // Verifies that returning to the app with a status changed in the settings updates the registration once.
    @Test
    public void resumeWithChangedStatusUpdatesRegistrationOnce() {
        lastSentStatus(PushPermissionStatus.DENIED);

        resume();

        verify(deviceRegistrar, times(1)).updateRegistrationNow(any(Runnable.class));
        verify(pushwoosh, never()).sendAppOpen();
    }

    // Verifies that a status the server already has sends nothing.
    @Test
    public void resumeWithUnchangedStatusSendsNothing() {
        lastSentStatus(PushPermissionStatusTracker.getStatus());

        resume();

        verify(deviceRegistrar, never()).updateRegistrationNow(any(Runnable.class));
    }

    // Verifies that the prompt result and the resume after it reporting the same change update the registration once.
    @Test
    public void sameChangeSeenByPromptAndResumeUpdatesOnce() {
        lastSentStatus(PushPermissionStatus.DENIED);

        PushPermissionStatusTracker.onPermissionResult(newActivity(), true);
        resume();

        verify(deviceRegistrar, times(1)).updateRegistrationNow(any(Runnable.class));
    }

    // Verifies that the first start after the SDK update leaves the status to the cold-start applicationOpen.
    @Test
    public void statusNeverSentToServerSendsNothing() {
        lastSentStatus("");

        resume();

        verify(deviceRegistrar, never()).updateRegistrationNow(any(Runnable.class));
    }

    // Verifies that a failed registerDevice keeps the old status, so the next resume sends it again.
    @Test
    public void failedReportIsRetriedOnNextResume() {
        lastSentStatus(PushPermissionStatus.DENIED);
        ArgumentCaptor<Runnable> onDone = ArgumentCaptor.forClass(Runnable.class);

        resume();
        verify(deviceRegistrar).updateRegistrationNow(onDone.capture());
        onDone.getValue().run();
        resume();

        verify(deviceRegistrar, times(2)).updateRegistrationNow(any(Runnable.class));
    }

    // Verifies that a grant through the SDK prompt, the first time the SDK sees the permission, is explicit.
    @Test
    public void grantFromSdkPromptReportsGrantedExplicit() {
        notificationsEnabled(true);

        PushPermissionStatusTracker.onPermissionResult(newActivity(), true);

        assertEquals(PushPermissionStatus.GRANTED_EXPLICIT, PushPermissionStatusTracker.getStatus());
    }

    // Verifies that a grant the SDK prompt did not deliver, e.g. from the settings, stays default.
    @Test
    public void grantOutsideSdkPromptReportsGrantedDefault() {
        notificationsEnabled(false);
        resume();

        notificationsEnabled(true);
        resume();

        assertEquals(PushPermissionStatus.GRANTED_DEFAULT, PushPermissionStatusTracker.getStatus());
    }

    // Verifies that a permission revoked in the settings is denied, not mistaken for one never asked.
    @Test
    public void resumeAfterRevokeInSettingsReportsDenied() {
        notificationsEnabled(true);
        resume();

        notificationsEnabled(false);
        resume();

        assertEquals(PushPermissionStatus.DENIED, PushPermissionStatusTracker.getStatus());
    }

    @Test
    public void onStatusDelivered_recordsLastSent() {
        lastSentStatus("not_determined");

        PushPermissionStatusTracker.onStatusDelivered("granted_explicit");

        assertEquals(
                "granted_explicit",
                RepositoryModule.getRegistrationPreferences()
                        .pushPermissionLastSent()
                        .get());
    }

    @Test
    public void onStatusDelivered_nullLeavesLastSentUnchanged() {
        lastSentStatus("not_determined");

        PushPermissionStatusTracker.onStatusDelivered(null);

        assertEquals(
                "not_determined",
                RepositoryModule.getRegistrationPreferences()
                        .pushPermissionLastSent()
                        .get());
    }

    private static Application.ActivityLifecycleCallbacks registerTracker(Application application) {
        Application spyApplication = Mockito.spy(application);
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> callbacks =
                ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);
        Mockito.doNothing().when(spyApplication).registerActivityLifecycleCallbacks(callbacks.capture());
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(spyApplication);
        PushPermissionStatusTracker.register(context);
        return callbacks.getValue();
    }

    private void resume() {
        resumeCallbacks.onActivityResumed(newActivity());
    }

    private static Activity newActivity() {
        return Robolectric.buildActivity(Activity.class).setup().get();
    }

    private static void notificationsEnabled(boolean enabled) {
        Application application = RuntimeEnvironment.getApplication();
        shadowOf((NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE))
                .setNotificationsEnabled(enabled);
    }

    private static void lastSentStatus(String status) {
        RepositoryModule.getRegistrationPreferences().pushPermissionLastSent().set(status);
    }
}
