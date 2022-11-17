package com.pushwoosh.richmedia.animation;

import android.view.View;
import android.view.animation.Animation;

public abstract class RichMediaAnimationBase implements RichMediaAnimation {
    public static final int DURATION_MILLIS = 300;

    @Override
    public void openAnimation(View contentView, View parentView) {
        Animation animation = getOpenAnimation(parentView);
        if (animation != null) {
            animation.setDuration(DURATION_MILLIS);
            if (contentView != null) {
                contentView.startAnimation(animation);
            }
        }
    }

    @Override
    public void closeAnimation(View contentView, View parentView, Animation.AnimationListener listener) {
        Animation animation = getCloseAnimation(parentView);
        if (animation != null) {
            animation.setDuration(DURATION_MILLIS);
            animation.setAnimationListener(listener);
            contentView.startAnimation(animation);
        }
    }

    abstract Animation getOpenAnimation(View parentView);

    abstract Animation getCloseAnimation(View parentView);
}
