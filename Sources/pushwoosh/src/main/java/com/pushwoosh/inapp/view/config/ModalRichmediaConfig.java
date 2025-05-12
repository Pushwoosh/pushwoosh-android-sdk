package com.pushwoosh.inapp.view.config;

import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;

public class ModalRichmediaConfig {
    private ModalRichMediaViewPosition viewPosition = ModalRichMediaViewPosition.FULLSCREEN;
    private ModalRichMediaSwipeGesture swipeGesture = ModalRichMediaSwipeGesture.NONE;
    private ModalRichMediaPresentAnimationType presentAnimationType = ModalRichMediaPresentAnimationType.FADE_IN;
    private ModalRichMediaDismissAnimationType dismissAnimationType = ModalRichMediaDismissAnimationType.FADE_OUT;
    private ModalRichMediaWindowWidth windowWidth = ModalRichMediaWindowWidth.FULL_SCREEN;
    private int animationDuration = 1000;
    private boolean statusBarCovered = false;

    public boolean isStatusBarCovered() {
        return statusBarCovered;
    }

    public ModalRichmediaConfig setStatusBarCovered(boolean statusBarCovered) {
        this.statusBarCovered = statusBarCovered;
        return this;
    }

    public ModalRichmediaConfig() {
    }

    public ModalRichmediaConfig setDismissAnimationType(ModalRichMediaDismissAnimationType dismissAnimationType) {
        this.dismissAnimationType = dismissAnimationType;
        return this;
    }

    public ModalRichmediaConfig setPresentAnimationType(ModalRichMediaPresentAnimationType presentAnimationType) {
        this.presentAnimationType = presentAnimationType;
        return this;
    }

    public ModalRichmediaConfig setSwipeGesture(ModalRichMediaSwipeGesture swipeGesture) {
        this.swipeGesture = swipeGesture;
        return this;
    }

    public ModalRichmediaConfig setViewPosition(ModalRichMediaViewPosition viewPosition) {
        this.viewPosition = viewPosition;
        return this;
    }

    public ModalRichmediaConfig setWindowWidth(ModalRichMediaWindowWidth windowWidth) {
        this.windowWidth = windowWidth;
        return this;
    }

    public ModalRichMediaViewPosition getViewPosition() {
        return viewPosition;
    }

    public ModalRichMediaSwipeGesture getSwipeGesture() {
        return swipeGesture;
    }

    public ModalRichMediaPresentAnimationType getPresentAnimationType() {
        return presentAnimationType;
    }

    public ModalRichMediaDismissAnimationType getDismissAnimationType() {
        return dismissAnimationType;
    }

    public ModalRichMediaWindowWidth getWindowWidth() {
        return windowWidth;
    }

    public int getAnimationDuration() {
        return animationDuration;
    }

    public ModalRichmediaConfig setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
        return this;
    }
}
