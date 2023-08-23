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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.inapp.network.model.Resource;

public abstract class WebActivity extends Activity implements InAppView {
    static final String EXTRA_INAPP = "extraInApp";
    static final String EXTRA_MODE = "extraMode";
    static final String EXTRA_SOUND = "extraSound";
    static final String RICH_MEDIA_CODE= "richMediaCode";
    static final String IN_APP_CODE = "inAppCode";

    protected static Intent applyIntentParams(Intent intent, Resource resource, String sound, int mode) {
        intent.putExtra(EXTRA_INAPP, resource);
        intent.putExtra(EXTRA_SOUND, sound);
        intent.putExtra(EXTRA_MODE, mode);

        intent.putExtra(RICH_MEDIA_CODE, !resource.isInApp() ? resource.getCode().substring(2) : "");
        intent.putExtra(IN_APP_CODE, resource.isInApp() ? resource.getCode() : "");

        if (!resource.isRequired()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    private int mode;
    protected Resource resource;
    protected String richMediaCode;
    protected String inAppCode;

    @Nullable
    private ResourceWebView resourceWebView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getAttributes().flags |= WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        if (state != null) {
            resource = (Resource) state.getSerializable(EXTRA_INAPP);
        }

        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        processIntent(intent);
        setIntent(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("ResourceType")
    private void processIntent(Intent intent) {
        boolean isSameResource = resource != null && resource.equals(intent.getSerializableExtra(EXTRA_INAPP));

        if (isSameResource && resourceWebView != null) {
            return;
        }

        try {
            boolean isInMultiWindowMode = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInMultiWindowMode = this.isInMultiWindowMode();
            }

            RichMediaStyle richMediaStyle = PushwooshPlatform.getInstance().getRichMediaController().getRichMediaStyle();

            if (isSameResource) {
                resourceWebView = new ResourceWebView(this, resource.getLayout(), richMediaStyle, isInMultiWindowMode);
                resourceWebView.setWebViewClient(new WebClient(this, resource));
                return;
            }

            resource = (Resource) intent.getSerializableExtra(EXTRA_INAPP);
            if (resource == null) {
                finish();
                return;
            }

            richMediaCode = intent.getStringExtra(RICH_MEDIA_CODE);
            inAppCode = intent.getStringExtra(IN_APP_CODE);
            String sound = intent.getStringExtra(EXTRA_SOUND);
            mode = intent.getIntExtra(EXTRA_MODE, InAppView.MODE_DEFAULT);


            resourceWebView = new ResourceWebView(this, resource.getLayout(), richMediaStyle, isInMultiWindowMode);
            resourceWebView.setWebViewClient(new WebClient(this, resource));
            resourceChanged(resource, sound, mode);
        } finally {
            if (resourceWebView != null) {
                updateWebView(resourceWebView);
            }
        }
    }

    protected abstract void resourceChanged(Resource resource, String sound, int mode);

    protected abstract void updateWebView(@Nullable ResourceWebView resourceWebView);

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_INAPP, resource);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resourceWebView = null;
    }

    @Override
    public void onPageLoaded() {
        if (resourceWebView != null) {
            resourceWebView.hideProgress();
        }
    }

    @Override
    public void close() {
        if (resourceWebView != null) {
            resourceWebView.clear();
            resourceWebView = null;
        }
        finish();
    }

    @Override
    public int getMode() {
        return mode;
    }
}
