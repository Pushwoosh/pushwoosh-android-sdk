package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.utils.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Regression guard for crash-fileutils-removeextension-no-dot (#14).
 *
 * <p>FileUtils.removeExtension previously did {@code path.substring(0, path.lastIndexOf("."))}: a
 * path with no '.' gave {@code lastIndexOf(".") == -1} and {@code substring(0, -1)} threw
 * StringIndexOutOfBoundsException. The fix guards the dotless case and returns the path unchanged.
 *
 * <p>The single production caller is AndroidManifestConfig:147-148, which reads the
 * {@code com.pushwoosh.notification_icon} meta-data, takes its last path component, then strips its
 * extension. A developer that sets the icon meta-data to a bare resource name with no extension
 * (e.g. {@code notification_small_icon}) used to crash the constructor during SDK init; it now
 * resolves the bare name as the drawable identifier.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class AndroidManifestConfigNotificationIconNoDotSiooobeTest {

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

    private AndroidManifestConfig constructWithNotificationIcon(String iconValue) {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.notification_icon", iconValue);
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metaData;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);
        return new AndroidManifestConfig();
    }

    // ---- Reach-path guard: real AndroidManifestConfig constructor, no-dot icon meta-data ----

    // Verifies that a bare resource name (no extension) no longer crashes the constructor and is
    // resolved as the drawable identifier, instead of throwing SIOOBE inside removeExtension.
    @Test
    public void reachPath_bareResourceName_resolvesIdentifierGracefully() {
        when(resourceProvider.getIdentifier("notification_small_icon", "drawable"))
                .thenReturn(777);

        AndroidManifestConfig config = constructWithNotificationIcon("notification_small_icon");

        assertEquals(777, config.getNotificationIcon());
    }

    // Verifies that a directory-style value whose final component has no extension is reduced to its
    // last component ("notification_small_icon") and resolved, with no SIOOBE.
    @Test
    public void reachPath_dirPathNoExtension_resolvesIdentifierGracefully() {
        when(resourceProvider.getIdentifier("notification_small_icon", "drawable"))
                .thenReturn(888);

        AndroidManifestConfig config = constructWithNotificationIcon("res/drawable/notification_small_icon");

        assertEquals(888, config.getNotificationIcon());
    }

    // ---- Direct crash-point guard: pure static FileUtils.removeExtension on a no-dot string ----

    // Verifies that removeExtension on a dotless string returns the path unchanged instead of throwing.
    @Test
    public void crashPoint_removeExtensionNoDot_returnsPathUnchanged() {
        assertEquals("notification_small_icon", FileUtils.removeExtension("notification_small_icon"));
        assertEquals("ic_push", FileUtils.removeExtension("ic_push"));
    }

    // ---- Negative controls (discriminators): a dotted path still has its extension stripped ----

    // Verifies the guard did not over-fire: a dotted icon path through the real constructor still
    // strips the extension before resolving the identifier.
    @Test
    public void negativeControl_dottedIconPath_stripsExtensionAndResolves() {
        when(resourceProvider.getIdentifier("notification_small_icon", "drawable"))
                .thenReturn(12345);

        AndroidManifestConfig config =
                constructWithNotificationIcon("res/drawable-xxhdpi-v11/notification_small_icon.png");

        assertEquals(12345, config.getNotificationIcon());
    }

    // Verifies the guard did not over-fire: a dotted string at the direct crash point still has its
    // extension stripped.
    @Test
    public void negativeControl_removeExtensionDotted_returnsTrimmed() {
        assertEquals("notification_small_icon", FileUtils.removeExtension("notification_small_icon.png"));
    }
}
