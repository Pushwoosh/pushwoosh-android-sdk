/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.specific.DeviceSpecificProvider;

/**
 * The values {@code registerDevice} reports about this device, read once so the request sends what existed when it
 * was created.
 */
final class RegistrationSnapshot {
    @NonNull private final String pushToken;

    @NonNull private final String applicationCode;

    @NonNull private final String hwid;

    private final int deviceType;

    @Nullable private final String language;

    @Nullable private final String locale;

    private RegistrationSnapshot(
            @NonNull String pushToken,
            @NonNull String applicationCode,
            @NonNull String hwid,
            int deviceType,
            @Nullable String language,
            @Nullable String locale) {
        this.pushToken = pushToken;
        this.applicationCode = applicationCode;
        this.hwid = hwid;
        this.deviceType = deviceType;
        this.language = language;
        this.locale = locale;
    }

    /**
     * Reads the current registration state.
     * <p>
     * {@code language} and {@code locale} are {@code null} when locale collection is not allowed at the moment of
     * reading. Values are stored as read, without normalisation.
     *
     * @return the snapshot, or {@code null} when the SDK is not initialized or no push transport module is present
     */
    @Nullable static RegistrationSnapshot current() {
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        DeviceSpecificProvider provider = DeviceSpecificProvider.getInstance();
        if (prefs == null || provider == null) {
            return null;
        }

        boolean collectingLocale = AppOpenRequest.isLocaleCollectionAllowed();
        return new RegistrationSnapshot(
                prefs.pushToken().get(),
                prefs.applicationId().get(),
                prefs.hwid().get(),
                provider.deviceType(),
                collectingLocale ? prefs.language().get() : null,
                collectingLocale ? prefs.localeForRequest() : null);
    }

    @NonNull String pushToken() {
        return pushToken;
    }

    @NonNull String applicationCode() {
        return applicationCode;
    }

    @NonNull String hwid() {
        return hwid;
    }

    int deviceType() {
        return deviceType;
    }

    @Nullable String language() {
        return language;
    }

    @Nullable String locale() {
        return locale;
    }
}
