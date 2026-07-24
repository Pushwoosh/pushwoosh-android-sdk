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

package com.pushwoosh.inapp.view.strategy;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppConfig;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.event.RichMediaErrorEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.mapper.NativeConfigLocalizer;
import com.pushwoosh.inapp.nativeui.NativeInAppPresenter;
import com.pushwoosh.inapp.nativeui.NativeInAppPresenterProvider;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.RichMediaWebActivity;
import com.pushwoosh.inapp.view.strategy.model.ResourceType;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ResourceViewStrategyFactory {
    private static final String TAG = "[InApp]ResourceViewStrategyFactory";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void showResource(ResourceWrapper resourceWrapper) {
        if (resourceWrapper == null) {
            PWLog.warn(TAG, "resourceWrapper is null, aborting show");
            return;
        }

        Resource resource = resourceWrapper.getResource();
        if (resource == null) {
            PWLog.warn(TAG, "resource is null, aborting show");
            return;
        }

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            PWLog.error(TAG, "context is null");
            return;
        }

        PWLog.info(
                TAG,
                String.format(
                        "presenting resource %s with code: %s, url: %s",
                        resourceWrapper.getResourceType(), resource.getCode(), resource.getUrl()));

        BackgroundExecutor.executeOnPool(() -> resolveDetectAndRoute(context, resourceWrapper, resource));
    }

    @WorkerThread
    private void resolveDetectAndRoute(Context context, ResourceWrapper resourceWrapper, Resource resource) {
        InAppRepository inAppRepository = InAppModule.getInAppRepository();
        if (inAppRepository == null) {
            sendError(resource, new ResourceParseException("InAppRepository is not initialized"));
            return;
        }

        Result<Resource, ResourceParseException> ensured = inAppRepository.ensureResolvedAndDeployed(resource);
        if (!ensured.isSuccess()) {
            sendError(resource, ensured.getException());
            return;
        }

        Resource resolved = ensured.getData();
        if (resolved == null) {
            sendError(resource, new ResourceParseException("Resolved resource is null for: " + resource.getCode()));
            return;
        }

        File nativeConfigFile = InAppModule.getInAppFolderProvider().getNativeConfigFile(resolved.getCode());
        if (nativeConfigFile == null || !nativeConfigFile.exists()) {
            mainHandler.post(() -> presentHtml(context, resourceWrapper, resource));
            return;
        }

        if (resourceWrapper.isLockScreen()) {
            PWLog.warn(TAG, "native in-app is not supported on lock screen, skipping: " + resolved.getCode());
            return;
        }

        String rawConfigJson;
        try {
            rawConfigJson = FileUtils.readFile(nativeConfigFile);
        } catch (IOException e) {
            // native-config.json is terminal: no HTML fallback even if index.html is in the same ZIP
            PWLog.error(TAG, "failed to read native-config.json for: " + resolved.getCode(), e);
            sendError(
                    resource,
                    new ResourceParseException("Failed to read native-config.json for: " + resolved.getCode(), e));
            return;
        }

        Map<String, String> localizedStrings;
        try {
            localizedStrings =
                    new InAppConfig(InAppModule.getInAppFolderProvider()).parseLocalizedStrings(resolved.getCode());
        } catch (Throwable e) {
            // No pushwoosh.json / no localization block: substitute placeholder defaults, never drop the show.
            // Throwable, not Exception: a pathologically nested pushwoosh.json can blow the parser stack
            // (StackOverflowError), and that must fall back to empty defaults too — same guard as
            // NativeConfigLocalizer.localize(), not slip past into a silently dropped show.
            PWLog.warn(TAG, "no localization dictionary for native in-app, using defaults: " + resolved.getCode(), e);
            localizedStrings = Collections.emptyMap();
        }

        String configJson = NativeConfigLocalizer.localize(rawConfigJson, localizedStrings);
        mainHandler.postDelayed(() -> presentNative(configJson, resolved), resourceWrapper.getDelay());
    }

    private void sendError(Resource resource, ResourceParseException exception) {
        EventBus.sendEvent(new RichMediaErrorEvent(resource, exception));
    }

    @MainThread
    private void presentNative(String configJson, Resource resource) {
        NativeInAppPresenter presenter = NativeInAppPresenterProvider.get();
        if (presenter == null) {
            PWLog.error(
                    TAG,
                    "native in-app detected but pushwoosh-inapp-ui module is not connected, skipping: "
                            + resource.getCode());
            return;
        }

        try {
            if (!presenter.present(configJson, resource)) {
                PWLog.warn(TAG, "native in-app presenter rejected resource: " + resource.getCode());
            }
        } catch (Throwable t) {
            PWLog.error(TAG, "native in-app presenter failed for: " + resource.getCode(), t);
        }
    }

    @MainThread
    private void presentHtml(Context context, ResourceWrapper resourceWrapper, Resource resource) {
        try {
            if (resourceWrapper.getResourceType() == ResourceType.IN_APP) {
                showInApp(context, resource);
            } else if (resourceWrapper.isLockScreen()) {
                showRichMediaLockScreen(context, resource, resourceWrapper.getSound());
            } else {
                showRichMedia(context, resource, resourceWrapper.getDelay());
            }
        } catch (Throwable t) {
            PWLog.error(TAG, "Failed to show resource", t);
        }
    }

    private void showInApp(Context context, Resource resource) {
        PushwooshPlatform.getInstance().pushwooshRepository().setCurrentInAppCode(resource.getCode());
        PushwooshPlatform.getInstance().pushwooshRepository().setCurrentRichMediaCode(null);

        if (RichMediaManager.getRichMediaType() == RichMediaType.MODAL) {
            ModalRichMediaWindow.showModalRichMediaWindow(resource);
        } else if (RichMediaManager.getRichMediaType() == RichMediaType.DEFAULT) {
            Intent intent = RichMediaWebActivity.createInAppIntent(context, resource);
            mainHandler.post(() -> context.startActivity(intent));
        }
    }

    private void showRichMedia(Context context, Resource resource, long delay) {
        String richMediaCode = resource.getCode().substring(2);
        PushwooshPlatform.getInstance().pushwooshRepository().setCurrentRichMediaCode(richMediaCode);
        PushwooshPlatform.getInstance().pushwooshRepository().setCurrentInAppCode(null);

        if (RichMediaManager.getRichMediaType() == RichMediaType.MODAL) {
            ModalRichMediaWindow.showModalRichMediaWindow(resource);
        } else if (RichMediaManager.getRichMediaType() == RichMediaType.DEFAULT) {
            Intent intent = RichMediaWebActivity.createRichMediaIntent(context, resource);
            mainHandler.postDelayed(() -> context.startActivity(intent), delay);
        }
    }

    private void showRichMediaLockScreen(Context context, Resource resource, String sound) {
        Intent intent = RichMediaWebActivity.createRichMediaLockScreenIntent(context, resource, sound);
        context.startActivity(intent);
    }
}
