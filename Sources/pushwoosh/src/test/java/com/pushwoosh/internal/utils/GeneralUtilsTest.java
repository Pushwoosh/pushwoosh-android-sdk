package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
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

import java.util.Collections;

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

    @Mock
    private ResourceProvider resourceProvider;

    private int nextResourceId = 0x7f0e0001;

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

    // Verifies that the R$raw class is found in the applicationId package itself.
    @Test
    public void testGetRawResourses_ApplicationIdHoldsRawClass_ReturnsItsSounds() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(
                    platform, "com.pwsoundfixture", "com.nosuchpkg.SampleApplication", "com.nosuchpkg.MainActivity");
            givenResourceTable("parent_package_sound");

            assertEquals(Collections.singletonList("parent_package_sound"), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that an applicationId with a suffix (".debug") still finds the R$raw class of a parent package.
    @Test
    public void testGetRawResourses_ApplicationIdSuffix_WalksUpToParentPackage() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture.app.debug", null, null);
            givenResourceTable("parent_package_sound");

            assertEquals(Collections.singletonList("parent_package_sound"), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that the package of a custom Application class is probed when applicationId holds no R$raw class.
    @Test
    public void testGetRawResourses_ApplicationClassPackage_WinsWhenApplicationIdHasNoRawClass() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(platform, "com.nosuchpkg.app", "com.pwsoundfixture.other.SampleApplication", null);
            givenResourceTable("application_class_sound", "parent_package_sound");

            assertEquals(Collections.singletonList("application_class_sound"), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that the launcher activity package is probed when neither applicationId nor Application class helps.
    @Test
    public void testGetRawResourses_LauncherActivityPackage_WinsWhenEarlierCandidatesHaveNoRawClass() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(platform, "com.nosuchpkg.app", null, "com.pwsoundfixture.launcher.MainActivity");
            givenResourceTable("launcher_activity_sound", "parent_package_sound");

            assertEquals(Collections.singletonList("launcher_activity_sound"), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that the parent-package walk stops above a single-segment package and never probes it.
    @Test
    public void testGetRawResourses_SingleSegmentPackage_NeverProbed() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(platform, "pwsoundroot.app", null, null);
            // The name would resolve if the walk reached "pwsoundroot", so an empty list is the floor working.
            givenResourceTable("too_shallow_sound");

            assertEquals(Collections.emptyList(), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that no R$raw class in any candidate yields an empty list and one plain log line.
    @Test
    public void testGetRawResourses_NoCandidateFound_ReturnsEmptyListAndLogsOnce() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            givenCandidateSources(platform, "com.nosuchpkg.app", null, null);

            assertEquals(Collections.emptyList(), GeneralUtils.getRawResourses());

            pwLog.verify(() -> PWLog.debug(anyString(), anyString()));
            pwLog.verifyNoMoreInteractions();
        }
    }

    // Verifies that a field name absent from our resource table is skipped without aborting the enumeration.
    @Test
    public void testGetRawResourses_FieldMissingFromResourceTable_SkippedWithoutError() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture.mixed", null, null);
            givenResourceTable("mixed_known_sound");

            assertEquals(Collections.singletonList("mixed_known_sound"), GeneralUtils.getRawResourses());

            pwLog.verify(() -> PWLog.noise(anyString(), anyString(), any(Throwable.class)), never());
        }
    }

    // Verifies that a PackageManager failure on one candidate leaves the earlier candidates in the queue.
    // Resolving a candidate is binder-IPC and dies on its own; the sounds of a healthy app must survive that.
    @Test
    public void testGetRawResourses_LauncherCandidateThrows_EarlierCandidateStillWins() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture", null, null);
            givenResourceTable("parent_package_sound");
            when(packageManager.getLaunchIntentForPackage("com.pwsoundfixture"))
                    .thenThrow(new RuntimeException("Package manager has died"));

            assertEquals(Collections.singletonList("parent_package_sound"), GeneralUtils.getRawResourses());

            pwLog.verify(() -> PWLog.noise(anyString(), anyString(), any(Throwable.class)));
        }
    }

    // Verifies that a failure outside the per-candidate guards still stays inside getRawResourses.
    // Enumeration feeds registerDevice, so an escaping exception used to break device registration itself.
    @Test
    public void testGetRawResourses_ApplicationIdLookupThrows_ReturnsEmptyListWithoutPropagating() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture", null, null);
            when(appInfoProvider.getPackageName()).thenThrow(new RuntimeException("Context has died"));

            assertEquals(Collections.emptyList(), GeneralUtils.getRawResourses());

            pwLog.verify(() -> PWLog.noise(anyString(), anyString(), any(Throwable.class)));
        }
    }

    // Verifies that the deepest parent package is probed first, whichever source it came from.
    // A two-segment parent of the applicationId is the likelier library namespace, so it must not win.
    @Test
    public void testGetRawResourses_DeeperParentPackage_BeatsShallowerParentOfAnotherSource() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture.app", null, "com.pwsoundfixture.deep.ui.MainActivity");
            givenResourceTable("deep_package_sound", "parent_package_sound");

            assertEquals(Collections.singletonList("deep_package_sound"), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that every exact candidate package is probed before any parent package.
    // A parent of applicationId may belong to a library that ships its own res/raw.
    @Test
    public void testGetRawResourses_ExactApplicationClassPackage_BeatsApplicationIdParent() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(
                    platform, "com.pwsoundfixture.app", "com.pwsoundfixture.other.SampleApplication", null);
            givenResourceTable("application_class_sound", "parent_package_sound");

            assertEquals(Collections.singletonList("application_class_sound"), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that a raw resource pointing at a non-audio file is not reported as a sound.
    @Test
    public void testGetRawResourses_NonAudioResource_NotReported() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture", null, null);
            givenResourceTable();
            // res/raw holds non-audio files too: keep.xml and firebase resources land there in a real APK.
            givenResourceFile("parent_package_sound", "res/raw/keep.xml");

            assertEquals(Collections.emptyList(), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that an <activity-alias> launcher resolves to the package of its targetActivity.
    @Test
    public void testGetRawResourses_LauncherActivityAlias_ResolvesTargetActivityPackage() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            givenCandidateSources(
                    platform,
                    "com.nosuchpkg.app",
                    null,
                    "com.brandingpkg.LauncherAlias",
                    "com.pwsoundfixture.launcher.MainActivity");
            givenResourceTable("launcher_activity_sound", "parent_package_sound");

            assertEquals(Collections.singletonList("launcher_activity_sound"), GeneralUtils.getRawResourses());
        }
    }

    // Verifies that a field failing to resolve does not truncate the sounds of the whole app.
    // Both unreadable fields must be reported: a single log line would mean the loop died on the first one.
    @Test
    public void testGetRawResourses_FieldResolutionThrows_RestOfListSurvives() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture.mixed", null, null);
            givenResourceTable("mixed_known_sound");
            givenUnreadableResource("mixed_foreign_sound");
            givenUnreadableResource("mixed_broken_sound");

            assertEquals(Collections.singletonList("mixed_known_sound"), GeneralUtils.getRawResourses());

            pwLog.verify(() -> PWLog.noise(anyString(), anyString(), any(Throwable.class)), times(2));
        }
    }

    // Verifies that a negative identifier skips the field instead of resolving it.
    // ContextResourceProvider answers -1 once its WeakReference<Context> has been cleared.
    @Test
    public void testGetRawResourses_IdentifierNegative_FieldSkippedWithoutError() throws Exception {
        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            givenCandidateSources(platform, "com.pwsoundfixture", null, null);
            givenResourceTable();
            when(resourceProvider.getIdentifier(anyString(), eq("raw"))).thenReturn(-1);

            assertEquals(Collections.emptyList(), GeneralUtils.getRawResourses());

            pwLog.verify(() -> PWLog.noise(anyString(), anyString(), any(Throwable.class)), never());
        }
    }

    private void givenCandidateSources(
            MockedStatic<AndroidPlatformModule> platform,
            String applicationId,
            @Nullable String applicationClassName,
            @Nullable String launcherActivityClassName)
            throws PackageManager.NameNotFoundException {
        givenCandidateSources(platform, applicationId, applicationClassName, launcherActivityClassName, null);
    }

    private void givenCandidateSources(
            MockedStatic<AndroidPlatformModule> platform,
            String applicationId,
            @Nullable String applicationClassName,
            @Nullable String launcherComponentClassName,
            @Nullable String launcherTargetActivity)
            throws PackageManager.NameNotFoundException {
        platform.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
        platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
        platform.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);

        when(appInfoProvider.getPackageName()).thenReturn(applicationId);

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.className = applicationClassName;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);

        when(managerProvider.getPackageManager()).thenReturn(packageManager);
        if (launcherComponentClassName == null) {
            when(packageManager.getLaunchIntentForPackage(applicationId)).thenReturn(null);
        } else {
            ComponentName component = new ComponentName(applicationId, launcherComponentClassName);
            when(packageManager.getLaunchIntentForPackage(applicationId))
                    .thenReturn(new Intent().setComponent(component));
            // Real PackageManager never answers null here, it throws NameNotFoundException instead.
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.targetActivity = launcherTargetActivity;
            when(packageManager.getActivityInfo(component, 0)).thenReturn(activityInfo);
        }

        // The www/res assets branch is out of scope: a null AssetManager ends getRawResourses.
        when(managerProvider.getAssets()).thenReturn(null);
    }

    private void givenResourceTable(String... soundFieldNames) {
        for (String fieldName : soundFieldNames) {
            givenResourceFile(fieldName, fieldName + ".mp3");
        }

        // Names outside the table resolve to a non-positive id, and real Resources throws for such an id.
        // Keeping that throw here is what stops a lost "res <= 0" guard from passing silently.
        doThrow(new Resources.NotFoundException("non-positive resource id"))
                .when(resourceProvider)
                .getValue(intThat(id -> id <= 0), any(TypedValue.class), anyBoolean());
    }

    private void givenUnreadableResource(String fieldName) {
        int id = nextResourceId++;
        when(resourceProvider.getIdentifier(fieldName, "raw")).thenReturn(id);
        doThrow(new Resources.NotFoundException(fieldName))
                .when(resourceProvider)
                .getValue(eq(id), any(TypedValue.class), anyBoolean());
    }

    private void givenResourceFile(String fieldName, String resourceFileName) {
        int id = nextResourceId++;
        when(resourceProvider.getIdentifier(fieldName, "raw")).thenReturn(id);
        doAnswer(invocation -> {
                    TypedValue value = invocation.getArgument(1);
                    value.string = resourceFileName;
                    return null;
                })
                .when(resourceProvider)
                .getValue(eq(id), any(TypedValue.class), anyBoolean());
    }
}
