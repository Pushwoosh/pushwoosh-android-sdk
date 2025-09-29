package com.pushwoosh.inapp.view.config;

import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModalRichmediaConfig {
    private ModalRichMediaViewPosition viewPosition = null;
    private Set<ModalRichMediaSwipeGesture> swipeGestures = null;
    private ModalRichMediaPresentAnimationType presentAnimationType = null;
    private ModalRichMediaDismissAnimationType dismissAnimationType = null;
    private ModalRichMediaWindowWidth windowWidth = null;
    private Integer animationDuration = null;
    private Boolean statusBarCovered = null;
    private Boolean respectEdgeToEdgeLayout = null;

    public Boolean isStatusBarCovered() {
        return statusBarCovered;
    }

    public ModalRichmediaConfig setStatusBarCovered(Boolean statusBarCovered) {
        this.statusBarCovered = statusBarCovered;
        return this;
    }

    public Boolean shouldRespectEdgeToEdgeLayout() {
        return respectEdgeToEdgeLayout;
    }

    public ModalRichmediaConfig setRespectEdgeToEdgeLayout(Boolean respectEdgeToEdgeLayout) {
        this.respectEdgeToEdgeLayout = respectEdgeToEdgeLayout;
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


    public ModalRichmediaConfig setSwipeGestures(Set<ModalRichMediaSwipeGesture> gestures) {
        if (gestures == null) {
            this.swipeGestures = null;
        } else {
            this.swipeGestures = new HashSet<>(gestures);
            this.swipeGestures.remove(ModalRichMediaSwipeGesture.NONE);
        }
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


    public Set<ModalRichMediaSwipeGesture> getSwipeGestures() {
        return swipeGestures == null ? Collections.emptySet() : Collections.unmodifiableSet(swipeGestures);
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

    public Integer getAnimationDuration() {
        return animationDuration;
    }

    public ModalRichmediaConfig setAnimationDuration(Integer animationDuration) {
        this.animationDuration = animationDuration;
        return this;
    }
}
