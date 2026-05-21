package com.pushwoosh.inapp.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.richmedia.animation.RichMediaAnimationSlideTop;
import com.pushwoosh.testutil.EventListenerWrapper;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
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
    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        mocks = MockitoAnnotations.openMocks(this);
        richMediaStyle = new RichMediaStyle(0, new RichMediaAnimationSlideTop());
        EventBus.clearSubscribersMap();
        buildActivity();
    }

    private void buildActivity() {
        Intent intent = new Intent();
        Resource resource = new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1);
        Intent intentWithContent = WebActivity.applyIntentParams(intent, resource, "123", 0);
        richMediaWebActivity = Robolectric.buildActivity(RichMediaWebActivity.class, intentWithContent)
                .create(intentWithContent.getExtras())
                .create()
                .get();
    }

    private RichMediaWebActivity buildActivityWith(Resource resource) {
        Intent intent = new Intent();
        Intent intentWithContent = WebActivity.applyIntentParams(intent, resource, "123", 0);
        return Robolectric.buildActivity(RichMediaWebActivity.class, intentWithContent)
                .create(intentWithContent.getExtras())
                .create()
                .get();
    }

    @After
    public void tearDown() throws Exception {
        EventBus.clearSubscribersMap();
        platformTestManager.tearDown();
        if (mocks != null) {
            mocks.close();
        }
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

    // Verifies that createRichMediaLockScreenIntent assembles intent with lockscreen mode, sound and required activity
    // flags.
    @Test
    public void createRichMediaLockScreenIntent_buildsIntentWithModeSoundAndFlags() {
        Resource resource = new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1);

        Intent intent = RichMediaWebActivity.createRichMediaLockScreenIntent(
                RuntimeEnvironment.getApplication(), resource, "ringtone");

        ComponentName component = intent.getComponent();
        Assert.assertNotNull(component);
        Assert.assertEquals(RichMediaWebActivity.class.getName(), component.getClassName());
        Assert.assertEquals("ringtone", intent.getStringExtra(WebActivity.EXTRA_SOUND));
        Assert.assertEquals(
                InAppView.MODE_LOCKSCREEN, intent.getIntExtra(WebActivity.EXTRA_MODE, InAppView.MODE_DEFAULT));
        int flags = intent.getFlags();
        Assert.assertTrue((flags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        Assert.assertTrue((flags & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        Assert.assertTrue((flags & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0);
        Assert.assertEquals(resource, intent.getSerializableExtra(WebActivity.EXTRA_INAPP));
    }

    // Verifies that onCreate restoring state with KEY_IS_CLOSED finishes the activity immediately.
    @Test
    public void onCreate_savedStateClosed_finishesActivity() {
        Intent intent = new Intent();
        Resource resource = new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1);
        Intent intentWithContent = WebActivity.applyIntentParams(intent, resource, "123", 0);
        Bundle savedState = new Bundle(intentWithContent.getExtras());
        savedState.putBoolean("IS_CLOSED", true);

        ActivityController<RichMediaWebActivity> controller = Robolectric.buildActivity(
                        RichMediaWebActivity.class, intentWithContent)
                .create(savedState);
        RichMediaWebActivity activity = controller.get();

        Assert.assertTrue(activity.isFinishing());
    }

    // Verifies that successLoadingHtmlData loads content via resourceWebView and injects JS interface into <head>.
    @Test
    public void successLoadingHtmlData_loadsContentAndInjectsJsInterface() {
        richMediaWebActivity.updateWebView(resourceWebView);
        HtmlData htmlData = new HtmlData("code", "https://host/path", "<html><head></head></html>");

        boolean result = richMediaWebActivity.successLoadingHtmlData(htmlData);

        Assert.assertTrue(result);
        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(resourceWebView)
                .loadDataWithBaseURL(
                        baseUrlCaptor.capture(), htmlCaptor.capture(), eq("text/html"), eq("UTF-8"), isNull());
        Assert.assertTrue(baseUrlCaptor.getValue().endsWith("/"));
        Assert.assertTrue(htmlCaptor.getValue().contains("<script type=\"text/javascript\">"));
    }

    // Verifies that successLoadingHtmlData appends trailing slash when baseUrl does not end with one.
    @Test
    public void successLoadingHtmlData_baseUrlMissingSlash_appendsSlash() {
        richMediaWebActivity.updateWebView(resourceWebView);
        HtmlData htmlData = new HtmlData("code", "https://host/path/nodash", "<html><head></head></html>");

        richMediaWebActivity.successLoadingHtmlData(htmlData);

        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(resourceWebView)
                .loadDataWithBaseURL(baseUrlCaptor.capture(), any(), eq("text/html"), eq("UTF-8"), isNull());
        Assert.assertEquals("https://host/path/nodash/", baseUrlCaptor.getValue());
    }

    // Verifies that successLoadingHtmlData skips loading when the same HtmlData was already shown.
    @Test
    public void successLoadingHtmlData_sameDataTwice_secondCallReturnsFalse() {
        richMediaWebActivity.updateWebView(resourceWebView);
        HtmlData htmlData = new HtmlData("code", "https://host/path", "<html><head></head></html>");

        boolean firstResult = richMediaWebActivity.successLoadingHtmlData(htmlData);
        boolean secondResult = richMediaWebActivity.successLoadingHtmlData(htmlData);

        Assert.assertTrue(firstResult);
        Assert.assertFalse(secondResult);
        verify(resourceWebView, times(1)).loadDataWithBaseURL(any(), any(), eq("text/html"), eq("UTF-8"), isNull());
    }

    // Verifies that failedLoadingHtmlData fires InAppViewFailedEvent and closes the activity for in-app resource.
    @Test
    public void failedLoadingHtmlData_inAppResource_firesFailedEventAndCloses() {
        richMediaWebActivity.updateWebView(resourceWebView);
        EventListener<InAppViewFailedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(InAppViewFailedEvent.class, listener);

        richMediaWebActivity.failedLoadingHtmlData(new ResourceParseException("boom"));

        verify(listener, timeout(500).times(1)).onReceive(any(InAppViewFailedEvent.class));
        verify(resourceWebView).animateClose(any());
    }

    // Verifies that failedLoadingHtmlData for non-inApp resource does NOT post InAppViewFailedEvent but still closes.
    @Test
    public void failedLoadingHtmlData_nonInAppResource_doesNotFireFailedEventButCloses() {
        Resource richMediaResource = new Resource("r-code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1);
        RichMediaWebActivity activity = buildActivityWith(richMediaResource);
        activity.updateWebView(resourceWebView);
        EventListener<InAppViewFailedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(InAppViewFailedEvent.class, listener);

        activity.failedLoadingHtmlData(new ResourceParseException("boom"));

        ShadowLooper.idleMainLooper();
        verify(listener, never()).onReceive(any(InAppViewFailedEvent.class));
        verify(resourceWebView).animateClose(any());
    }

    // Verifies that resourceChanged updates sound/mode/isSoundPlayed and replaces the in-app fragment.
    @Test
    public void resourceChanged_updatesStateAndReplacesFragment() {
        Resource newResource = new Resource("code2", "url2", "hash2", 0, InAppLayout.FULLSCREEN, null, true, 1);

        richMediaWebActivity.resourceChanged(newResource, "s2", InAppView.MODE_LOCKSCREEN);

        Assert.assertEquals("s2", WhiteboxHelper.getInternalState(richMediaWebActivity, "sound"));
        Assert.assertEquals(InAppView.MODE_LOCKSCREEN, WhiteboxHelper.getInternalState(richMediaWebActivity, "mode"));
        Assert.assertEquals(false, WhiteboxHelper.getInternalState(richMediaWebActivity, "isSoundPlayed"));
        Assert.assertEquals(false, WhiteboxHelper.getInternalState(richMediaWebActivity, "viewTracked"));
        Assert.assertNotNull(richMediaWebActivity
                .getFragmentManager()
                .findFragmentByTag("RichMediaWebActivitypushwoosh.inAppFragment"));
    }

    // Verifies that onDestroy posts exactly one RichMediaCloseEvent through sendClose.
    @Test
    public void onDestroy_postsSingleRichMediaCloseEvent() {
        Intent intent = new Intent();
        Resource resource = new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1);
        Intent intentWithContent = WebActivity.applyIntentParams(intent, resource, "123", 0);
        ActivityController<RichMediaWebActivity> controller = Robolectric.buildActivity(
                        RichMediaWebActivity.class, intentWithContent)
                .create(intentWithContent.getExtras());
        EventListener<RichMediaCloseEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(RichMediaCloseEvent.class, listener);

        controller.destroy();

        verify(listener, timeout(500).times(1)).onReceive(any(RichMediaCloseEvent.class));
    }

    // Verifies that sendClose is idempotent: close() + onDestroy together still post a single RichMediaCloseEvent.
    @Test
    public void sendClose_isIdempotentAcrossCloseAndDestroy() {
        Intent intent = new Intent();
        Resource resource = new Resource("code1", "url1", "hash1", 0, InAppLayout.FULLSCREEN, null, true, 1);
        Intent intentWithContent = WebActivity.applyIntentParams(intent, resource, "123", 0);
        ActivityController<RichMediaWebActivity> controller = Robolectric.buildActivity(
                        RichMediaWebActivity.class, intentWithContent)
                .create(intentWithContent.getExtras());
        RichMediaWebActivity activity = controller.get();
        activity.updateWebView(resourceWebView);
        EventListener<RichMediaCloseEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(RichMediaCloseEvent.class, listener);

        activity.close();
        verify(resourceWebView).animateClose(listenerCaptor.capture());
        listenerCaptor.getValue().onAnimationEnd(animation);
        controller.destroy();

        ShadowLooper.idleMainLooper();
        verify(listener, times(1)).onReceive(any(RichMediaCloseEvent.class));
    }
}
