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
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation a) {
                    if (contentView != null && contentView.isAttachedToWindow()) {
                        contentView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }
                }

                @Override
                public void onAnimationEnd(Animation a) {
                    if (contentView != null && contentView.isAttachedToWindow()) {
                        contentView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation a) {}
            });
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
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation a) {
                    if (contentView != null && contentView.isAttachedToWindow()) {
                        contentView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }
                    if (listener != null) {
                        listener.onAnimationStart(a);
                    }
                }

                @Override
                public void onAnimationEnd(Animation a) {
                    if (contentView != null && contentView.isAttachedToWindow()) {
                        contentView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    if (listener != null) {
                        listener.onAnimationEnd(a);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation a) {
                    if (listener != null) {
                        listener.onAnimationRepeat(a);
                    }
                }
            });
            if (contentView != null) {
                contentView.startAnimation(animation);
            }
        }
    }

    abstract Animation getOpenAnimation(View parentView);

    abstract Animation getCloseAnimation(View parentView);
}
