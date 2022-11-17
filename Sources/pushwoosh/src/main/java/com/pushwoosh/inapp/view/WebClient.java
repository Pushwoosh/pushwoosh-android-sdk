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

import java.util.Map;
import java.util.logging.Logger;

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
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.event.RichMediaPresentEvent;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.js.JsCallback;
import com.pushwoosh.inapp.view.js.PushManagerJSInterface;
import com.pushwoosh.inapp.view.js.PushwooshJSInterface;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

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

	private Resource resource;

	private View mainContainer;

	public WebClient(InAppView inAppView, Resource resource) {
		this.inAppView = inAppView;
		this.resource = resource;

		jsInterfaces = PushwooshPlatform.getInstance().pushwooshInApp().getJavascriptInterfaces();
	}

	public void attachToWebView(WebView webView) {
		String customData = RepositoryModule.getNotificationPreferences().customData().get();

		pushwooshJSInterface = new PushwooshJSInterface(this, webView, mainContainer, customData);

		RepositoryModule.getNotificationPreferences().customData().set(null);

		//WebView.setWebContentsDebuggingEnabled(true);

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

	public void attachMainContainer(View view){
		this.mainContainer = view;
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		super.onPageFinished(view, url);

		PWLog.noise(TAG, "Finished loading url: " + url);

		pushwooshJSInterface.onPageFinished(view);

		inAppView.onPageLoaded();

		EventBus.sendEvent(new RichMediaPresentEvent(resource));
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		super.onPageStarted(view, url, favicon);
		PWLog.noise(TAG, "Page started: " + url);

		pushwooshJSInterface.onPageStarted(view);
	}

	@Override
	public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
		super.onReceivedError(view, request, error);

		PWLog.error(TAG, "Page failed: " + request.toString() + "; " + error.toString());
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		final Uri uri = Uri.parse(url);
		return handleUri(view.getContext(), uri);
	}

	@TargetApi(Build.VERSION_CODES.N)
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
		final Uri uri = request.getUrl();
		return handleUri(view.getContext(), uri);
	}

	private boolean handleUri(Context context, Uri uri) {
		PWLog.noise(TAG, "Trying to open url: " + uri);

		// custom pushwoosh scheme
		if (uri.getScheme().equals("pushwoosh")) {
			if (uri.getHost() != null) {
				return applyPushwooshUrlScheme(uri.getHost(), context);
			} else {
				PWLog.error(TAG, "Wrong url format: " + uri);
			}

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
		//launching default launcher category activity
		try {
			Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
			intent.addCategory("android.intent.category.LAUNCHER");
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
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

		//do not let webview to open this url itself
		return true;
	}

	private boolean isLockScreen() {return (inAppView.getMode() & InAppView.MODE_LOCKSCREEN) != 0;}

	@Override
	public void close() {
		handler.post(inAppView::close);
	}
}
