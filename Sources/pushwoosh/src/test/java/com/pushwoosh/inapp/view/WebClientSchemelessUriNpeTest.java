package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.repository.LockScreenMediaStorage;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;

// Regression guard for crash-webclient-uri-getscheme-null. WebClient.handleUri used to do
// uri.getScheme().equals("pushwoosh") with no null-check; Uri.getScheme() is null for a schemeless URI
// (relative href <a href="foo/bar">, empty href href="", bare fragment), so the system WebView's
// main-thread shouldOverrideUrlLoading callback would NPE and kill the process. The fix early-returns
// (consumes the navigation as a no-op) when the scheme is null. These tests feed a schemeless URL through
// the real public callbacks and assert the graceful no-op: handled==true and no external open / view close.
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class WebClientSchemelessUriNpeTest {

    @Mock
    private InAppView inAppView;

    @Mock
    private Resource resource;

    @Mock
    private WebView webView;

    @Mock
    private WebResourceRequest webResourceRequest;

    @Mock
    private PushwooshPlatform pushwooshPlatform;

    @Mock
    private PushwooshInAppImpl pushwooshInApp;

    @Mock
    private LockScreenMediaStorage lockScreenMediaStorage;

    @Mock
    private InAppFolderProvider inAppFolderProvider;

    private AutoCloseable mocks;
    private MockedStatic<PushwooshPlatform> pushwooshPlatformStatic;
    private MockedStatic<RepositoryModule> repositoryModuleStatic;
    private MockedStatic<InAppModule> inAppModuleStatic;

    private WebClient webClient;
    private Application application;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        application = RuntimeEnvironment.getApplication();

        pushwooshPlatformStatic = Mockito.mockStatic(PushwooshPlatform.class);
        repositoryModuleStatic = Mockito.mockStatic(RepositoryModule.class);

        pushwooshPlatformStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
        when(pushwooshPlatform.pushwooshInApp()).thenReturn(pushwooshInApp);
        when(pushwooshInApp.getJavascriptInterfaces()).thenReturn(Collections.emptyMap());

        repositoryModuleStatic.when(RepositoryModule::getLockScreenMediaStorage).thenReturn(lockScreenMediaStorage);

        inAppModuleStatic = Mockito.mockStatic(InAppModule.class);
        inAppModuleStatic.when(InAppModule::getInAppFolderProvider).thenReturn(inAppFolderProvider);

        when(resource.getCode()).thenReturn("CODE");

        when(webView.getContext()).thenReturn(application);
        when(inAppView.getMode()).thenReturn(InAppView.MODE_DEFAULT);

        webClient = new WebClient(inAppView, resource);
    }

    @After
    public void tearDown() throws Exception {
        if (inAppModuleStatic != null) {
            inAppModuleStatic.close();
        }
        if (repositoryModuleStatic != null) {
            repositoryModuleStatic.close();
        }
        if (pushwooshPlatformStatic != null) {
            pushwooshPlatformStatic.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    // Ground-truth the load-bearing fact: real Robolectric Uri.getScheme() is null for a relative href.
    @Test
    public void uriGetScheme_relativeHref_isNull() {
        assertNull(Uri.parse("foo/bar").getScheme());
        assertNull(Uri.parse("").getScheme());
        assertNull(Uri.parse("#anchor").getScheme());
    }

    // Verifies that a relative href tapped in rich media → shouldOverrideUrlLoading(view, String) → handleUri
    // consumes the schemeless navigation as a no-op (returns true) instead of NPEing, and never opens it
    // externally (inAppView.close() is only called on the external-open path).
    @Test
    public void shouldOverrideUrlLoading_stringOverload_relativeHref_isConsumedAsNoOp() {
        boolean handled = webClient.shouldOverrideUrlLoading(webView, "foo/bar");
        assertTrue(handled);
        verify(inAppView, never()).close();
    }

    // Verifies that an empty href ("") parses to a schemeless URI and is likewise consumed as a no-op.
    @Test
    public void shouldOverrideUrlLoading_stringOverload_emptyHref_isConsumedAsNoOp() {
        boolean handled = webClient.shouldOverrideUrlLoading(webView, "");
        assertTrue(handled);
        verify(inAppView, never()).close();
    }

    // Verifies the WebResourceRequest overload (API >= N): a schemeless request.getUrl() is consumed as a no-op.
    @Test
    public void shouldOverrideUrlLoading_requestOverload_relativeUri_isConsumedAsNoOp() {
        when(webResourceRequest.getUrl()).thenReturn(Uri.parse("foo/bar"));
        boolean handled = webClient.shouldOverrideUrlLoading(webView, webResourceRequest);
        assertTrue(handled);
        verify(inAppView, never()).close();
    }

    // NEGATIVE CONTROL: a URL WITH a scheme does not throw — proves the null scheme is the necessary condition.
    // "https://example.com/page" in default mode opens an ACTION_VIEW intent and closes the view, so close()
    // IS called here — the inverse of the schemeless no-op above, making the close() discriminator explicit.
    @Test
    public void shouldOverrideUrlLoading_urlWithScheme_opensExternallyAndCloses() {
        boolean handled = webClient.shouldOverrideUrlLoading(webView, "https://example.com/page");
        assertTrue(handled);
        verify(inAppView).close();
    }
}
