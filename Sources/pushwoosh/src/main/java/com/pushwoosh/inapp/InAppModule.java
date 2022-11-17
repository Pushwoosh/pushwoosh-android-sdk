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

package com.pushwoosh.inapp;

import androidx.annotation.Nullable;

import com.pushwoosh.inapp.mapper.ResourceMapper;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.downloader.InAppDownloader;
import com.pushwoosh.inapp.storage.ContextInAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppDbHelper;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.repository.RepositoryModule;

public class InAppModule {

    private static final InAppFolderProvider inAppFolderProvider = new ContextInAppFolderProvider(AndroidPlatformModule.getApplicationContext());
    private static final InAppDownloader inAppDownloader = new InAppDownloader(getInAppFolderProvider());
    private static final ResourceMapper resourceMapper = new ResourceMapper(getInAppFolderProvider());

    private static volatile InAppStorage inAppStorage;
    private static final Object inAppStorageMutex = new Object();

    private static volatile InAppRepository inAppRepository;
    private static final Object inAppRepositoryMutex = new Object();

    public static InAppFolderProvider getInAppFolderProvider() {
        return inAppFolderProvider;
    }

    private static InAppDownloader getInAppDownloader() {
        return inAppDownloader;
    }

    private static ResourceMapper getResourceMapper() {
        return resourceMapper;
    }

    @Nullable
    public static InAppStorage getInAppStorage() {
        synchronized (inAppStorageMutex) {
            if (inAppStorage == null) {
                if (AndroidPlatformModule.getApplicationContext() == null) {
                    return null;
                }

                inAppStorage = new InAppDbHelper(AndroidPlatformModule.getApplicationContext());
            }
        }

        return inAppStorage;
    }

    @Nullable
    public static InAppRepository getInAppRepository() {
        synchronized (inAppRepositoryMutex) {
            if (inAppRepository == null) {
                if (AndroidPlatformModule.getApplicationContext() == null) {
                    return null;
                }
                inAppRepository = new InAppRepository(
                        NetworkModule.getRequestManager(),
                        getInAppStorage(),
                        getInAppDownloader(),
                        getResourceMapper(),
                        getInAppFolderProvider(),
                        RepositoryModule.getRegistrationPreferences());
            }
        }

        return inAppRepository;
    }

    public static void setInAppRepository(InAppRepository inAppRepository) {
        InAppModule.inAppRepository = inAppRepository;
    }

    public static void setInAppStorage(InAppStorage inAppStorage) {
        InAppModule.inAppStorage = inAppStorage;
    }
}
