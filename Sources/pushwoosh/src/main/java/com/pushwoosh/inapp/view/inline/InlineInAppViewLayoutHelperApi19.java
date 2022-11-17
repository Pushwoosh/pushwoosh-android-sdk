package com.pushwoosh.inapp.view.inline;

import android.os.Build;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.KITKAT)
class InlineInAppViewLayoutHelperApi19 extends InlineInAppViewLayoutHelperBase {
    public InlineInAppViewLayoutHelperApi19(InlineInAppView view, InlineInAppViewAnimationHelper animationHelper) {
        super(view, animationHelper);
    }

    @Override
    public void stateChanged(InlineInAppView.State state) {
        super.stateChanged(state);
        if (state == InlineInAppView.State.LOADED) {
            //force webView to calculate it's content size
            int webViewWidth = inappView.getWebView().getWidth() == 0 ? 1 : inappView.getWebView().getWidth();
            int webViewHeight = inappView.getWebView().getHeight() == 0 ? 1 : inappView.getWebView().getHeight();
            inappView.getWebView().layout(0,0, webViewWidth, webViewHeight);

            inappView.setState(InlineInAppView.State.RENDERED);
        }
    }
}
