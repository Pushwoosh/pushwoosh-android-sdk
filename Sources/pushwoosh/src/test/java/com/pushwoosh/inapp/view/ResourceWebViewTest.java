package com.pushwoosh.inapp.view;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebView;

import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.richmedia.animation.RichMediaAnimation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.pushwoosh.inapp.network.model.InAppLayout.FULLSCREEN;
import static org.mockito.Matchers.any;

@RunWith(RobolectricTestRunner.class)
public class ResourceWebViewTest {

    private ResourceWebView resourceWebView;

    private Context context;

    @Mock
    RichMediaAnimation richMediaAnimation;
    @Mock
    Animation.AnimationListener listener;

    RichMediaStyle richMediaStyle;

    View contentView;
    WebView webView;

    @Before
    public void setUp() throws Exception {
        richMediaStyle = new RichMediaStyle(0, richMediaAnimation);
        MockitoAnnotations.initMocks(this);
        context = Mockito.spy(RuntimeEnvironment.application);
        richMediaStyle.setRichMediaAnimation(richMediaAnimation);
        resourceWebView = new ResourceWebView(context, FULLSCREEN, richMediaStyle, false);
        contentView = Whitebox.getInternalState(resourceWebView, "container");
        webView = Whitebox.getInternalState(resourceWebView, "webView");
    }

    @Test
    public void animateOpen() {
        resourceWebView.animateOpen();
        resourceWebView.animateOpen();

        Mockito.verify(richMediaAnimation).openAnimation(webView,contentView);
    }

    @Test
    public void animateClose() {
        resourceWebView.animateClose(listener);

        Mockito.verify(richMediaAnimation).closeAnimation(webView,contentView,listener);
    }
}