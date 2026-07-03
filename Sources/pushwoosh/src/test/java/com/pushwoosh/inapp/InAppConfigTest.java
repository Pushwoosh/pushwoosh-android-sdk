package com.pushwoosh.inapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;

import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.RepositoryTestManager;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.DEFAULT_PACKAGE_NAME, sdk = Build.VERSION_CODES.M)
public class InAppConfigTest {
    private InAppFolderProvider mockFolderProvider;
    private File mockConfigFile;
    private InAppConfig inAppConfig;
    private RegistrationPrefs registrationPrefs;

    @Before
    public void setUp() {
        mockFolderProvider = Mockito.mock(InAppFolderProvider.class);
        mockConfigFile = Mockito.mock(File.class);
        inAppConfig = new InAppConfig(mockFolderProvider);
        when(mockFolderProvider.getConfigFile(anyString())).thenReturn(mockConfigFile);
    }

    @After
    public void tearDown() {
        if (registrationPrefs != null) {
            RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
            registrationPrefs = null;
        }
        resetAndroidPlatformModule();
    }

    private static void resetAndroidPlatformModule() {
        try {
            Field contextField = AndroidPlatformModule.class.getDeclaredField("context");
            contextField.setAccessible(true);
            contextField.set(AndroidPlatformModule.getInstance(), null);
        } catch (Exception ignored) {
            // best-effort reset — leave the module initialized if reflection fails
        }
    }

    private void initRegistrationPrefs(String preferredLanguage) {
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        registrationPrefs =
                RepositoryTestManager.createRegistrationPrefs(MockConfig.createMock(), mock(DeviceRegistrar.class));
        RepositoryModule.setRegistrationPreferences(registrationPrefs);
        registrationPrefs.language().set(preferredLanguage);
    }

    @Test
    public void testParseModalConfig_FileNotExists_ReturnsNull() throws IOException {
        when(mockConfigFile.exists()).thenReturn(false);

        ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

        assertNull("Should return null when config file doesn't exist", result);
    }

