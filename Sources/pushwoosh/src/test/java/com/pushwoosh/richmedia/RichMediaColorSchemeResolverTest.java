package com.pushwoosh.richmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.util.TypedValue;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class RichMediaColorSchemeResolverTest {

    private Context app;

    @Before
    public void setUp() throws Exception {
        app = RuntimeEnvironment.application;
        resetPlatformInstance();
    }

    static void resetPlatformInstance() throws Exception {
        Field instance = PushwooshPlatform.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static Activity activityWithTheme(int themeRes) {
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        activity.setTheme(themeRes);
        return activity;
    }

    // Design precondition: since API 29 isLightTheme is declared on the base framework Theme.
    @Test
    public void frameworkBaseTheme_resolvesIsLightTheme() {
        Resources.Theme theme = app.getResources().newTheme();
        theme.applyStyle(android.R.style.Theme, true);
        TypedValue value = new TypedValue();
        assertTrue(theme.resolveAttribute(android.R.attr.isLightTheme, value, true));
    }

    @Test
    public void light_isNeverDark() {
        RuntimeEnvironment.setQualifiers("+night");
        app.getApplicationInfo().theme = android.R.style.Theme_Material;
        Activity darkHost = activityWithTheme(android.R.style.Theme_Material);

        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.LIGHT, darkHost, app));
        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.LIGHT, null, app));
    }

    @Test
    public void dark_isAlwaysDark() {
        RuntimeEnvironment.setQualifiers("+notnight");
        app.getApplicationInfo().theme = android.R.style.Theme_Material_Light;
        Activity lightHost = activityWithTheme(android.R.style.Theme_Material_Light);

        assertTrue(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.DARK, lightHost, app));
        assertTrue(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.DARK, null, app));
    }

    @Test
    public void system_nightYes_darkRegardlessOfThemes() {
        RuntimeEnvironment.setQualifiers("+night");
        app.getApplicationInfo().theme = android.R.style.Theme_Material_Light;
        Activity lightHost = activityWithTheme(android.R.style.Theme_Material_Light);

        assertTrue(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.SYSTEM, lightHost, app));
        assertTrue(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.SYSTEM, null, app));
    }

    @Test
    public void system_nightNo_lightRegardlessOfThemes() {
        RuntimeEnvironment.setQualifiers("+notnight");
        app.getApplicationInfo().theme = android.R.style.Theme_Material;
        Activity darkHost = activityWithTheme(android.R.style.Theme_Material);

        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.SYSTEM, darkHost, app));
        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.SYSTEM, null, app));
    }

    @Test
    public void app_lightHost_lightDespiteNightAndDarkManifest() {
        RuntimeEnvironment.setQualifiers("+night");
        app.getApplicationInfo().theme = android.R.style.Theme_Material;
        Activity lightHost = activityWithTheme(android.R.style.Theme_Material_Light);

        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.APP, lightHost, app));
    }

    @Test
    public void app_darkHost_darkDespiteDayAndLightManifest() {
        RuntimeEnvironment.setQualifiers("+notnight");
        app.getApplicationInfo().theme = android.R.style.Theme_Material_Light;
        Activity darkHost = activityWithTheme(android.R.style.Theme_Material);

        assertTrue(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.APP, darkHost, app));
    }

    @Test
    public void app_noHost_followsManifestTheme() {
        app.getApplicationInfo().theme = android.R.style.Theme_Material_Light;
        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.APP, null, app));

        app.getApplicationInfo().theme = android.R.style.Theme_Material;
        assertTrue(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.APP, null, app));
    }

    @Test
    public void app_noHost_manifestThemeZero_isLight() {
        RuntimeEnvironment.setQualifiers("+night");
        app.getApplicationInfo().theme = 0;

        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.APP, null, app));
    }

    @Test
    public void app_themeWithoutIsLightThemeAttribute_isLight() {
        // TextAppearance is a style, not a theme: isLightTheme never resolves from it.
        app.getApplicationInfo().theme = android.R.style.TextAppearance;

        assertFalse(RichMediaColorSchemeResolver.isDark(RichMediaColorScheme.APP, null, app));
    }

    @Test
    public void isHostCandidate_falseForTranslucent_trueForOpaque() {
        Activity translucent = activityWithTheme(android.R.style.Theme_Translucent_NoTitleBar);
        assertFalse(PushwooshPlatform.isHostCandidate(translucent));

        Activity opaque = activityWithTheme(android.R.style.Theme_Material_Light);
        assertTrue(PushwooshPlatform.isHostCandidate(opaque));
    }

    @Test
    public void isHostCandidate_falseForFloatingDialogThemes() {
        Activity darkDialog = activityWithTheme(android.R.style.Theme_Material_Dialog);
        assertFalse(PushwooshPlatform.isHostCandidate(darkDialog));

        Activity lightDialog = activityWithTheme(android.R.style.Theme_Material_Light_Dialog);
        assertFalse(PushwooshPlatform.isHostCandidate(lightDialog));
    }

    // --- wrapForCurrentScheme ---

    private static boolean resolveIsLightTheme(Context context, TypedValue out) {
        return context.getTheme().resolveAttribute(android.R.attr.isLightTheme, out, true);
    }

    @Test
    public void wrap_opaqueDarkActivityBase_usedAsHost_wrapsDark() {
        Activity darkHost = activityWithTheme(android.R.style.Theme_Material);

        Context wrapped = RichMediaColorSchemeResolver.wrapForCurrentScheme(darkHost);

        TypedValue value = new TypedValue();
        assertTrue(resolveIsLightTheme(wrapped, value));
        assertEquals(0, value.data);
    }

    @Test
    public void wrap_translucentActivityBase_fallsBackToManifestTheme() {
        // Legacy path: RichMediaWebActivity is translucent, so its own (dark) theme must be ignored.
        app.getApplicationInfo().theme = android.R.style.Theme_Material_Light;
        Activity translucent = activityWithTheme(android.R.style.Theme_Translucent_NoTitleBar);

        Context wrapped = RichMediaColorSchemeResolver.wrapForCurrentScheme(translucent);

        TypedValue value = new TypedValue();
        assertTrue(resolveIsLightTheme(wrapped, value));
        assertTrue(value.data != 0);
    }

    @Test
    public void wrap_translucentActivityBase_usesPlatformHostTheme() {
        // Legacy path with a live host: the manifest theme is light, only the host can make it dark.
        app.getApplicationInfo().theme = android.R.style.Theme_Material_Light;
        Activity darkHost = activityWithTheme(android.R.style.Theme_Material);
        Activity translucent = activityWithTheme(android.R.style.Theme_Translucent_NoTitleBar);
        PushwooshPlatform platform = mock(PushwooshPlatform.class);
        when(platform.getHostActivity()).thenReturn(darkHost);

        try (MockedStatic<PushwooshPlatform> mocked = mockStatic(PushwooshPlatform.class)) {
            mocked.when(PushwooshPlatform::getInstance).thenReturn(platform);
            mocked.when(() -> PushwooshPlatform.isHostCandidate(any(Activity.class)))
                    .thenCallRealMethod();

            Context wrapped = RichMediaColorSchemeResolver.wrapForCurrentScheme(translucent);

            TypedValue value = new TypedValue();
            assertTrue(resolveIsLightTheme(wrapped, value));
            assertEquals(0, value.data);
        }
    }

    @Test
    public void wrap_baseApplicationContextNull_resolvesViaBaseItself() {
        // Non-Activity Context whose own getApplicationContext() is null (seen in the wild): the
        // manifest theme must be read through base, and here it is light.
        Context base = mock(Context.class);
        when(base.getApplicationContext()).thenReturn(null);
        when(base.getResources()).thenReturn(app.getResources());
        ApplicationInfo info = new ApplicationInfo();
        info.theme = android.R.style.Theme_Material_Light;
        when(base.getApplicationInfo()).thenReturn(info);

        Context wrapped = RichMediaColorSchemeResolver.wrapForCurrentScheme(base);

        TypedValue value = new TypedValue();
        assertTrue(resolveIsLightTheme(wrapped, value));
        assertTrue(value.data != 0);
    }

    @Test
    public void wrap_schemeFromPrefs_reachesResolver() {
        // A light opaque host would resolve APP to light; only the stored DARK scheme can wrap dark.
        Activity lightHost = activityWithTheme(android.R.style.Theme_Material_Light);
        NotificationPrefs prefs = mock(NotificationPrefs.class);
        PreferenceStringValue schemePref = mock(PreferenceStringValue.class);
        when(schemePref.get()).thenReturn(RichMediaColorScheme.DARK.name());
        when(prefs.richMediaColorScheme()).thenReturn(schemePref);

        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            repo.when(RepositoryModule::getNotificationPreferences).thenReturn(prefs);

            Context wrapped = RichMediaColorSchemeResolver.wrapForCurrentScheme(lightHost);

            TypedValue value = new TypedValue();
            assertTrue(resolveIsLightTheme(wrapped, value));
            assertEquals(0, value.data);
        }
    }

    // --- isCurrentSchemeDark ---

    @Test
    public void isCurrentSchemeDark_storedDarkScheme_trueOnLightHost() {
        Activity lightHost = activityWithTheme(android.R.style.Theme_Material_Light);
        NotificationPrefs prefs = mock(NotificationPrefs.class);
        PreferenceStringValue schemePref = mock(PreferenceStringValue.class);
        when(schemePref.get()).thenReturn(RichMediaColorScheme.DARK.name());
        when(prefs.richMediaColorScheme()).thenReturn(schemePref);

        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            repo.when(RepositoryModule::getNotificationPreferences).thenReturn(prefs);
            assertTrue(RichMediaColorSchemeResolver.isCurrentSchemeDark(lightHost));
        }
    }

    @Test
    public void isCurrentSchemeDark_defaultAppScheme_followsHostTheme() {
        assertTrue(RichMediaColorSchemeResolver.isCurrentSchemeDark(activityWithTheme(android.R.style.Theme_Material)));
        assertFalse(RichMediaColorSchemeResolver.isCurrentSchemeDark(
                activityWithTheme(android.R.style.Theme_Material_Light)));
    }
}
