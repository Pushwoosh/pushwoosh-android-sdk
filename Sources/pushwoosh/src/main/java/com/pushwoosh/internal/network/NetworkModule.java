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

package com.pushwoosh.internal.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.repository.RegistrationPrefs;

/**
 * DI to provide {@link com.pushwoosh.internal.network.RequestManager}
 */
public class NetworkModule {
    private static final RequestManager NOT_INITIALIZED = new NotInitializedRequestManager();

    private static volatile RequestManager requestManager = NOT_INITIALIZED;

    public static synchronized void init(
            RegistrationPrefs registrationPrefs,
            ServerCommunicationManager serverCommunicationManager,
            boolean reverseProxyRequired) {
        if (requestManager == NOT_INITIALIZED) {
            requestManager =
                    new PushwooshRequestManager(registrationPrefs, serverCommunicationManager, reverseProxyRequired);
        }
    }

    @NonNull public static RequestManager getRequestManager() {
        return requestManager;
    }

    /**
     * Installs a manager, or resets to the not-initialized stub when {@code requestManager} is null.
     */
    public static synchronized void setRequestManager(@Nullable RequestManager requestManager) {
        NetworkModule.requestManager = requestManager != null ? requestManager : NOT_INITIALIZED;
    }
}
