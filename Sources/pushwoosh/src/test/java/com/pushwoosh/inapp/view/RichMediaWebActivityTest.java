package com.pushwoosh.inapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.businesscases.BusinessCasesManager;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.richmedia.animation.RichMediaAnimationSlideTop;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class RichMediaWebActivityTest {

    private RichMediaWebActivity richMediaWebActivity;

    @Mock
    private ResourceWebView resourceWebView;
    @Captor
    private ArgumentCaptor<Animation.AnimationListener> listenerCaptor;
    @Mock
    Animation animation;

    private PlatformTestManager platformTestManager;
    private RichMediaStyle richMediaStyle;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        MockitoAnnotations.initMocks(this);
        richMediaStyle = new RichMediaStyle(0, new RichMediaAnimationSlideTop());
        buildActivity();
    }

    private void buildActivity() {
        Intent intent = new Intent();
        Resource resource = new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1, BusinessCasesManager.PUSH_RECOVER_CASE, "");
        Intent intentWithContent = WebActivity.applyIntentParams(intent, resource, "123", 0);
        richMediaWebActivity = Robolectric
                .buildActivity(RichMediaWebActivity.class, intentWithContent)
                .create(intentWithContent.getExtras())
                .create().get();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void closeAnimatedNotCallAnimationTwice() {
        richMediaWebActivity.updateWebView(resourceWebView);

        richMediaWebActivity.close();
        richMediaWebActivity.close();
        richMediaWebActivity.close();

        verify(resourceWebView, times(1)).animateClose(any());
    }

    @Test
    public void onBackButtonShouldNotWorkBeforeDelay() {
        PushwooshPlatform.getInstance().getRichMediaStyle().setTimeOutBackButtonEnable(1000);
        buildActivity();
        richMediaWebActivity.updateWebView(resourceWebView);

        richMediaWebActivity.onBackPressed();

        verify(resourceWebView, never()).animateClose(any());
    }

    @Test
    public void onBackButtonShouldWorkAfterDelay() {
        PushwooshPlatform.getInstance().getRichMediaStyle().setTimeOutBackButtonEnable(1000);
        buildActivity();
        richMediaWebActivity.updateWebView(resourceWebView);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        richMediaWebActivity.onBackPressed();

        verify(resourceWebView).animateClose(any());
    }

    @Test
    public void afterCloseAnimationResourceWebViewShouldGone() {
        richMediaWebActivity.updateWebView(resourceWebView);

        richMediaWebActivity.close();
        verify(resourceWebView).animateClose(listenerCaptor.capture());
        Animation.AnimationListener listener = listenerCaptor.getValue();
        listener.onAnimationEnd(animation);

        verify(resourceWebView).setVisibility(View.GONE);
    }

    @Test
    @Ignore
    public void onCreateShouldClosedIfInStateWasSavedKeyClosed() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("is_closed", true);

        richMediaWebActivity.onCreate(bundle);

        verify(richMediaWebActivity).finish();
        //todo make refactoring and fix test
    }

    @Test
    public void onSaveInstanceStateShouldSaveAnimationSate(){
        Bundle bundle = new Bundle();
        WhiteboxHelper.setInternalState(richMediaWebActivity, "isAnimated", true);
        WhiteboxHelper.setInternalState(richMediaWebActivity, "isAnimatedClose", true);

        richMediaWebActivity.onSaveInstanceState(bundle);

        Assert.assertNotEquals(true, "IS_ANIMATED");
        Assert.assertNotEquals(true, "IS_CLOSED");
    }
}