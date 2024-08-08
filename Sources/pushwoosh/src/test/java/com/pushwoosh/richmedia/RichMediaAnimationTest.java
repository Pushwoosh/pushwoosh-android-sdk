package com.pushwoosh.richmedia;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.pushwoosh.richmedia.animation.RichMediaAnimation;
import com.pushwoosh.richmedia.animation.RichMediaAnimationCrossFade;
import com.pushwoosh.richmedia.animation.RichMediaAnimationSlideBottom;
import com.pushwoosh.richmedia.animation.RichMediaAnimationSlideLeft;
import com.pushwoosh.richmedia.animation.RichMediaAnimationSlideRight;
import com.pushwoosh.richmedia.animation.RichMediaAnimationSlideTop;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class RichMediaAnimationTest {

    public static final int PARENT_HEIGHT = 100;
    public static final int PARENT_WIDTH = 100;

    @Mock
    private View view;
    @Mock
    private View parent;
    @Mock
    private Animation.AnimationListener listener;


    @Captor
    ArgumentCaptor<Animation> animationArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(parent.getHeight()).thenReturn(PARENT_HEIGHT);
        Mockito.when(parent.getWidth()).thenReturn(PARENT_HEIGHT);
    }

    @Test
    public void openAnimationTopSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideTop();
        richMediaAnimation.openAnimation(view, parent);
        assertStartTranslateAnimation(0, 0, -PARENT_HEIGHT, 0);
    }

    private void assertStartTranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {
        Mockito.verify(view).startAnimation(animationArgumentCaptor.capture());
        assertTranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
    }

    private void assertTranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {
        Animation animation = animationArgumentCaptor.getValue();
        Assert.assertTrue(animation instanceof TranslateAnimation);

        float mFromXValue = (float) WhiteboxHelper.getInternalState(animation, "mFromXValue");
        Assert.assertEquals(fromXDelta, mFromXValue, 0);
        float mFromYValue = (float) WhiteboxHelper.getInternalState(animation, "mFromYValue");
        Assert.assertEquals(fromYDelta, mFromYValue, 0);
        float mToYValue = (float) WhiteboxHelper.getInternalState(animation, "mToYValue");
        Assert.assertEquals(toYDelta, mToYValue, 0);
        float mToXValue = (float) WhiteboxHelper.getInternalState(animation, "mToXValue");
        Assert.assertEquals(toXDelta, mToXValue, 0);

    }

    @Test
    public void openAnimationBottomSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideBottom();
        richMediaAnimation.openAnimation(view, parent);
        assertStartTranslateAnimation(0, 0, PARENT_HEIGHT, 0);

    }

    @Test
    public void openAnimationLeftSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideLeft();
        richMediaAnimation.openAnimation(view, parent);
        assertStartTranslateAnimation(-PARENT_WIDTH, 0, 0, 0);
    }

    @Test
    public void openAnimationRightSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideRight();
        richMediaAnimation.openAnimation(view, parent);
        assertStartTranslateAnimation(PARENT_WIDTH, 0, 0, 0);
    }

    @Test
    public void openAnimationCrossFadeShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationCrossFade();

        richMediaAnimation.openAnimation(view, parent);

        Mockito.verify(view).startAnimation(animationArgumentCaptor.capture());
        Assert.assertTrue(animationArgumentCaptor.getValue() instanceof AlphaAnimation);
    }

    @Test
    public void closeAnimationCrossFadeShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationCrossFade();

        richMediaAnimation.closeAnimation(view, parent, listener);

        Mockito.verify(view).startAnimation(animationArgumentCaptor.capture());
        Assert.assertTrue(animationArgumentCaptor.getValue() instanceof AlphaAnimation);
    }

    private void assertCloseTranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {
        Mockito.verify(view).startAnimation(animationArgumentCaptor.capture());
        assertTranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
    }

    @Test
    public void closeAnimationTopSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideTop();
        richMediaAnimation.closeAnimation(view, parent, listener);
        assertCloseTranslateAnimation(0, 0, 0, -PARENT_HEIGHT);
    }

    @Test
    public void closeAnimationBottomSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideBottom();
        richMediaAnimation.closeAnimation(view, parent, listener);
        assertCloseTranslateAnimation(0, 0, 0, PARENT_HEIGHT);
    }

    @Test
    public void closeAnimationRightSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideRight();
        richMediaAnimation.closeAnimation(view, parent, listener);
        assertCloseTranslateAnimation(0, PARENT_WIDTH, 0, 0);
    }

    @Test
    public void closeAnimationLeftSlideShouldStartTranslateAnimation() {
        RichMediaAnimation richMediaAnimation = new RichMediaAnimationSlideLeft();
        richMediaAnimation.closeAnimation(view, parent, listener);
        assertCloseTranslateAnimation(0, -PARENT_WIDTH, 0, 0);
    }

}