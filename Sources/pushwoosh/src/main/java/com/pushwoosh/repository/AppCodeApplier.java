package com.pushwoosh.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Owns the decision of applying an (application code, base URL) pair to the persisted
 * registration state: validates the input, detects an application code change, runs the
 * re-registration cycle bookkeeping and reports what happened as a verdict.
 */
public class AppCodeApplier {
    private static final String TAG = "AppCodeApplier";

    private final RegistrationPrefs prefs;

    public AppCodeApplier(@NonNull RegistrationPrefs prefs) {
        this.prefs = prefs;
    }

    /**
     * Applies the pair and reports the verdict.
     *
     * <p>An application code change runs the re-registration cycle bookkeeping (clears the stored
     * pair, arms {@code forceRegister}) and returns the replaced identity via
     * {@link Result#getPreviousRegistration()} so the caller can unregister the device from the
     * previous application. A base URL change alone just persists the new URL (server-migration
     * semantics).
     *
     * @param appCode application code
     * @param customBaseUrl raw base URL, or {@code null} to keep or compute the default one
     * @return {@code rejected} on invalid input (nothing is written), {@code applied} otherwise
     */
    public Result apply(@NonNull final String appCode, @Nullable final String customBaseUrl) {
        if (TextUtils.isEmpty(appCode)) {
            PWLog.error(TAG, "apply() rejected: empty application code");
            return Result.rejected();
        }
        String normalizedCustomUrl = null;
        if (customBaseUrl != null) {
            normalizedCustomUrl = RegistrationPrefs.normalizeBaseUrl(customBaseUrl);
            if (normalizedCustomUrl == null) {
                PWLog.error(TAG, "apply() rejected: invalid base URL: " + customBaseUrl);
                return Result.rejected();
            }
        }

        String previousAppCode = prefs.applicationId().get();
        String previousBaseUrl = prefs.baseUrl().get();
        boolean appCodeChange = !TextUtils.isEmpty(previousAppCode) && !TextUtils.equals(previousAppCode, appCode);

        PreviousRegistration previous = null;
        if (appCodeChange) {
            previous = new PreviousRegistration(
                    previousAppCode,
                    prefs.pushToken().get(),
                    previousBaseUrl,
                    prefs.registeredOnServer().get());
            prefs.removeAppId();
            prefs.forceRegister().set(prefs.isRegisteredForPush().get());
        }

        prefs.applicationId().set(appCode);

        if (normalizedCustomUrl != null) {
            prefs.updateBaseUrl(normalizedCustomUrl);
        } else if (!TextUtils.equals(previousAppCode, appCode) || TextUtils.isEmpty(previousBaseUrl)) {
            String defaultUrl = prefs.getDefaultBaseUrl(appCode);
            if (prefs.updateBaseUrl(defaultUrl) == null) {
                PWLog.error(
                        TAG,
                        "Default base URL rejected: " + defaultUrl
                                + ". Check com.pushwoosh.base_url in AndroidManifest.xml.");
            }
        }

        return Result.applied(prefs.baseUrl().get(), previous);
    }

    /**
     * Verdict of {@link #apply}: rejected (invalid input, nothing written) or applied. An
     * application code change additionally carries the {@link PreviousRegistration} being replaced.
     */
    public static class Result {
        private static final Result REJECTED = new Result(true, null, null);

        private final boolean rejected;

        @Nullable private final String baseUrl;

        @Nullable private final PreviousRegistration previousRegistration;

        private Result(
                boolean rejected, @Nullable String baseUrl, @Nullable PreviousRegistration previousRegistration) {
            this.rejected = rejected;
            this.baseUrl = baseUrl;
            this.previousRegistration = previousRegistration;
        }

        static Result applied(String baseUrl, @Nullable PreviousRegistration previous) {
            return new Result(false, baseUrl, previous);
        }

        static Result rejected() {
            return REJECTED;
        }

        public boolean isRejected() {
            return rejected;
        }

        /** The base URL persisted by this call; null only when rejected. */
        @Nullable public String getBaseUrl() {
            return baseUrl;
        }

        /** The registration being left behind; non-null only when the application code changed. */
        @Nullable public PreviousRegistration getPreviousRegistration() {
            return previousRegistration;
        }
    }

    /** Snapshot of the registration being replaced, taken before the stored pair is cleared. */
    public static class PreviousRegistration {
        public final String appCode;
        public final String pushToken;
        public final String baseUrl;
        public final boolean wasRegisteredOnServer;

        private PreviousRegistration(String appCode, String pushToken, String baseUrl, boolean wasRegisteredOnServer) {
            this.appCode = appCode;
            this.pushToken = pushToken;
            this.baseUrl = baseUrl;
            this.wasRegisteredOnServer = wasRegisteredOnServer;
        }
    }
}
