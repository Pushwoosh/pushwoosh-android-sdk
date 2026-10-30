package com.pushwoosh.inapp.view;

import static com.pushwoosh.inapp.network.model.InAppLayout.BOTTOM;
import static com.pushwoosh.inapp.network.model.InAppLayout.DIALOG;
import static com.pushwoosh.inapp.network.model.InAppLayout.FULLSCREEN;
import static com.pushwoosh.inapp.network.model.InAppLayout.TOP;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.view.js.PushwooshJSInterface;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaColorScheme;
import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.richmedia.animation.RichMediaAnimation;

import org.junit.After;
import org.junit.Assert;
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

import java.lang.reflect.Field;

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

    private NotificationPrefs originalNotificationPrefs;

    @Before
    public void setUp() throws Exception {
        Field instance = PushwooshPlatform.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        mocks = MockitoAnnotations.openMocks(this);
        richMediaStyle = new RichMediaStyle(0, richMediaAnimation);
        context = Mockito.spy(RuntimeEnvironment.application);
        richMediaStyle.setRichMediaAnimation(richMediaAnimation);
        resourceWebView = new ResourceWebView(context, FULLSCREEN, richMediaStyle, false);

        originalNotificationPrefs = RepositoryModule.getNotificationPreferences();
        NotificationPrefs notificationPrefs = mock(NotificationPrefs.class);
        when(notificationPrefs.showFullscreenRichMedia()).thenReturn(mock(PreferenceBooleanValue.class));
        PreferenceStringValue colorScheme = mock(PreferenceStringValue.class);
        when(colorScheme.get()).thenReturn(RichMediaColorScheme.APP.name());
        when(notificationPrefs.richMediaColorScheme()).thenReturn(colorScheme);
        RepositoryModule.setNotificationPreferences(notificationPrefs);
    }

    @After
    public void tearDown() throws Exception {
        RepositoryModule.setNotificationPreferences(originalNotificationPrefs);
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

    // Verifies the WebView context is wrapped with the color-scheme overlay (light: app theme is 0 here).
    @Test
    public void createWebView_contextIsThemeWrapper_lightByDefault() {
        Context webViewContext = resourceWebView.webView.getContext();

        Assert.assertTrue(webViewContext instanceof ContextThemeWrapper);
        TypedValue value = new TypedValue();
        Assert.assertTrue(webViewContext.getTheme().resolveAttribute(android.R.attr.isLightTheme, value, true));
        Assert.assertTrue(value.data != 0);
    }

    // Verifies the resolved manifest (dark) theme reaches the WebView context on API 29+.
    @Test
    public void createWebView_darkManifestTheme_wrapsDark() {
        RuntimeEnvironment.application.getApplicationInfo().theme = android.R.style.Theme_Material;

        ResourceWebView view = new ResourceWebView(context, FULLSCREEN, richMediaStyle, false);

        TypedValue value = new TypedValue();
        Assert.assertTrue(
                view.webView.getContext().getTheme().resolveAttribute(android.R.attr.isLightTheme, value, true));
        Assert.assertEquals(0, value.data);
    }

    // API <= 28: the framework has no isLightTheme attribute; the wrapper exists but resolves nothing.
    @Test
    @Config(sdk = 28)
    public void createWebView_api28_wrapperPresentAttributeAbsent() {
        Context webViewContext = resourceWebView.webView.getContext();

        Assert.assertTrue(webViewContext instanceof ContextThemeWrapper);
        TypedValue value = new TypedValue();
        Assert.assertFalse(webViewContext.getTheme().resolveAttribute(android.R.attr.isLightTheme, value, true));
    }

    // TOP layout keeps its content below the status bar using the real inset, not a framework resource.
    @Test
    public void topLayout_statusBarInset_becomesWebViewTopMargin() {
        ResourceWebView view = new ResourceWebView(context, TOP, richMediaStyle, false);

        dispatchStatusBarInset(view, 63);

        Assert.assertEquals(63, webViewTopMargin(view));
    }

    // Only TOP reserves room for the status bar; the other layouts must stay edge-to-edge as before.
    @Test
    public void nonTopLayouts_statusBarInset_noTopMargin() {
        for (InAppLayout layout : new InAppLayout[] {FULLSCREEN, BOTTOM, DIALOG}) {
            ResourceWebView view = new ResourceWebView(context, layout, richMediaStyle, false);

            dispatchStatusBarInset(view, 63);

            Assert.assertEquals(layout.name(), 0, webViewTopMargin(view));
        }
    }

    private static void dispatchStatusBarInset(ResourceWebView view, int top) {
        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, top, 0, 0))
                .build();
        ViewCompat.dispatchApplyWindowInsets(view, insets);
    }

    private static int webViewTopMargin(ResourceWebView view) {
        return ((FrameLayout.LayoutParams) view.webView.getLayoutParams()).topMargin;
    }
}
