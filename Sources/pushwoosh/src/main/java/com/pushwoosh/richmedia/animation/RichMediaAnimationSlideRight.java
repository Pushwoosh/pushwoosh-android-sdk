package com.pushwoosh.richmedia.animation;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

public class RichMediaAnimationSlideRight extends RichMediaAnimationBase {

    @Override
    public Animation getOpenAnimation(View parentView) {
        return new TranslateAnimation(parentView.getWidth(), 0, 0, 0);
    }

    @Override
    public Animation getCloseAnimation(View parentView) {
        return new TranslateAnimation(0, parentView.getWidth(), 0, 0);
    }
}
