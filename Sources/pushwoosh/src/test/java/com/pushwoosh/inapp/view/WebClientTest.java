package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.utils.PWLog;
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
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class WebClientTest {

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

    private AutoCloseable mocks;
    private MockedStatic<PushwooshPlatform> pushwooshPlatformStatic;
    private MockedStatic<RepositoryModule> repositoryModuleStatic;

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

        when(webView.getContext()).thenReturn(application);
        when(inAppView.getMode()).thenReturn(InAppView.MODE_DEFAULT);

        webClient = new WebClient(inAppView, resource);
    }

    @After
    public void tearDown() throws Exception {
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

    // Verifies that pushwoosh://close closes the in-app view and returns true.
    @Test
    public void shouldOverrideUrlLoading_pushwooshClose_closesInAppView() {
        boolean handled = webClient.shouldOverrideUrlLoading(webView, "pushwoosh://close");

        assertTrue(handled);
        verify(inAppView).close();
    }

    // Verifies that pushwoosh://open in default (non-lockscreen) mode does not close view or launch activity.
    @Test
    public void shouldOverrideUrlLoading_pushwooshOpenDefaultMode_doesNothing() {
        when(inAppView.getMode()).thenReturn(InAppView.MODE_DEFAULT);

        boolean handled = webClient.shouldOverrideUrlLoading(webView, "pushwoosh://open");

        assertTrue(handled);
        verify(inAppView, never()).close();
        assertNull(Shadows.shadowOf(application).getNextStartedActivity());
    }

    // Verifies that pushwoosh://open in lockscreen mode launches default launcher activity and closes view.
    @Test
    public void shouldOverrideUrlLoading_pushwooshOpenLockscreenMode_launchesDefaultActivityAndCloses() {
        when(inAppView.getMode()).thenReturn(InAppView.MODE_LOCKSCREEN);

        PackageManager pm = application.getPackageManager();
        ShadowPackageManager shadowPm = Shadows.shadowOf(pm);
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setPackage(application.getPackageName());
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = application.getPackageName();
        info.activityInfo.name = "FakeLauncherActivity";
        shadowPm.addResolveInfoForIntent(launchIntent, info);

        boolean handled = webClient.shouldOverrideUrlLoading(webView, "pushwoosh://open");

        assertTrue(handled);
        verify(inAppView).close();

        ShadowApplication shadowApp = Shadows.shadowOf(application);
        Intent started = shadowApp.getNextStartedActivity();
        assertNotNull("expected default launcher intent to be started", started);
        assertNotNull(started.getCategories());
        assertTrue(started.getCategories().contains("android.intent.category.LAUNCHER"));
        assertTrue((started.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
    }

    // Verifies that an unknown pushwoosh method logs an error and does nothing else.
    @Test
    public void shouldOverrideUrlLoading_unknownPushwooshMethod_logsErrorAndReturnsTrue() {
        try (MockedStatic<PWLog> pwLog = Mockito.mockStatic(PWLog.class)) {
            boolean handled = webClient.shouldOverrideUrlLoading(webView, "pushwoosh://unknown");

            assertTrue(handled);
            verify(inAppView, never()).close();
            assertNull(Shadows.shadowOf(application).getNextStartedActivity());
            pwLog.verify(() -> PWLog.error(anyString(), contains("Unrecognized pushwoosh method")));
        }
    }

    // Verifies that pushwoosh URL without host logs a wrong-url-format error and returns true.
    @Test
    public void shouldOverrideUrlLoading_pushwooshWithoutHost_logsWrongFormatError() {
        try (MockedStatic<PWLog> pwLog = Mockito.mockStatic(PWLog.class)) {
            boolean handled = webClient.shouldOverrideUrlLoading(webView, "pushwoosh:");

            assertTrue(handled);
            verify(inAppView, never()).close();
            assertNull(Shadows.shadowOf(application).getNextStartedActivity());
            pwLog.verify(() -> PWLog.error(anyString(), contains("Wrong url format")));
        }
    }

    // Verifies that http URL in default mode opens external ACTION_VIEW intent and closes the in-app.
    @Test
    public void shouldOverrideUrlLoading_httpUrlDefaultMode_opensIntentAndCloses() {
        when(inAppView.getMode()).thenReturn(InAppView.MODE_DEFAULT);

        boolean handled = webClient.shouldOverrideUrlLoading(webView, "https://example.com/page");

        assertTrue(handled);
        verify(inAppView).close();

        Intent started = Shadows.shadowOf(application).getNextStartedActivity();
        assertNotNull("expected ACTION_VIEW intent to be started", started);
        assertEquals(Intent.ACTION_VIEW, started.getAction());
        assertEquals(Uri.parse("https://example.com/page"), started.getData());
        assertTrue((started.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
    }

    // Verifies that http URL in lockscreen mode caches URL via LockScreenMediaStorage and closes view.
    @Test
    public void shouldOverrideUrlLoading_httpUrlLockscreenMode_cachesRemoteUrl() {
        when(inAppView.getMode()).thenReturn(InAppView.MODE_LOCKSCREEN);

        boolean handled = webClient.shouldOverrideUrlLoading(webView, "https://example.com/page");

        assertTrue(handled);
        verify(inAppView).close();
        verify(lockScreenMediaStorage).cacheRemoteUrl(Uri.parse("https://example.com/page"));
        assertNull(Shadows.shadowOf(application).getNextStartedActivity());
    }

    // Verifies that file:// URL is treated as local and triggers no close or intent.
    @Test
    public void shouldOverrideUrlLoading_fileUrl_doesNothing() {
        boolean handled = webClient.shouldOverrideUrlLoading(webView, "file:///android_asset/index.html");

        assertTrue(handled);
        verify(inAppView, never()).close();
        assertNull(Shadows.shadowOf(application).getNextStartedActivity());
        verify(lockScreenMediaStorage, never()).cacheRemoteUrl(any());
    }
}
