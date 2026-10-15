package com.pushwoosh.notification;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Observes the notification permission and persists the flags {@link PushPermissionStatus} is derived from.
 */
public final class PushPermissionStatusTracker {
    private static final String TAG = "PushPermissionStatusTracker";
    private static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    private static final int FIRST_OBSERVED_UNKNOWN = -1;

    // the prompt result and onResume fire together; one registerDevice per status is enough
    private static final AtomicReference<String> statusInFlight = new AtomicReference<>();

    private PushPermissionStatusTracker() {}

    public static void register(@NonNull Context context) {
        Application application = (Application) context.getApplicationContext();
        application.registerActivityLifecycleCallbacks(new ResumeCallbacks());
    }

    /**
     * Status for outgoing requests; the rationale needs an Activity, so its last persisted value is used.
     */
    @Nullable public static String getStatus() {
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            return null;
        }
        return observe(context, false);
    }

    /**
     * Records the status the server accepted.
     */
    public static void onStatusDelivered(@Nullable String status) {
        if (status == null) {
            return;
        }
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null) {
            return;
        }
        prefs.pushPermissionLastSent().set(status);
    }

    /**
     * Records the result of the SDK permission prompt and reports a changed status to the server.
     * A denial that is the user's choice stops the SDK from prompting again.
     */
    public static void onPermissionResult(@NonNull Activity activity, boolean granted) {
        if (recordPromptResult(activity, granted, shouldShowRationale(activity))) {
            reportIfChanged();
        }
    }

    // registerDevice, not applicationOpen: the server counts every applicationOpen as an app open.
    private static void reportIfChanged() {
        SdkStateProvider.getInstance().executeOrQueue(PushPermissionStatusTracker::reportStatusIfChanged);
    }

    private static void reportStatusIfChanged() {
        PushwooshPlatform platform = PushwooshPlatform.getInstance();
        if (platform == null) {
            return;
        }
        DeviceRegistrar registrar = platform.deviceRegistrar();
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null) {
            return;
        }
        String lastSent = prefs.pushPermissionLastSent().get();
        String status = getStatus();
        // Nothing sent yet (first start after the SDK update): the cold-start applicationOpen carries the status.
        if (TextUtils.isEmpty(lastSent)
                || status == null
                || status.equals(lastSent)
                || status.equals(statusInFlight.getAndSet(status))) {
            return;
        }
        PWLog.debug(TAG, "Push permission status changed to " + status + ", updating registration");
        registrar.updateRegistrationNow(() -> statusInFlight.compareAndSet(status, null));
    }

    @VisibleForTesting
    static void resetForTesting() {
        statusInFlight.set(null);
    }

    // Same lock as observe(): getStatus() reads these prefs from the network thread.
    private static synchronized boolean recordPromptResult(
            @NonNull Activity activity, boolean granted, boolean rationaleNow) {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null) {
            return false;
        }
        int apiLevel = Build.VERSION.SDK_INT;
        int targetSdk = targetSdk(activity);

        // The SDK prompts only while the permission is missing, so an unknown first observation was "not granted".
        if (prefs.pushPermissionFirstObservedGranted().get() == FIRST_OBSERVED_UNKNOWN) {
            prefs.pushPermissionFirstObservedGranted().set(0);
        }
        boolean deniedByUser = PushPermissionStatus.isDeniedByUser(
                apiLevel,
                targetSdk,
                granted,
                rationaleNow,
                prefs.pushPermissionRationaleSeen().get());
        if (deniedByUser) {
            prefs.hasUserDeniedNotificationPermission().set(true);
        }
        boolean firstObservedGranted =
                prefs.pushPermissionFirstObservedGranted().get() == 1;
        if (PushPermissionStatus.isGrantedViaSdkPrompt(apiLevel, targetSdk, granted, firstObservedGranted)) {
            prefs.pushPermissionGrantedViaSdkPrompt().set(true);
        }

        boolean sdkPromptCounts = targetSdk >= PushPermissionStatus.RUNTIME_PERMISSION_API;
        String status = observe(activity, sdkPromptCounts && rationaleNow);
        PWLog.debug(TAG, "Permission prompt result: " + status + ", denied by user: " + deniedByUser);
        return true;
    }

    @Nullable private static synchronized String observe(@NonNull Context context, boolean rationaleNow) {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null) {
            return null;
        }
        boolean enabled;
        try {
            enabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to read notification permission", e);
            return null;
        }

        if (prefs.pushPermissionFirstObservedGranted().get() == FIRST_OBSERVED_UNKNOWN) {
            prefs.pushPermissionFirstObservedGranted().set(enabled ? 1 : 0);
        }
        if (enabled && !prefs.pushPermissionEverGranted().get()) {
            prefs.pushPermissionEverGranted().set(true);
        }
        if (rationaleNow && !prefs.pushPermissionRationaleSeen().get()) {
            prefs.pushPermissionRationaleSeen().set(true);
        }

        return PushPermissionStatus.compute(
                Build.VERSION.SDK_INT,
                targetSdk(context),
                enabled,
                prefs.pushPermissionGrantedViaSdkPrompt().get(),
                prefs.pushPermissionEverGranted().get(),
                prefs.pushPermissionRationaleSeen().get(),
                rationaleNow);
    }

    private static boolean shouldShowRationale(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < PushPermissionStatus.RUNTIME_PERMISSION_API) {
            return false;
        }
        try {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS);
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to read permission rationale", e);
            return false;
        }
    }

    private static int targetSdk(@NonNull Context context) {
        return context.getApplicationInfo().targetSdkVersion;
    }

    private static class ResumeCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            observe(activity, shouldShowRationale(activity));
            reportIfChanged();
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {}
    }
}
