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

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.js.PushwooshJSInterface;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaStyle;

import java.lang.ref.WeakReference;

public class RichMediaWebActivity extends WebActivity implements OnRichMediaListener {
    private static final String TAG = "[InApp]RichMediaWebAct";
    private static final String FRAGMENT_TAG = TAG + "pushwoosh.inAppFragment";
    private static final String KEY_IS_CLOSED = "IS_CLOSED";
    private static final String KEY_IS_ANIMATED = "IS_ANIMATED";
    private static final String KEY_IS_SOUND_PLAYED = "KEY_IS_SOUND_PLAYED";
    private boolean onBackPressedEnable;
    final Handler handler = new Handler();
    private boolean isAnimatedClose;

    public static Intent createInAppIntent(Context context, Resource resource) {
        return applyIntentParams(createIntent(context), resource, "", InAppView.MODE_DEFAULT);
    }

    public static Intent createRichMediaIntent(Context context, Resource resource) {
        return applyIntentParams(createIntent(context), resource, "", InAppView.MODE_DEFAULT);
    }

    public static Intent createRichMediaLockScreenIntent(Context context, Resource resource, String sound) {
        Intent intent = applyIntentParams(createIntent(context), resource, sound, InAppView.MODE_LOCKSCREEN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
    }

    private static Intent createIntent(Context context) {
        return new Intent(context, RichMediaWebActivity.class);
    }

    @Nullable
    private ResourceWebView resourceWebView;
    private String sound;
    private int mode;
    private HtmlData htmlData;
    private boolean viewTracked;
    private boolean isAnimated;
    private boolean closed = false;
    private boolean isSoundPlayed;

    @Override
    protected void onCreate(Bundle state) {
        try {
            super.onCreate(state);
        } catch (Throwable t) {
            // fix for "java.lang.IllegalStateException: Could not find active fragment with index -1"
            finish();
        }

        if (state != null) {
            if(state.getBoolean(KEY_IS_CLOSED)){
                finish();
                return;
            }
            isAnimated = state.getBoolean(KEY_IS_ANIMATED);
            isSoundPlayed = state.getBoolean(KEY_IS_SOUND_PLAYED);
            sound = state.getString(EXTRA_SOUND, "");
            mode = state.getInt(EXTRA_MODE, mode);
        }
        startTimerEnableBackButton();
    }

    private void startTimerEnableBackButton() {
        RichMediaStyle richMediaStyle = PushwooshPlatform.getInstance().getRichMediaStyle();
        long timeOutBackButtonEnable = richMediaStyle.getTimeOutBackButtonEnable();
        if (timeOutBackButtonEnable == 0) {
            onBackPressedEnable = true;
        } else {
            handler.postDelayed(() -> onBackPressedEnable = true, timeOutBackButtonEnable);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_ANIMATED, isAnimated);
        outState.putBoolean(KEY_IS_CLOSED, isAnimatedClose);
        outState.putBoolean(KEY_IS_SOUND_PLAYED, isSoundPlayed);
        outState.putString(EXTRA_SOUND, sound);
        outState.putInt(EXTRA_MODE, mode);
    }

    @Override
    protected void resourceChanged(Resource resource, String sound, int mode) {
        this.sound = sound;
        this.mode = mode;
        this.isSoundPlayed = false;

        InAppFragment inAppFragment = (InAppFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if (inAppFragment != null) {
            fragmentTransaction
                    .remove(inAppFragment);
        }
        viewTracked = false;

        inAppFragment = InAppFragment.createInstance(resource);

        fragmentTransaction
                .add(inAppFragment, FRAGMENT_TAG)
                .commitAllowingStateLoss();

        viewTracked = false;
    }

    @Override
    protected void updateWebView(@Nullable ResourceWebView resourceWebView) {
        this.resourceWebView = resourceWebView;
        setContentView(resourceWebView);

        InAppFragment fragmentByTag = (InAppFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);

        if (fragmentByTag != null) {
            fragmentByTag.syncState();
        }
    }

    @Override
    public void onPageLoaded() {
        super.onPageLoaded();
        waitContentMeasure();
    }

    @Override
    public void startLoadingRichMedia() {
        if (resourceWebView != null) {
            resourceWebView.showProgress();
        }
    }

    @Override
    public void finishLoadingRichMedia() {
        if (resourceWebView != null) {
            resourceWebView.hideProgress();
        }
    }

    @Override
    public boolean successLoadingHtmlData(HtmlData htmlData) {
        if (resourceWebView == null || htmlData.equals(this.htmlData)) {
            return false;
        }

        this.htmlData = htmlData;

        String htmlContent = htmlData.getHtmlContent();
        String baseUrl = htmlData.getUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        resourceWebView.loadDataWithBaseURL(baseUrl, htmlContentWithPushwooshInterface(htmlContent), "text/html", "UTF-8", null);
        return true;
    }

    private String htmlContentWithPushwooshInterface(String content) {
        String messageHash = RepositoryModule.getNotificationPreferences().messageHash().get();
        String jsInterface = String.format(PushwooshJSInterface.PUSHWOOSH_JS,
                Pushwoosh.getInstance().getHwid(),
                GeneralUtils.SDK_VERSION,
                Pushwoosh.getInstance().getApplicationCode(),
                Pushwoosh.getInstance().getUserId(),
                !resource.isInApp() ? resource.getCode().substring(2) : "",
                DeviceSpecificProvider.getInstance().deviceType(),
                messageHash != null ? messageHash : "",
                resource.isInApp() ? resource.getCode() : ""
        );
        return content.replace("<head>", "<head>\n<script type=\"text/javascript\">" + jsInterface + "</script>");
    }

    @Override
    public void failedLoadingHtmlData(ResourceParseException exception) {
        PWLog.error(TAG, "Failed loading html data", exception);
        if (resource.isInApp()) {
            EventBus.sendEvent(new InAppViewFailedEvent(resource));
        }
        close();
    }

    private void waitContentMeasure() {
        if (resourceWebView == null) {
            return;
        }

        if ((mode & InAppView.MODE_LOCKSCREEN) != 0) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

            NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
            if (notificationPrefs.lightScreenOn().get()) {
                NotificationUtils.turnScreenOn();
            }
        }

        Uri soundUri = NotificationUtils.getSoundUri(sound);
        if (soundUri != null && !isSoundPlayed) {
            isSoundPlayed = true;
            new GetRingtoneTask(this, soundUri, result -> {
                Ringtone sound = result.getData();
                if (sound != null) {
                    sound.play();
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (!isAnimated) {
            if (!viewTracked) {
                viewTracked = true;
                EventBus.sendEvent(new InAppViewEvent(resource));
            }
            resourceWebView.animateOpen();
            isAnimated = true;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendClose();
    }

    @Override
    public void close() {
        InAppFragment inAppFragment = (InAppFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (inAppFragment != null) {
            getFragmentManager().beginTransaction()
                    .remove(inAppFragment)
                    .commitAllowingStateLoss();
        }
        if (isAnimatedClose) {
            return;
        } else {
            isAnimatedClose = true;
        }

        if (resourceWebView != null) {
            resourceWebView.animateClose(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    RichMediaWebActivity.super.close();
                    overridePendingTransition(0, 0);
                    resourceWebView.setVisibility(View.GONE);
                    sendClose();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }

    private void sendClose() {
        if (!closed) {
            closed = true;
            EventBus.sendEvent(new RichMediaCloseEvent(resource));
        }
    }

    @Override
    public void onBackPressed() {
        if (onBackPressedEnable) {
            close();
        }
    }

    private static class GetRingtoneTask extends AsyncTask<Void, Void, Ringtone> {
        private final WeakReference<Context> contextWeakRef;
        private final Uri soundUri;
        private final Callback<Ringtone, PushwooshException> callback;

        public GetRingtoneTask(Context context, Uri soundUri, Callback<Ringtone, PushwooshException> callback) {
            this.contextWeakRef = new WeakReference<>(context);
            this.soundUri = soundUri;
            this.callback = callback;
        }

        @Override
        protected Ringtone doInBackground(Void... voids) {
            if (contextWeakRef.get() == null) {
                return null;
            }
            try {
                return RingtoneManager.getRingtone(contextWeakRef.get(), soundUri);
            } catch (Exception e) {
                PWLog.error("Failed parse ringtone with songUri: " + soundUri, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Ringtone ringtone) {
            super.onPostExecute(ringtone);
            callback.process(Result.from(ringtone, null));
        }
    }
}