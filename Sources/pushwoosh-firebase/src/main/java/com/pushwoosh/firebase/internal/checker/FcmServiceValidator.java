package com.pushwoosh.firebase.internal.checker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.pushwoosh.firebase.PushFcmIntentService;
import com.pushwoosh.firebase.PushwooshFcmRouterService;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Diagnoses the FCM service resolution the way ServiceStarter does it: resolveService,
 * limited to own package. Verdict A (info): the winner is foreign and no message has reached
 * Pushwoosh since install/update. A correct manual integration passes through this state too,
 * until its first Pushwoosh message, so it is a notice, not a warning. Verdict B (warn): a
 * declared service lost the resolve and will never receive messages. The firebase-messaging
 * fallback winning (warn): the SDK service was cut from the manifest and the app declares none,
 * so no service receives Pushwoosh messages and nothing clears the state. Same mechanism as
 * ManifestValidator in the core.
 */
public class FcmServiceValidator {
    private static final String TAG = "FcmServiceValidator";
    private static final String ACTION_MESSAGING_EVENT = "com.google.firebase.MESSAGING_EVENT";
    // Declared by firebase-messaging itself with priority -500; present in every FCM app.
    private static final String FCM_FALLBACK_SERVICE = FirebaseMessagingService.class.getName();

    private static final AtomicBoolean validated = new AtomicBoolean(false);

    private FcmServiceValidator() {}

    public static void scheduleValidation() {
        scheduleValidation(BackgroundExecutor::executeOnPool);
    }

    @VisibleForTesting
    static boolean scheduleValidation(Executor executor) {
        if (!validated.compareAndSet(false, true)) {
            return false;
        }
        executor.execute(FcmServiceValidator::runValidationSafely);
        return true;
    }

    private static void runValidationSafely() {
        try {
            validate();
        } catch (Throwable t) {
            PWLog.exception(t);
        }
    }

    @VisibleForTesting
    static void validate() {
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(ACTION_MESSAGING_EVENT).setPackage(context.getPackageName());

        ResolveInfo winner = pm.resolveService(intent, 0);
        if (winner == null || winner.serviceInfo == null || winner.serviceInfo.name == null) {
            return;
        }
        String winnerName = winner.serviceInfo.name;

        reportIfPushwooshUnreachable(winnerName);
        warnAboutLosers(pm, intent, winnerName);
    }

    private static void reportIfPushwooshUnreachable(String winnerName) {
        if (PushFcmIntentService.class.getName().equals(winnerName)) {
            return;
        }
        if (FCM_FALLBACK_SERVICE.equals(winnerName)) {
            PWLog.warn(
                    TAG,
                    "No service in this app receives FCM messages for Pushwoosh: " + ACTION_MESSAGING_EVENT
                            + " resolves to the firebase-messaging fallback " + FCM_FALLBACK_SERVICE
                            + ". Keep com.pushwoosh.firebase.PushFcmIntentService in the merged manifest, or declare"
                            + " a service that extends com.pushwoosh.firebase.PushwooshFcmRouterService.");
            return;
        }
        if (extendsRouter(winnerName)) {
            return;
        }
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs == null || prefs.fcmMessageReceivedSinceInstallOrUpdate().get()) {
            return;
        }
        PWLog.info(
                TAG,
                "No Pushwoosh message has reached PushwooshFcmHelper since the app was installed or last"
                        + " updated. FCM messages in this app are delivered to " + winnerName + ". If it already"
                        + " calls PushwooshFcmHelper.onMessageReceived() and PushwooshFcmHelper.onTokenRefresh()"
                        + " from its callbacks, nothing is wrong: this notice clears after the first Pushwoosh"
                        + " message arrives. Otherwise Pushwoosh delivery and open statistics are not collected:"
                        + " make " + winnerName + " extend com.pushwoosh.firebase.PushwooshFcmRouterService,"
                        + " or add those calls.");
    }

    private static boolean extendsRouter(String winnerName) {
        // Same technique as ManifestValidator, but initialize=false: the winner may be another library's
        // service, and its static init is not ours to run on the SDK pool.
        try {
            Class<?> cls = Class.forName(winnerName, false, PushwooshFcmRouterService.class.getClassLoader());
            return PushwooshFcmRouterService.class.isAssignableFrom(cls);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void warnAboutLosers(PackageManager pm, Intent intent, String winnerName) {
        List<ResolveInfo> registered = pm.queryIntentServices(intent, 0);
        if (registered == null) {
            return;
        }
        for (ResolveInfo info : registered) {
            if (info == null || info.serviceInfo == null || info.serviceInfo.name == null) {
                continue;
            }
            String name = info.serviceInfo.name;
            if (name.equals(winnerName)
                    || name.equals(FCM_FALLBACK_SERVICE)
                    || name.equals(PushFcmIntentService.class.getName())) {
                continue;
            }
            PWLog.warn(
                    TAG,
                    "FCM will not deliver messages to " + name + ": " + winnerName
                            + " wins the resolution of " + ACTION_MESSAGING_EVENT + " for this package."
                            + " Remove the leftover service or give the intended one a higher"
                            + " android:priority on its intent-filter.");
        }
    }

    @VisibleForTesting
    static void resetForTesting() {
        validated.set(false);
    }
}
