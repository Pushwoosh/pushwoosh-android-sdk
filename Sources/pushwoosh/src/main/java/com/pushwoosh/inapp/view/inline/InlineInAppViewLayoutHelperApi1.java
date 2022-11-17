package com.pushwoosh.inapp.view.inline;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.webkit.JavascriptInterface;

class InlineInAppViewLayoutHelperApi1 extends InlineInAppViewLayoutHelperBase {
    public static final int RENDER_DURATION = 400;

    public static final String REQUEST_SIZE_SCRIPT = "javascript:pwInlineInappSizeDelegate.resize(document.body.clientWidth, document.body.clientHeight)";

    private int contentWidth;
    private int contentHeight;
    private final Handler requestSizeHandler = new Handler();
    private boolean resetLayout;
    private int webViewWidth;
    private int webViewHeight;

    @SuppressLint("AddJavascriptInterface")
    public InlineInAppViewLayoutHelperApi1(InlineInAppView view, InlineInAppViewAnimationHelper animationHelper) {
        super(view, animationHelper);

        view.getWebView().addJavascriptInterface(new InlineInappSizeInterface(), InlineInappSizeInterface.JS_INTEFACE_NAME);
    }

    protected void onWebViewLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && resetLayout && (r != webViewWidth || b != webViewHeight)) {
            contentWidth = 0;
            contentHeight = 0;
            requestSizeHandler.post(this::requestWebViewSize);
            resetLayout = false;
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webViewWidth = inappView.getWebView().getWidth();
            webViewHeight = inappView.getWebView().getHeight();
            resetLayout = true;
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec, MeasureCallback measureCallback) {
        if (inappView.getState() == InlineInAppView.State.RENDERED && contentWidth > 0 && contentHeight > 0) {
            measureCallback.setDesiredSize(contentWidth, contentHeight);
        } else if (inappView.getState() == InlineInAppView.State.LOADED) {
            measureCallback.setDesiredSize(1, 1);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec, measureCallback);
        }
    }

    @Override
    public void stateChanged(InlineInAppView.State state) {
        super.stateChanged(state);
        if (state == InlineInAppView.State.LOADED) {
            //in pre KITKAT render is very slow and we need to display webView with nonzero size to start drawing,
            //so we need to add an additional delay, and handle LOADED state in onMeasure
            inappView.getContainer().setAlpha(0.01f); //with alpha == 0 got artifacts on webView
            relayout();
            requestSizeHandler.postDelayed(this::requestWebViewSize, RENDER_DURATION);
        }
    }

    private void requestWebViewSize() {
        inappView.getWebView().loadUrl(REQUEST_SIZE_SCRIPT);

        synchronized (requestSizeHandler) {
            requestSizeHandler.postDelayed(this::requestWebViewSize, RENDER_DURATION);
        }
    }

    private class InlineInappSizeInterface {
        static final String JS_INTEFACE_NAME = "pwInlineInappSizeDelegate";

        @JavascriptInterface
        public void resize(final float width, final float height) {
            if (width > 0 && height > 0) {
                synchronized (requestSizeHandler) {
                    if (contentWidth == width && contentHeight == height)
                        return;
                    requestSizeHandler.removeCallbacksAndMessages(null);
                    contentWidth = (int) (width * inappView.getResources().getDisplayMetrics().density);
                    contentHeight = (int) (height * inappView.getResources().getDisplayMetrics().density);
                    requestSizeHandler.post(() -> {
                        if (inappView.getState() == InlineInAppView.State.RENDERED) {
                            relayout();
                        } else {
                            inappView.setState(InlineInAppView.State.RENDERED);
                        }
                    });
                }
            }
        }
    }
}
