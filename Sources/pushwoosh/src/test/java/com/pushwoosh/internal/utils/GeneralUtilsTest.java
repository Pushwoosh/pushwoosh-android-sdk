package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.utils.GeneralUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class GeneralUtilsTest {

    private AutoCloseable mocks;

    @Mock
    private ManagerProvider managerProvider;

    @Mock
    private AppInfoProvider appInfoProvider;

    @Mock
    private ConnectivityManager connectivityManager;

    @Mock
    private NetworkInfo networkInfo;

    @Mock
    private Context context;

    @Mock
    private PackageManager packageManager;

    @Mock
    private Activity activity;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckNotNullOrEmpty_Null() throws Exception {
        GeneralUtils.checkNotNullOrEmpty(null, "Unit test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckNotNullOrEmpty_Empty() throws Exception {
        GeneralUtils.checkNotNullOrEmpty("", "Unit test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckNotNull_Null() throws Exception {
        GeneralUtils.checkNotNull(null, "Unit test");
    }

    @Test
    public void testParseColor() throws Exception {
        assertEquals(GeneralUtils.parseColor("#FF0000"), Color.RED); // #rrggbb
        assertEquals(GeneralUtils.parseColor("#FFFF0000"), Color.RED); // #aarrggbb
        assertEquals(GeneralUtils.parseColor("#F00"), Color.RED); // #rgb
        assertEquals(GeneralUtils.parseColor("#FF00"), Color.RED); // #argb
        assertEquals(GeneralUtils.parseColor("0,255,0,255"), Color.GREEN); // r,g,b,a
    }

    // Verifies that md5 produces the known lowercase hex digest for a non-empty string.
    @Test
    public void testMd5_NonEmptyInput_ReturnsLowercaseHexDigest() {
        String digest = GeneralUtils.md5("hello");

        assertEquals("5d41402abc4b2a76b9719d911017c592", digest);
        assertEquals(32, digest.length());
        assertTrue("digest must be lowercase hex", digest.matches("[0-9a-f]{32}"));
    }

    // Verifies that md5(null) returns an empty string instead of throwing.
    @Test
    public void testMd5_NullInput_ReturnsEmpty() {
        assertEquals("", GeneralUtils.md5(null));
    }

    // Verifies that parseColor returns opaque white when the input is malformed.
    @Test
    public void testParseColor_Malformed_ReturnsOpaqueWhite() {
        assertEquals(0xFFFFFFFF, GeneralUtils.parseColor("not-a-color"));
        assertEquals(0xFFFFFFFF, GeneralUtils.parseColor("#ZZZ"));
    }

    // Verifies that isNetworkAvailable returns true when the active network is available and connected.
    @Test
    public void testIsNetworkAvailable_AvailableAndConnected_ReturnsTrue() {
        try (MockedStatic<AndroidPlatformModule> mocked = mockStatic(AndroidPlatformModule.class)) {
            mocked.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            when(managerProvider.getConnectivityManager()).thenReturn(connectivityManager);
            when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
            when(networkInfo.isAvailable()).thenReturn(true);
            when(networkInfo.isConnected()).thenReturn(true);

            assertTrue(GeneralUtils.isNetworkAvailable());
        }
    }

    // Verifies that isNetworkAvailable returns false when ConnectivityManager is null.
    @Test
    public void testIsNetworkAvailable_ConnectivityManagerNull_ReturnsFalse() {
        try (MockedStatic<AndroidPlatformModule> mocked = mockStatic(AndroidPlatformModule.class)) {
            mocked.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            when(managerProvider.getConnectivityManager()).thenReturn(null);

            assertFalse(GeneralUtils.isNetworkAvailable());
        }
    }

    // Verifies that isNetworkAvailable returns false when the active network is available but not connected.
    @Test
    public void testIsNetworkAvailable_NotConnected_ReturnsFalse() {
        try (MockedStatic<AndroidPlatformModule> mocked = mockStatic(AndroidPlatformModule.class)) {
            mocked.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            when(managerProvider.getConnectivityManager()).thenReturn(connectivityManager);
            when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
            when(networkInfo.isAvailable()).thenReturn(true);
            when(networkInfo.isConnected()).thenReturn(false);

            assertFalse(GeneralUtils.isNetworkAvailable());
        }
    }

    // Verifies that isMainActivity returns true when the activity class equals the launch intent's component class.
    @Test
    public void testIsMainActivity_ActivityMatchesLaunchComponent_ReturnsTrue() throws Exception {
        String pkg = "com.example.app";
        String activityClass = activity.getClass().getName();
        ComponentName component = new ComponentName(pkg, activityClass);
        Intent launchIntent = new Intent().setComponent(component);

        when(activity.getPackageManager()).thenReturn(packageManager);
        when(activity.getPackageName()).thenReturn(pkg);
        when(packageManager.getLaunchIntentForPackage(pkg)).thenReturn(launchIntent);
        ActivityInfo info = new ActivityInfo();
        info.targetActivity = null;
        when(packageManager.getActivityInfo(component, 0)).thenReturn(info);

        assertTrue(GeneralUtils.isMainActivity(activity));
    }

    // Verifies that isMainActivity returns false when no launch intent exists for the package.
    @Test
    public void testIsMainActivity_NoLaunchIntent_ReturnsFalse() {
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(activity.getPackageName()).thenReturn("com.example.app");
        when(packageManager.getLaunchIntentForPackage("com.example.app")).thenReturn(null);

        assertFalse(GeneralUtils.isMainActivity(activity));
    }

    // Verifies that isMainActivity returns false when the launch intent has no component.
    @Test
    public void testIsMainActivity_LaunchIntentNoComponent_ReturnsFalse() {
        Intent launchIntent = new Intent();
        launchIntent.setComponent(null);

        when(activity.getPackageManager()).thenReturn(packageManager);
        when(activity.getPackageName()).thenReturn("com.example.app");
        when(packageManager.getLaunchIntentForPackage("com.example.app")).thenReturn(launchIntent);

        assertFalse(GeneralUtils.isMainActivity(activity));
    }

    // Verifies that isMainActivity honors ActivityInfo.targetActivity (activity-alias case).
    @Test
    public void testIsMainActivity_TargetActivityAlias_ReturnsTrue() throws Exception {
        String pkg = "com.example.app";
        String activityClass = activity.getClass().getName();
        ComponentName aliasComponent = new ComponentName(pkg, "com.example.app.LauncherAlias");
        Intent launchIntent = new Intent().setComponent(aliasComponent);

        when(activity.getPackageManager()).thenReturn(packageManager);
        when(activity.getPackageName()).thenReturn(pkg);
        when(packageManager.getLaunchIntentForPackage(pkg)).thenReturn(launchIntent);
        ActivityInfo info = new ActivityInfo();
        info.targetActivity = activityClass;
        when(packageManager.getActivityInfo(aliasComponent, 0)).thenReturn(info);

        assertTrue(GeneralUtils.isMainActivity(activity));
    }

    // Verifies that isMainActivity falls back to componentName.getClassName when getActivityInfo throws NameNotFound.
    @Test
    public void testIsMainActivity_GetActivityInfoThrows_FallsBackToComponentClassName() throws Exception {
        String pkg = "com.example.app";
        String activityClass = activity.getClass().getName();
        ComponentName component = new ComponentName(pkg, activityClass);
        Intent launchIntent = new Intent().setComponent(component);

        when(activity.getPackageManager()).thenReturn(packageManager);
        when(activity.getPackageName()).thenReturn(pkg);
        when(packageManager.getLaunchIntentForPackage(pkg)).thenReturn(launchIntent);
        when(packageManager.getActivityInfo(component, 0))
                .thenThrow(new PackageManager.NameNotFoundException("not found"));

        assertTrue(GeneralUtils.isMainActivity(activity));
    }
}
