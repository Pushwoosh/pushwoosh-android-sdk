package com.pushwoosh.liveupdates;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mockStatic;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.chain.Chain;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.liveupdates.internal.DefaultProgressStyleProvider;
import com.pushwoosh.liveupdates.internal.LiveUpdateNotificationRenderer;
import com.pushwoosh.liveupdates.internal.LiveUpdatePushHandler;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class LiveUpdatesPluginTest {

    private static final String META_KEY = "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER";

    private MockedStatic<AndroidPlatformModule> platformMock;
    private Application app;

    @Before
    public void setUp() {
        MessageSystemHandleChainProvider.init();
        PushwooshLiveUpdates.installForTest(null);
        app = RuntimeEnvironment.getApplication();
        platformMock = mockStatic(AndroidPlatformModule.class);
        platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(app);
    }

    @After
    public void tearDown() {
        platformMock.close();
    }

    private void setStyleProviderMeta(String fqn) {
        ApplicationInfo ai =
                shadowOf(app.getPackageManager()).getInternalMutablePackageInfo(app.getPackageName()).applicationInfo;
        if (ai.metaData == null) {
            ai.metaData = new Bundle();
        }
        ai.metaData.putString(META_KEY, fqn);
    }

    private LiveUpdateProgressStyleProvider installedProvider() {
        LiveUpdateNotificationRenderer r = PushwooshLiveUpdates.getActiveRenderer();
        assertNotNull(r);
        return r.getProviderForTest();
    }

    @Test
    @Config(sdk = 35)
    public void api35_doesNotRegisterHandler() {
        new LiveUpdatesPlugin().init();
        assertFalse(chainHasLiveUpdateHandler());
    }

    @Test
    @Config(sdk = 36)
    public void api36_registersExactlyOneHandler() {
        new LiveUpdatesPlugin().init();
        assertEquals(1, countLiveUpdateHandlersInChain());
    }

    @Test
    @Config(sdk = 36)
    public void api36_noManifestEntry_installsDefaultProvider() {
        new LiveUpdatesPlugin().init();
        assertTrue(installedProvider() instanceof DefaultProgressStyleProvider);
    }

    @Test
    @Config(sdk = 36)
    public void api36_validManifestEntry_installsThatProvider() {
        setStyleProviderMeta("com.pushwoosh.liveupdates.FakeLiveUpdateStyleProvider");
        new LiveUpdatesPlugin().init();
        assertTrue(installedProvider() instanceof FakeLiveUpdateStyleProvider);
    }

    @Test
    public void resolveClassName_prependsPackageForLeadingDot() {
        // ".Foo" is the manifest shorthand the ManifestValidator already resolves; the runtime must match
        assertEquals(
                "com.example.app.MyStyleProvider",
                LiveUpdatesPlugin.resolveClassName(".MyStyleProvider", "com.example.app"));
    }

    @Test
    public void resolveClassName_leavesFullyQualifiedNameUntouched() {
        assertEquals(
                "com.example.app.MyStyleProvider",
                LiveUpdatesPlugin.resolveClassName("com.example.app.MyStyleProvider", "com.example.app"));
    }

    @Test
    public void resolveClassName_returnsNullForBlankOrAbsent() {
        assertNull(LiveUpdatesPlugin.resolveClassName(null, "com.example.app"));
        assertNull(LiveUpdatesPlugin.resolveClassName("   ", "com.example.app"));
    }

    @Test
    @Config(sdk = 36)
    public void api36_invalidManifestEntry_fallsBackToDefault() {
        setStyleProviderMeta("com.example.DoesNotExist");
        new LiveUpdatesPlugin().init();
        assertTrue(installedProvider() instanceof DefaultProgressStyleProvider);
    }

    @Test
    @Config(sdk = 36)
    public void api36_classNotImplementingInterface_fallsBackToDefault() {
        setStyleProviderMeta("java.lang.Object");
        new LiveUpdatesPlugin().init();
        assertTrue(installedProvider() instanceof DefaultProgressStyleProvider);
    }

    private boolean chainHasLiveUpdateHandler() {
        return countLiveUpdateHandlersInChain() > 0;
    }

    private int countLiveUpdateHandlersInChain() {
        Chain<MessageSystemHandler> chain = MessageSystemHandleChainProvider.getMessageSystemChain();
        Iterator<MessageSystemHandler> it = chain.getIterator();
        int n = 0;
        while (it.hasNext()) {
            if (it.next() instanceof LiveUpdatePushHandler) n++;
        }
        return n;
    }
}
