package com.pushwoosh.appevents;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Fires a delayed {@code PW_ApplicationExit} event when the user leaves the app
 * and does not return within the configured timeout.
 *
 * <p>Timeout is clamped to [{@link #MIN_EXIT_INTENT_TIMEOUT_SECONDS},
 * {@link #MAX_EXIT_INTENT_TIMEOUT_SECONDS}]. Screen name and session duration
 * are captured at backgrounding time to reflect the actual session, not the
 * extended wait for exit intent.</p>
 */
class ExitIntentDetector {

    private static final String TAG = "ExitIntentDetector";
    static final int MIN_EXIT_INTENT_TIMEOUT_SECONDS = 10;
    static final int MAX_EXIT_INTENT_TIMEOUT_SECONDS = 30;

    private final int effectiveTimeoutSeconds;
    private final long exitIntentTimeoutMs;
    private final ExitIntentCallback callback;
    private final Handler handler;
    private final Runnable exitIntentRunnable;

    @Nullable private String pendingScreenName;

    private long pendingSessionDurationSeconds;

    ExitIntentDetector(int timeoutSeconds, @NonNull ExitIntentCallback callback) {
        if (timeoutSeconds < MIN_EXIT_INTENT_TIMEOUT_SECONDS) {
            PWLog.warn(
                    TAG,
                    "Exit Intent timeout " + timeoutSeconds + "s is below minimum ("
                            + MIN_EXIT_INTENT_TIMEOUT_SECONDS + "s). Using "
                            + MIN_EXIT_INTENT_TIMEOUT_SECONDS + "s.");
        } else if (timeoutSeconds > MAX_EXIT_INTENT_TIMEOUT_SECONDS) {
            PWLog.warn(
                    TAG,
                    "Exit Intent timeout " + timeoutSeconds + "s exceeds maximum ("
                            + MAX_EXIT_INTENT_TIMEOUT_SECONDS + "s). Using "
                            + MAX_EXIT_INTENT_TIMEOUT_SECONDS + "s.");
        }
        this.effectiveTimeoutSeconds =
                Math.min(MAX_EXIT_INTENT_TIMEOUT_SECONDS, Math.max(MIN_EXIT_INTENT_TIMEOUT_SECONDS, timeoutSeconds));
        this.exitIntentTimeoutMs = effectiveTimeoutSeconds * 1000L;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
        this.exitIntentRunnable = this::onExitIntentFired;
    }

    void onAppBackgrounded(@Nullable String screenName, long sessionDurationSeconds) {
        pendingScreenName = screenName;
        pendingSessionDurationSeconds = sessionDurationSeconds;
        handler.removeCallbacks(exitIntentRunnable);
        handler.postDelayed(exitIntentRunnable, exitIntentTimeoutMs);
    }

    void onAppForegrounded() {
        handler.removeCallbacks(exitIntentRunnable);
        pendingScreenName = null;
        pendingSessionDurationSeconds = 0;
    }

    private void onExitIntentFired() {
        PWLog.debug(TAG, "Exit intent detected for " + pendingScreenName + " after " + effectiveTimeoutSeconds + "s");
        callback.onExitIntent(pendingScreenName, pendingSessionDurationSeconds, effectiveTimeoutSeconds);
        pendingScreenName = null;
        pendingSessionDurationSeconds = 0;
    }

    interface ExitIntentCallback {
        void onExitIntent(@Nullable String screenName, long sessionDurationSeconds, int exitIntentTimeoutSeconds);
    }
}
