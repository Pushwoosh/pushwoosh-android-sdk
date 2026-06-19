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
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
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

import java.io.File;
import java.io.FileWriter;
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

    // Verifies an absolute external https URL is not intercepted (loader returns null → network load).
    @Test
    public void shouldInterceptRequest_absoluteHttpsUrl_returnsNullForNetworkLoad() {
        File folder = new File(application.getCacheDir(), "richmedia");
        folder.mkdirs();
        when(inAppFolderProvider.getInAppFolder("CODE")).thenReturn(folder);
        when(webResourceRequest.getUrl()).thenReturn(Uri.parse("https://lh3.googleusercontent.com/x.png"));

        WebResourceResponse response = webClient.shouldInterceptRequest(webView, webResourceRequest);

        assertNull(response);
    }

    // Verifies a relative asset under the synthetic origin is intercepted and served from disk.
    @Test
    public void shouldInterceptRequest_localAsset_servedFromDisk() throws Exception {
        File folder = new File(application.getCacheDir(), "richmedia");
        folder.mkdirs();
        File asset = new File(folder, "img.png");
        try (FileWriter w = new FileWriter(asset)) {
            w.write("PNGDATA");
        }
        when(inAppFolderProvider.getInAppFolder("CODE")).thenReturn(folder);
        when(webResourceRequest.getUrl())
                .thenReturn(Uri.parse("https://appassets.androidplatform.net/pushwoosh_richmedia/CODE/img.png"));

        WebResourceResponse response = webClient.shouldInterceptRequest(webView, webResourceRequest);

        assertNotNull("expected local asset to be intercepted", response);
    }

    // Verifies a folder outside allowed storage degrades to no interception instead of crashing the worker thread.
    @Test
    public void shouldInterceptRequest_folderOutsideAllowedStorage_servesWithoutCrashing() throws Exception {
        File outside =
                java.nio.file.Files.createTempDirectory("outside_datadir").toFile();
        when(inAppFolderProvider.getInAppFolder("CODE")).thenReturn(outside);
        when(webResourceRequest.getUrl())
                .thenReturn(Uri.parse("https://appassets.androidplatform.net/pushwoosh_richmedia/CODE/img.png"));

        WebResourceResponse response = webClient.shouldInterceptRequest(webView, webResourceRequest);

        assertNull(response);
    }

    // Verifies that after release() a lifecycle callback is a no-op: no inAppView callback, no phantom present event.
    @Test
    public void onPageFinished_afterRelease_isNoOp() {
        webClient.release();

        webClient.onPageFinished(webView, "https://appassets.androidplatform.net/pushwoosh_richmedia/CODE/");

        verify(inAppView, never()).onPageLoaded();
    }

    // Verifies navigation to the in-app's own synthetic origin is a no-op: no external intent, no close. Layer 1.
    @Test
    public void shouldOverrideUrlLoading_ownVirtualOrigin_doesNothing() {
        boolean handled = webClient.shouldOverrideUrlLoading(
                webView, "https://appassets.androidplatform.net/pushwoosh_richmedia/CODE/");

        assertTrue(handled);
        verify(inAppView, never()).close();
        assertNull(Shadows.shadowOf(application).getNextStartedActivity());
        verify(lockScreenMediaStorage, never()).cacheRemoteUrl(any());
    }

    // Verifies file:// URLs are still swallowed and never start an activity.
    @Test
    public void shouldOverrideUrlLoading_fileUrl_doesNothing() {
        boolean handled = webClient.shouldOverrideUrlLoading(webView, "file:///android_asset/index.html");

        assertTrue(handled);
        verify(inAppView, never()).close();
        assertNull(Shadows.shadowOf(application).getNextStartedActivity());
        verify(lockScreenMediaStorage, never()).cacheRemoteUrl(any());
    }
}