    @Test
    public void testParseModalConfig_EmptyFile_ReturnsNull() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn("");

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNull("Should return null for empty config file", result);
        }
    }

    @Test
    public void testParseModalConfig_NoStyleSettings_ReturnsEmptyConfig() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn("{}");

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object even without style_settings", result);
            assertNull("viewPosition should be null", result.getViewPosition());
            assertNull("presentAnimationType should be null", result.getPresentAnimationType());
            assertNull("dismissAnimationType should be null", result.getDismissAnimationType());
            assertTrue(
                    "swipeGestures should be empty", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_CompleteStyleSettings_ParsesAllValues() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"position\": \"center\",\n"
                + "    \"present_animation\": \"up\",\n"
                + "    \"dismiss_animation\": \"fade_out\",\n"
                + "    \"swipe_to_dismiss\": [\"up\", \"down\"]\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object", result);
            assertEquals("viewPosition should be CENTER", ModalRichMediaViewPosition.CENTER, result.getViewPosition());
            assertEquals(
                    "presentAnimationType should be SLIDE_UP",
                    ModalRichMediaPresentAnimationType.SLIDE_UP,
                    result.getPresentAnimationType());
            assertEquals(
                    "dismissAnimationType should be FADE_OUT",
                    ModalRichMediaDismissAnimationType.FADE_OUT,
                    result.getDismissAnimationType());
            assertEquals(
                    "Should have 2 swipe gestures", 2, result.getSwipeGestures().size());
            assertTrue("Should contain UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
            assertTrue(
                    "Should contain DOWN gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.DOWN));
        }
    }

    @Test
    public void testParseModalConfig_PartialStyleSettings_ParsesOnlyProvidedValues() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"position\": \"top\",\n"
                + "    \"dismiss_animation\": \"down\"\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object", result);
            assertEquals("viewPosition should be TOP", ModalRichMediaViewPosition.TOP, result.getViewPosition());
            assertNull("presentAnimationType should be null (not provided)", result.getPresentAnimationType());
            assertEquals(
                    "dismissAnimationType should be SLIDE_DOWN",
                    ModalRichMediaDismissAnimationType.SLIDE_DOWN,
                    result.getDismissAnimationType());
            assertTrue(
                    "swipeGestures should be empty (not provided)",
                    result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_InvalidEnumValues_IgnoresInvalidValues() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"position\": \"invalid_position\",\n"
                + "    \"present_animation\": \"invalid_animation\",\n"
                + "    \"dismiss_animation\": \"fade_out\"\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object", result);
            assertNull("ViewPosition should be null for invalid value", result.getViewPosition());
            assertNull("PresentAnimationType should be null for invalid value", result.getPresentAnimationType());
            assertEquals(
                    "dismissAnimationType should be FADE_OUT (valid value)",
                    ModalRichMediaDismissAnimationType.FADE_OUT,
                    result.getDismissAnimationType());
        }
    }

    @Test
    public void testParseModalConfig_SwipeGesturesWithNone_FiltersOutNone() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"swipe_to_dismiss\": [\"up\", \"none\", \"down\", \"invalid_gesture\"]\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object", result);
            assertEquals(
                    "Should have 2 valid gestures", 2, result.getSwipeGestures().size());
            assertTrue("Should contain UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
            assertTrue(
                    "Should contain DOWN gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.DOWN));
        }
    }

    @Test(expected = IOException.class)
    public void testParseModalConfig_MalformedJSON_ThrowsIOException() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn("{ invalid json }");

            inAppConfig.parseModalConfig("test_code");
        }
    }

    @Test
    public void testParseModalConfig_SwipeGesturesNotArray_ContinuesGracefully() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"position\": \"center\",\n"
                + "    \"swipe_to_dismiss\": \"not_an_array\"\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object", result);
            assertEquals(
                    "Should parse position correctly", ModalRichMediaViewPosition.CENTER, result.getViewPosition());
            assertTrue(
                    "Should have empty gesture set when swipe_to_dismiss parsing fails",
                    result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_ExplicitNoneValues_ParsesCorrectly() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"present_animation\": \"none\",\n"
                + "    \"dismiss_animation\": \"none\",\n"
                + "    \"swipe_to_dismiss\": [\"none\"]\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object", result);
            assertEquals(
                    "Should parse none present animation",
                    ModalRichMediaPresentAnimationType.NONE,
                    result.getPresentAnimationType());
            assertEquals(
                    "Should parse none dismiss animation",
                    ModalRichMediaDismissAnimationType.NONE,
                    result.getDismissAnimationType());
            assertTrue(
                    "Should filter out NONE gestures", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseLocalizedStrings_PreferredLanguagePresent_ReturnsPreferredMap() throws Exception {
        initRegistrationPrefs("en");

        String jsonContent =
                "{\"default_language\":\"en\",\"localization\":{" + "\"en\":{\"hello\":\"hi\",\"bye\":\"see ya\"}}}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            Map<String, String> result = inAppConfig.parseLocalizedStrings("test_code");

            assertNotNull("Should return localized strings map", result);
            assertEquals("Should contain 2 entries", 2, result.size());
            assertEquals("hi", result.get("hello"));
            assertEquals("see ya", result.get("bye"));
        }
    }

    @Test
    public void testParseLocalizedStrings_PreferredMissing_FallsBackToDefault() throws Exception {
        initRegistrationPrefs("fr");

        String jsonContent = "{\"default_language\":\"en\",\"localization\":{" + "\"en\":{\"hello\":\"hi\"}}}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            Map<String, String> result = inAppConfig.parseLocalizedStrings("test_code");

            assertNotNull("Should fall back to default language map", result);
            assertEquals("Should contain 1 entry from en block", 1, result.size());
            assertEquals("hi", result.get("hello"));
        }
    }

    @Test
    public void testParseLocalizedStrings_NeitherPreferredNorDefaultPresent_ThrowsJSONException() {
        initRegistrationPrefs("fr");

        String jsonContent = "{\"default_language\":\"de\",\"localization\":{" + "\"en\":{\"hello\":\"hi\"}}}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            assertThrows(JSONException.class, () -> inAppConfig.parseLocalizedStrings("test_code"));
        }
    }

    // Pins LATENT BUG: parseLocalizedStrings does not null-check the content from
    // FileUtils.readFile (unlike parseModalConfig in the same class). The NPE escapes
    // through the declared `throws IOException, JSONException`. If prod fixes this
    // (null-handling or typed IOException), delete or update this test.
    @Test
    public void testParseLocalizedStrings_NullContent_ThrowsNpeBecauseOfLatentBug() {
        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(null);

            assertThrows(NullPointerException.class, () -> inAppConfig.parseLocalizedStrings("test_code"));
        }
    }

    @Test
    public void testParseModalConfig_PositionIsJsonNull_SwallowsExceptionAndKeepsSiblings() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"position\": null,\n"
                + "    \"present_animation\": \"up\"\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNotNull("Should return config object", result);
            assertNull("viewPosition should be null when position is JSON null", result.getViewPosition());
            assertEquals(
                    "present_animation sibling should still be parsed",
                    ModalRichMediaPresentAnimationType.SLIDE_UP,
                    result.getPresentAnimationType());
        }
    }

    @Test
    public void testParseModalConfig_AnimationDuration_Parsed() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"animation_duration\": 500\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertEquals(
                    "animationDuration should be parsed in milliseconds",
                    Integer.valueOf(500),
                    result.getAnimationDuration());
        }
    }

    @Test
    public void testParseModalConfig_AnimationDuration_LegacyDurationKeyIgnored() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"duration\": 1000\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNull(
                    "legacy \"duration\" key must be ignored; only animation_duration is read",
                    result.getAnimationDuration());
        }
    }

    @Test
    public void testParseModalConfig_AnimationDurationZero_RemainsNull() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"animation_duration\": 0\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNull(
                    "server-sent 0 should be treated as unset so the default animation plays (iOS parity)",
                    result.getAnimationDuration());
        }
    }

    @Test
    public void testParseModalConfig_NoAnimationDuration_RemainsNull() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"position\": \"center\"\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNull(
                    "animationDuration should stay null when absent (resolver applies default)",
                    result.getAnimationDuration());
        }
    }

    @Test
    public void testParseModalConfig_AnimationDuration_Negative_RemainsNull() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"animation_duration\": -100\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNull(
                    "negative animationDuration should be dropped (resolver applies default)",
                    result.getAnimationDuration());
        }
    }

    @Test
    public void testParseModalConfig_AnimationDuration_NonNumericString_RemainsNull() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"animation_duration\": \"abc\"\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertNull(
                    "non-numeric animationDuration should be swallowed and stay null",
                    result.getAnimationDuration());
        }
    }

    @Test
    public void testParseModalConfig_AnimationDuration_BothKeys_PrefersAnimationDuration() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"animation_duration\": 700,\n"
                + "    \"duration\": 300\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertEquals(
                    "animation_duration should take precedence over duration",
                    Integer.valueOf(700),
                    result.getAnimationDuration());
        }
    }

    @Test
    public void testParseModalConfig_AnimationDuration_Fractional_Truncated() throws IOException {
        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" + "  \"style_settings\": {\n"
                + "    \"animation_duration\": 500.5\n"
                + "  }\n"
                + "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");

            assertEquals(
                    "fractional animationDuration is truncated to int milliseconds",
                    Integer.valueOf(500),
                    result.getAnimationDuration());
        }
    }
}
