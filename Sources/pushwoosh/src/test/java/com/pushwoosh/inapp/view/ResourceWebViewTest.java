package com.pushwoosh.inapp.view;

import static com.pushwoosh.inapp.network.model.InAppLayout.FULLSCREEN;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.view.js.PushwooshJSInterface;
import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.richmedia.animation.RichMediaAnimation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class ResourceWebViewTest {

    private ResourceWebView resourceWebView;

    private Context context;

    @Mock
    RichMediaAnimation richMediaAnimation;

    @Mock
    Animation.AnimationListener listener;

    RichMediaStyle richMediaStyle;

    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        richMediaStyle = new RichMediaStyle(0, richMediaAnimation);
        context = Mockito.spy(RuntimeEnvironment.application);
        richMediaStyle.setRichMediaAnimation(richMediaAnimation);
        resourceWebView = new ResourceWebView(context, FULLSCREEN, richMediaStyle, false);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Verifies that loadData appends a trailing slash to the baseUrl and injects the Pushwoosh JS bridge.
    @Test
    public void loadData_baseUrlWithoutSlash_appendsSlashAndInjectsJs() {
        ResourceWebView spy = Mockito.spy(resourceWebView);
        doNothing().when(spy).loadDataWithBaseURL(anyString(), anyString(), anyString(), anyString(), isNull());

        HtmlData htmlData = new HtmlData("code1", "https://example.com", "<html><head></head><body>hi</body></html>");

        spy.loadData(htmlData);

        verify(spy)
                .loadDataWithBaseURL(
                        eq("https://example.com/"),
                        contains(PushwooshJSInterface.PUSHWOOSH_JS),
                        eq("text/html"),
                        eq("UTF-8"),
                        isNull());
    }

    // Verifies that loadData does not duplicate the trailing slash when the baseUrl already ends with one.
    @Test
    public void loadData_baseUrlWithTrailingSlash_notDuplicated() {
        ResourceWebView spy = Mockito.spy(resourceWebView);
        doNothing().when(spy).loadDataWithBaseURL(anyString(), anyString(), anyString(), anyString(), isNull());

        HtmlData htmlData = new HtmlData("code2", "https://example.com/", "<html><head></head><body>hi</body></html>");

        spy.loadData(htmlData);

        verify(spy)
                .loadDataWithBaseURL(eq("https://example.com/"), anyString(), eq("text/html"), eq("UTF-8"), isNull());
    }

    // Verifies file access is disabled on load (synthetic https origin makes file:// unnecessary).
    @Test
    public void loadDataWithBaseURL_disablesFileAccess() {
        WebView webView = resourceWebView.webView;
        webView.getSettings().setAllowFileAccess(true);

        resourceWebView.loadDataWithBaseURL(
                "https://appassets.androidplatform.net/pushwoosh_richmedia/code/",
                "<html></html>",
                "text/html",
                "UTF-8",
                null);

        org.junit.Assert.assertFalse(webView.getSettings().getAllowFileAccess());
    }

    // Verifies mixed-content is set to COMPATIBILITY so http:// sub-resources render on the https origin.
    @Test
    public void initWebView_setsMixedContentCompatibilityMode() {
        WebSettings settings = resourceWebView.webView.getSettings();

        org.junit.Assert.assertEquals(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE, settings.getMixedContentMode());
    }
}
