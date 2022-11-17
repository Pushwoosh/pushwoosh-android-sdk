package com.pushwoosh.richmedia.animation;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

public class RichMediaAnimationCrossFade extends RichMediaAnimationBase {

    @Override
    public Animation getOpenAnimation(View parentView) {
        return new AlphaAnimation(0,1);
    }

    @Override
    public Animation getCloseAnimation(View parentView) {
        return new AlphaAnimation(1,0);
    }
}
