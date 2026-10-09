package com.pushwoosh.featureflags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.utils.BackgroundExecutor;

/**
 * Feature flags evaluated by Pushwoosh for this device and user.
 * <p>
 * Obtain it with {@code Pushwoosh.getInstance().getFeatureFlags()}; there is one instance per process, so it is safe
 * to keep a reference. {@link #get(String)} reads the local cache only and never blocks on the network; the SDK
 * refreshes the cache on app start, on return from background after 30 minutes, hourly while in foreground, after
 * the first registration and after {@code setUserId}.
 * Do not store personal data in flag properties.
 */
public final class PushwooshFeatureFlags {
    interface ManagerSource {
        @Nullable FeatureFlagsManager current();
    }

    @NonNull private final ManagerSource source;

    PushwooshFeatureFlags(@NonNull ManagerSource source) {
        this.source = source;
    }

    /**
     * Reads a flag from the local cache without touching the network.
     *
     * @param key the flag key as configured in Pushwoosh
     * @return the cached flag, or {@code null} when the SDK is not initialized, there is no cache younger than
     *         24 hours for the current user, device data was removed, or there is no such flag
     */
    @Nullable public FeatureFlag get(@NonNull String key) {
        FeatureFlagsManager manager = source.current();
        return manager != null ? manager.get(key) : null;
    }

    /**
     * Reads the enabled state of a flag, falling back to the app's own default.
     *
     * @param key the flag key as configured in Pushwoosh
     * @param defaultValue the value to use while the flag is not available, see {@link #get(String)}
     * @return {@link FeatureFlag#isEnabled()} of the cached flag, or {@code defaultValue} when {@link #get} is null
     */
    public boolean isEnabled(@NonNull String key, boolean defaultValue) {
        FeatureFlag flag = get(key);
        return flag != null ? flag.isEnabled() : defaultValue;
    }

    /**
     * Requests fresh flags. A call made while a request is in flight joins it and gets its result. Calls within
     * 5 minutes of the previous request complete at once with the cache; the throttle can be turned off with the
     * meta-data {@code com.pushwoosh.feature_flags.disable_throttle}. Failures of a request, a call before the SDK is
     * initialized and a call after its initialization failed are reported as {@link FeatureFlagsException}. A call
     * made while the SDK is still initializing waits for it; if that initialization fails, the callback is not
     * invoked. The callback is invoked on the main thread.
     *
     * @param callback receives the outcome; may be {@code null}
     */
    public void refresh(@Nullable Callback<Void, FeatureFlagsException> callback) {
        FeatureFlagsManager manager = source.current();
        if (manager != null) {
            manager.refresh(callback);
            return;
        }
        if (callback != null) {
            BackgroundExecutor.main(() -> callback.process(Result.<Void, FeatureFlagsException>fromException(
                    new FeatureFlagsException("SDK is not initialized", 0))));
        }
    }

    /**
     * Registers a listener for flag changes.
     * <p>
     * The listener is called on the main thread after a refresh changed any flag. Adding the same listener twice
     * registers it once.
     *
     * @param listener the listener to notify
     */
    public void addUpdateListener(@NonNull FeatureFlagsUpdateListener listener) {
        FeatureFlagsManager manager = source.current();
        if (manager != null) {
            manager.addUpdateListener(listener);
        }
    }

    /**
     * Unregisters a listener added with {@link #addUpdateListener(FeatureFlagsUpdateListener)}.
     * <p>
     * Removing a listener that is not registered has no effect.
     *
     * @param listener the listener to stop notifying
     */
    public void removeUpdateListener(@NonNull FeatureFlagsUpdateListener listener) {
        FeatureFlagsManager manager = source.current();
        if (manager != null) {
            manager.removeUpdateListener(listener);
        }
    }

    /**
     * Reports that the user was shown the branch of {@code key}, enabled or not.
     * <p>
     * A flag value is reported once per session; a report that failed to send is retried by the next call. Nothing
     * is sent when {@link #get} returns null.
     *
     * @param key the flag key whose branch was shown
     */
    public void logImpression(@NonNull String key) {
        FeatureFlagsManager manager = source.current();
        if (manager != null) {
            manager.logImpression(key);
        }
    }
}
