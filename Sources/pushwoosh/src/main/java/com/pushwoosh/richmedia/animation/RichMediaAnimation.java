package com.pushwoosh.richmedia.animation;

import android.view.View;
import android.view.animation.Animation;

public interface RichMediaAnimation {

   void openAnimation(View contentView, View parentView);

   void closeAnimation(View contentView, View parentView, Animation.AnimationListener endAnimationListener);

}
