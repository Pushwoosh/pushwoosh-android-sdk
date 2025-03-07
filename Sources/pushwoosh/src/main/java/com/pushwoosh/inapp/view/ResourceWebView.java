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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.R;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.view.js.PushwooshJSInterface;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.richmedia.animation.RichMediaAnimation;

@SuppressLint("ViewConstructor")
public class ResourceWebView extends FrameLayout {
    private static final String LIGHT_BLACK_BG = "#40000000";
    private static final int ANIMATION_DURATION = 300;
    private InAppLayout inAppLayout = InAppLayout.DIALOG;

    protected FrameLayout container;
    private View loadingView;
    protected WebView webView;
    private boolean animated;
    private boolean progressVisible = false;
    private Runnable postedShowLoading;
    private RichMediaAnimation richMediaAnimation;
    private Handler handler;
    int backgroundColor;
    boolean isInMultiWindowMode;

    public ResourceWebView(Context context) {
        super(context);
        init(InAppLayout.DIALOG, PushwooshPlatform.getInstance().getRichMediaController().getRichMediaStyle(), context);
    }

    protected ResourceWebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(InAppLayout.DIALOG, PushwooshPlatform.getInstance().getRichMediaController().getRichMediaStyle(), context);
    }

    protected ResourceWebView(@NonNull Context context, InAppLayout inAppLayout, RichMediaStyle richMediaStyle, boolean isInMultiWindowMode) {
        super(context);
        this.isInMultiWindowMode = isInMultiWindowMode;
        init(inAppLayout, richMediaStyle, context);
    }

    private void init(InAppLayout inAppLayout, RichMediaStyle richMediaStyle, Context context) {
        this.inAppLayout = inAppLayout;
        this.richMediaAnimation = richMediaStyle.getRichMediaAnimation();

        backgroundColor = richMediaStyle.getBackgroundColor();
        if (backgroundColor == 0) {
            backgroundColor = Color.parseColor(LIGHT_BLACK_BG);
        }

        container = new FrameLayout(getContext());
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.setLayoutParams(createWebViewParams(inAppLayout, 0));
        container.setBackgroundColor(Color.TRANSPARENT);

        setWebViewDataDirectorySuffixIfNeeded(context);
        initWebView();
        if (richMediaStyle.getLoadingViewCreator() != null) {
            loadingView = richMediaStyle.getLoadingViewCreator().createLoadingView();
        } else {
            loadingView = createDefaultLoadingView(getContext());
        }
        loadingView.setVisibility(GONE);

        container.addView(webView);
        container.addView(loadingView);

        addView(container);
    }

    protected WebView createWebView() {
        return new WebView(getContext());
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void initWebView() {
        webView = createWebView();

        webView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.getSettings().setForceDark(WebSettings.FORCE_DARK_OFF);
            webView.setForceDarkAllowed(false);
        }
        webView.setLayoutParams(createWebViewParams(inAppLayout, getStatusBarHeight()));
        webView.setBackgroundColor(Color.TRANSPARENT);

        // disable text selection
        webView.setLongClickable(false);
        webView.setHapticFeedbackEnabled(false);
    }

    @NonNull
    protected LayoutParams createWebViewParams(InAppLayout mode, int topMargin) {
        LayoutParams layoutParams;
        switch (mode) {
            case FULLSCREEN: {
                layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                break;
            }
            case BOTTOM: {
                layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.BOTTOM;
                break;
            }
            case TOP: {
                layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.TOP;
                if (!isInMultiWindowMode) {
                    layoutParams.topMargin = RepositoryModule.getNotificationPreferences().showFullscreenRichMedia().get() ? 0 : topMargin;
                }
                break;
            }
            case DIALOG: {
                layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER;
                break;
            }
            default: {
                throw new IllegalArgumentException("Unrecognized mode: " + mode.toString());
            }
        }
        return layoutParams;
    }

    public void setWebViewClient(WebClient webViewClient) {
        webViewClient.attachToWebView(webView);
    }

    protected void showProgress() {
        if (progressVisible)
            return;
        progressVisible = true;
        handler = new Handler();
        handler.postDelayed(postedShowLoading = () -> {
            loadingView.setAlpha(0);
            loadingView.setVisibility(VISIBLE);
            loadingView.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
        }, 500);
        webView.setVisibility(INVISIBLE);
    }

    protected void hideProgress() {
        if (!progressVisible)
            return;
        progressVisible = false;

        if (handler != null) {
            if (postedShowLoading != null) {
                handler.removeCallbacks(postedShowLoading);
                postedShowLoading = null;
            }
            handler = null;
        }
        loadingView.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
        webView.setVisibility(VISIBLE);
    }

    protected void animateOpen() {
        if (!animated) {
            animateBackground(container, Color.alpha(0), backgroundColor);

            animated = true;
            if (richMediaAnimation != null) {
                richMediaAnimation.openAnimation(webView, container);
            }
        } else {
            container.setBackgroundColor(backgroundColor);
        }
    }

    protected void animateClose(Animation.AnimationListener listener) {
        animateBackground(container, backgroundColor, Color.alpha(0));

        if (richMediaAnimation != null) {
            richMediaAnimation.closeAnimation(webView, container, listener);
        }
    }

    private void animateBackground(View view, int colorFrom, int colorTo) {
        ValueAnimator backgroundAnimator = ObjectAnimator.ofInt(view, "backgroundColor", colorFrom, colorTo);
        backgroundAnimator.setEvaluator(new ArgbEvaluator());
        backgroundAnimator.setDuration(ANIMATION_DURATION);
        backgroundAnimator.start();
    }

    protected void loadUrl(String url) {
        webView.loadUrl(url);
    }

    protected void loadDataWithBaseURL(String baseUrl, String htmlData, String mimeType, String encoding, String historyUri) {
        webView.getSettings().setAllowFileAccess(true);
        webView.loadDataWithBaseURL(baseUrl, htmlData, mimeType, encoding, historyUri);
    }

    protected void clear() {
        webView.setWebViewClient(null);
        webView = null;
    }

    private View createDefaultLoadingView(Context context) {
        int theme = context.getApplicationInfo().theme;
        if (theme == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                theme = android.R.style.Theme_Material;
            } else {
                theme = android.R.style.Theme_Holo;
            }
        }
        View loadingView = View.inflate(new ContextThemeWrapper(context, theme), R.layout.pw_default_loading_view, null);
        LayoutParams progressParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = Gravity.CENTER;
        loadingView.setLayoutParams(progressParams);

        return loadingView;
    }

    protected void loadData(HtmlData htmlData) {
        String htmlContent = htmlData.getHtmlContent();
        String baseUrl = htmlData.getUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String htmlContentWithPushWooshInterface = htmlContent.replace("<head>", "<head>\n<script type=\"text/javascript\">" + PushwooshJSInterface.PUSHWOOSH_JS + "</script>");
        loadDataWithBaseURL(baseUrl, htmlContentWithPushWooshInterface, "text/html", "UTF-8", null);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // Web-based data directories separated by process
    // https://developer.android.com/about/versions/pie/android-9.0-changes-28
    private void setWebViewDataDirectorySuffixIfNeeded(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return;
            }
            if (!TextUtils.equals(context.getPackageName(), Application.getProcessName())) {
                WebView.setDataDirectorySuffix(Application.getProcessName());
            }
        } catch (Throwable throwable) {
            PWLog.error("Error occurred when tried to set Webview data dirrectory suffix: " +
                    throwable.getMessage());
        }
    }
}
