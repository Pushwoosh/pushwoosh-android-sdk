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

package com.pushwoosh.inapp.view;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.event.RichMediaPresentEvent;
import com.pushwoosh.inapp.mapper.ResourceMapper;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.js.JsCallback;
import com.pushwoosh.inapp.view.js.PushManagerJSInterface;
import com.pushwoosh.inapp.view.js.PushwooshJSInterface;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public class WebClient extends WebViewClient implements JsCallback {
    private static final String TAG = "[InApp]WebClient";

    private static final String CLOSE = "close";
    private static final String OPEN = "open";

    private static final String JS_PUSH_MANAGER = "pushManager";
    private static final String JS_PUSHWOOSH = "pushwooshImpl";

    private final InAppView inAppView;
    private final Map<String, Object> jsInterfaces;

    private PushwooshJSInterface pushwooshJSInterface;

    private Handler handler = new Handler(Looper.getMainLooper());

    private volatile Resource resource;

    private View mainContainer;

    @Nullable private WebViewAssetLoader assetLoader;

    private boolean assetLoaderInitialized;

    private boolean released;

    public WebClient(InAppView inAppView, Resource resource) {
        this.inAppView = inAppView;
        this.resource = resource;

        jsInterfaces = PushwooshPlatform.getInstance().pushwooshInApp().getJavascriptInterfaces();
    }

    public void attachToWebView(WebView webView) {
        String customData =
                RepositoryModule.getNotificationPreferences().customData().get();
        String messageHash =
                RepositoryModule.getNotificationPreferences().messageHash().get();

        pushwooshJSInterface = new PushwooshJSInterface(this, webView, mainContainer, customData, messageHash);

        RepositoryModule.getNotificationPreferences().customData().set(null);

        // WebView.setWebContentsDebuggingEnabled(true);

        webView.setWebViewClient(this);

        // add bridge to handle js calls to pushwoosh API
        webView.addJavascriptInterface(new PushManagerJSInterface(webView, this), JS_PUSH_MANAGER);
        webView.addJavascriptInterface(pushwooshJSInterface, JS_PUSHWOOSH);

        // add user javascript interfaces
        for (Map.Entry<String, Object> entry : jsInterfaces.entrySet()) {
            String name = entry.getKey();
            Object jsInterface = entry.getValue();
            webView.addJavascriptInterface(jsInterface, name);
        }
    }

    public void attachMainContainer(View view) {
        this.mainContainer = view;
    }

    // Released from clear() before destroy(): skips side effects of lifecycle callbacks still queued after teardown (JS
    // bridge, phantom present event).
    public void release() {
        released = true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        PWLog.noise(TAG, String.format("onPageFinished(url: %s)", url));

        if (released) {
            return;
        }

        pushwooshJSInterface.onPageFinished(view, resource);

        inAppView.onPageLoaded();

        EventBus.sendEvent(new RichMediaPresentEvent(resource));
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        PWLog.noise(TAG, String.format("onPageStarted(url: %s)", url));

        if (released) {
            return;
        }

        pushwooshJSInterface.onPageStarted(view, resource);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        PWLog.error(TAG, String.format("onReceivedError(request: %s, error: %s)", request, error));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        PWLog.noise(TAG, String.format("shouldOverrideUrlLoading(url: %s)", url));
        final Uri uri = Uri.parse(url);
        return handleUri(view.getContext(), uri);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        PWLog.noise(TAG, String.format("shouldOverrideUrlLoading(request: %s)", request.getUrl()));
        final Uri uri = request.getUrl();
        return handleUri(view.getContext(), uri);
    }

    @Override
    @Nullable public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return interceptViaAssetLoader(view, request.getUrl());
    }

    @SuppressWarnings("deprecation")
    @Override
    @Nullable public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return interceptViaAssetLoader(view, Uri.parse(url));
    }

    @Nullable private WebResourceResponse interceptViaAssetLoader(WebView view, Uri uri) {
        WebViewAssetLoader loader = getAssetLoader(view.getContext());
        if (loader == null) {
            return null;
        }
        return loader.shouldInterceptRequest(uri);
    }

    // synchronized: parallel shouldInterceptRequest worker threads must not see assetLoaderInitialized==true while
    // assetLoader is still null mid-build and miss interception.
    @Nullable private synchronized WebViewAssetLoader getAssetLoader(Context context) {
        if (assetLoaderInitialized) {
            return assetLoader;
        }

        String code = resource.getCode();
        File inAppFolder = InAppModule.getInAppFolderProvider().getInAppFolder(code);
        if (inAppFolder == null) {
            // Don't latch initialized here: a null folder may be transient, so a later request can retry the build.
            PWLog.warn(TAG, "InApp folder is null for code " + code + "; serving without asset interception");
            return null;
        }
        assetLoaderInitialized = true;

        try {
            assetLoader = new WebViewAssetLoader.Builder()
                    // Same host constant as ResourceMapper's document origin so the two never drift.
                    .setDomain(ResourceMapper.RICH_MEDIA_ASSET_HOST)
                    .addPathHandler(
                            ResourceMapper.RICH_MEDIA_PATH_PREFIX + code + "/",
                            new WebViewAssetLoader.InternalStoragePathHandler(context, inAppFolder))
                    .build();
        } catch (RuntimeException e) {
            // Builder can throw IllegalArgumentException; an uncaught throw on the WebView worker thread crashes the
            // process, so degrade to no interception (null → normal load).
            PWLog.warn(
                    TAG,
                    "Failed to build rich media asset loader for code " + code + "; serving without interception",
                    e);
            assetLoader = null;
        }
        return assetLoader;
    }

    private boolean handleUri(Context context, Uri uri) {
        PWLog.noise(TAG, String.format("handleUri(uri: %s)", uri));

        // A schemeless URI (relative/empty/fragment href in server-controlled rich-media HTML) makes
        // Uri.getScheme() null; the scheme checks below would NPE on the WebView's main-thread callback.
        // Such a navigation is neither our scheme nor an openable external URL, so consume it as a no-op.
        if (uri.getScheme() == null) {
            PWLog.warn(TAG, "Ignoring navigation to schemeless uri: " + uri);
            return true;
        }

        // custom pushwoosh scheme
        if (uri.getScheme().equals("pushwoosh")) {
            if (uri.getHost() != null) {
                return applyPushwooshUrlScheme(uri.getHost(), context);
            } else {
                PWLog.error(TAG, "Wrong url format: " + uri);
            }

            return true;
        }

        // Layer 1: navigation to our own synthetic origin (e.g. empty href → base URL) is a no-op, never opened
        // externally.
        if (ResourceMapper.RICH_MEDIA_ASSET_HOST.equals(uri.getHost())) {
            return true;
        }

        if (!uri.getScheme().startsWith("file")) {

            if (!isLockScreen()) {
                openRemoteUrl(uri, context);
            } else {
                RepositoryModule.getLockScreenMediaStorage().cacheRemoteUrl(uri);
            }

            inAppView.close();
            return true;
        }

        return true;
    }

    private void openRemoteUrl(Uri url, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            PWLog.error("Can't open remote url: " + url, e);
        }
    }

    private void launchDefaultActivity(final Context context) {
        // launching default launcher category activity
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent == null) {
                PWLog.warn("No launcher activity declared in manifest, skipping default launch.");
                return;
            }
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Logger.getLogger(getClass().getSimpleName()).severe("Failed to start default launch activity.");
        }
    }

    private boolean applyPushwooshUrlScheme(String method, final Context context) {
        if (method.equalsIgnoreCase(CLOSE)) {
            inAppView.close();
        } else if (method.equalsIgnoreCase(OPEN)) {
            if (isLockScreen()) {
                launchDefaultActivity(context);
                inAppView.close();
            }
        } else {
            PWLog.error(TAG, "Unrecognized pushwoosh method: " + method);
        }

        // do not let webview to open this url itself
        return true;
    }

    private boolean isLockScreen() {
        return (inAppView.getMode() & InAppView.MODE_LOCKSCREEN) != 0;
    }

    @Override
    public void close() {
        handler.post(inAppView::close);
    }
}
