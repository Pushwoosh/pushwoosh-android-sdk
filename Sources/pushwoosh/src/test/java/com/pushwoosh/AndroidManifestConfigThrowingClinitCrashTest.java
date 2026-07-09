package com.pushwoosh;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Regression guard for crash-androidmanifestconfig-getclass-error-init:
// AndroidManifestConfig.getClass() resolves a manifest-declared class name via the one-arg
// Class.forName (initialize=true), which eagerly runs the class's <clinit>. Before the fix the catch
// at AndroidManifestConfig.java:252 handled only ClassNotFoundException | NoSuchMethodException, so a
// host class with a throwing static initializer produced an ExceptionInInitializerError (a
// LinkageError, i.e. an Error, not an Exception) that escaped the catch, propagated out of the
// constructor and — because PushwooshInitProvider.onCreate only catches Exception — crashed the app
// at startup. The catch now also covers LinkageError, so a load-time failure degrades the same way a
// missing class does: the constructor completes and the field is left null. If the LinkageError arm
// is removed, the constructor throws again and this test goes red.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class AndroidManifestConfigThrowingClinitCrashTest {

    @Mock
    private AppInfoProvider appInfoProvider;

    @Mock
    private ResourceProvider resourceProvider;

    private AutoCloseable mocks;
    private MockedStatic<AndroidPlatformModule> platformMock;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        platformMock = mockStatic(AndroidPlatformModule.class);
        platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
        platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");
    }

    @After
    public void tearDown() throws Exception {
        platformMock.close();
        mocks.close();
    }

    private void prepareConfigInputs(Bundle metaData) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metaData;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);
    }

    // Verifies that a notification_service_extension whose <clinit> throws leaves notificationService
    // null and does not crash the constructor.
    // The eager Class.forName raises an ExceptionInInitializerError (a LinkageError), which the
    // widened catch swallows exactly like a missing class, so the constructor completes gracefully.
    @Test
    public void notificationServiceExtensionWithThrowingClinit_isSwallowed_noCrash() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_service_extension",
                "com.pushwoosh.test.manifest.ThrowingClinitNotificationService");
        prepareConfigInputs(metaData);

        AndroidManifestConfig config = new AndroidManifestConfig();

        assertNull(config.getNotificationService());
    }

    // Verifies that a non-existent class name leaves notificationService null and does not throw.
    // Negative control: a class name that does not resolve throws ClassNotFoundException — an
    // Exception the catch already swallowed before the fix — so the constructor completes and the
    // field is left null. Keeps the two swallow paths (missing class vs failing <clinit>) distinct.
    @Test
    public void unknownNotificationServiceExtension_isSwallowed_noCrash() {
        Bundle metaData = new Bundle();
        metaData.putString(
                "com.pushwoosh.notification_service_extension", "com.pushwoosh.test.manifest.NotExistingClass");
        prepareConfigInputs(metaData);

        AndroidManifestConfig config = new AndroidManifestConfig();

        assertNull(config.getNotificationService());
    }
}
