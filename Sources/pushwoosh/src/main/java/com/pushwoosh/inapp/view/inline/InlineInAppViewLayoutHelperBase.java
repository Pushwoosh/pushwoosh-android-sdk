package com.pushwoosh.inapp.view.inline;

import android.content.res.Configuration;

class InlineInAppViewLayoutHelperBase {
    interface MeasureCallback {
        void setDesiredSize(int width, int height);
    }
    protected InlineInAppView inappView;
    protected InlineInAppViewAnimationHelper inlineInAppViewAnimationHelper;

    public InlineInAppViewLayoutHelperBase(InlineInAppView view, InlineInAppViewAnimationHelper animationHelper) {
        inappView = view;
        inlineInAppViewAnimationHelper = animationHelper;
    }

    protected void relayout() {
        inappView.getWebView().forceLayout();
        inappView.forceLayout();
        if (inappView.getParent() != null)
            inappView.getParent().requestLayout();
    }

    public void stateChanged(InlineInAppView.State state) {
        if (state == InlineInAppView.State.CLOSED ||
                state == InlineInAppView.State.RENDERED) {
            inlineInAppViewAnimationHelper.addTransition();
            relayout();
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {

    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec, MeasureCallback measureCallback) {
        if (inappView.getState() == InlineInAppView.State.CLOSED || inappView.getState() == InlineInAppView.State.LOADING) {
            measureCallback.setDesiredSize(0, 0);
        }
    }

    protected void onWebViewLayout(boolean changed, int l, int t, int r, int b) {

    }
}
