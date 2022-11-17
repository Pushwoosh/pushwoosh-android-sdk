package com.pushwoosh.richmedia;

import android.view.View;

import com.pushwoosh.richmedia.animation.RichMediaAnimation;

public class RichMediaStyle {

    public interface LoadingViewCreatorInterface {
        View createLoadingView();
    }

    private int backgroundColor;
    private RichMediaAnimation richMediaAnimation;
    private LoadingViewCreatorInterface loadingViewCreator;
    private long timeOutBackButtonEnable;



    public RichMediaStyle(int backgroundColor, RichMediaAnimation richMediaAnimation) {
        this.backgroundColor = backgroundColor;
        this.richMediaAnimation = richMediaAnimation;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public RichMediaAnimation getRichMediaAnimation() {
        return richMediaAnimation;
    }

    public void setRichMediaAnimation(RichMediaAnimation richMediaAnimation) {
        this.richMediaAnimation = richMediaAnimation;
    }

    public void setLoadingViewCreator(LoadingViewCreatorInterface createLoadingView) {
        this.loadingViewCreator = createLoadingView;
    }

    public LoadingViewCreatorInterface getLoadingViewCreator() {
        return loadingViewCreator;
    }

    public long getTimeOutBackButtonEnable() {
        return timeOutBackButtonEnable;
    }

    public void setTimeOutBackButtonEnable(long timeOutBackButtonEnable) {
        this.timeOutBackButtonEnable = timeOutBackButtonEnable;
    }
}
