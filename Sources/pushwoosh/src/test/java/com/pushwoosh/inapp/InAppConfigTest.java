package com.pushwoosh.inapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.os.Build;

import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.internal.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.DEFAULT_PACKAGE_NAME, sdk = Build.VERSION_CODES.M)

public class InAppConfigTest {
    private InAppFolderProvider mockFolderProvider;
    private File mockConfigFile;
    private InAppConfig inAppConfig;

    @Before
    public void setUp() {
        mockFolderProvider = Mockito.mock(InAppFolderProvider.class);
        mockConfigFile = Mockito.mock(File.class);
        inAppConfig = new InAppConfig(mockFolderProvider);
        when(mockFolderProvider.getConfigFile(anyString())).thenReturn(mockConfigFile);
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
    public void testParseModalConfig_WhitespaceOnlyFile_ReturnsNull() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn("   \n\t  ");


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNull("Should return null for whitespace-only config file", result);
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
            assertTrue("swipeGestures should be empty", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_EmptyStyleSettings_ReturnsEmptyConfig() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {}\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertNull("viewPosition should be null", result.getViewPosition());
            assertNull("presentAnimationType should be null", result.getPresentAnimationType());
            assertNull("dismissAnimationType should be null", result.getDismissAnimationType());
            assertTrue("swipeGestures should be empty", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_CompleteStyleSettings_ParsesAllValues() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"center\",\n" +
            "    \"present_animation\": \"up\",\n" +
            "    \"dismiss_animation\": \"fade_out\",\n" +
            "    \"swipe_to_dismiss\": [\"up\", \"down\"]\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("viewPosition should be CENTER", ModalRichMediaViewPosition.CENTER, result.getViewPosition());
            assertEquals("presentAnimationType should be SLIDE_UP", ModalRichMediaPresentAnimationType.SLIDE_UP, result.getPresentAnimationType());
            assertEquals("dismissAnimationType should be FADE_OUT", ModalRichMediaDismissAnimationType.FADE_OUT, result.getDismissAnimationType());
            assertEquals("Should have 2 swipe gestures", 2, result.getSwipeGestures().size());
            assertTrue("Should contain UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
            assertTrue("Should contain DOWN gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.DOWN));
        }
    }

    @Test
    public void testParseModalConfig_PartialStyleSettings_ParsesOnlyProvidedValues() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"top\",\n" +
            "    \"dismiss_animation\": \"down\"\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("viewPosition should be TOP", ModalRichMediaViewPosition.TOP, result.getViewPosition());
            assertNull("presentAnimationType should be null (not provided)", result.getPresentAnimationType());
            assertEquals("dismissAnimationType should be SLIDE_DOWN", ModalRichMediaDismissAnimationType.SLIDE_DOWN, result.getDismissAnimationType());
            assertTrue("swipeGestures should be empty (not provided)", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_InvalidEnumValues_IgnoresInvalidValues() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"invalid_position\",\n" +
            "    \"present_animation\": \"invalid_animation\",\n" +
            "    \"dismiss_animation\": \"fade_out\"\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertNull("ViewPosition should be null for invalid value", result.getViewPosition());
            assertNull("PresentAnimationType should be null for invalid value", result.getPresentAnimationType());
            assertEquals("dismissAnimationType should be FADE_OUT (valid value)", ModalRichMediaDismissAnimationType.FADE_OUT, result.getDismissAnimationType());
        }
    }

    @Test
    public void testParseModalConfig_SwipeGesturesWithNone_FiltersOutNone() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"swipe_to_dismiss\": [\"up\", \"none\", \"down\", \"invalid_gesture\"]\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("Should have 2 valid gestures", 2, result.getSwipeGestures().size());
            assertTrue("Should contain UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
            assertTrue("Should contain DOWN gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.DOWN));
        }
    }

    @Test
    public void testParseModalConfig_OnlyInvalidSwipeGestures_ResultsInEmptySet() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"swipe_to_dismiss\": [\"none\", \"invalid\", \"bad_gesture\"]\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertTrue("Should have empty gesture set when all gestures are invalid", result.getSwipeGestures().isEmpty());
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

    @Test(expected = IOException.class)
    public void testParseModalConfig_InvalidJSONStructure_ThrowsIOException() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn("{ \"unclosed\": { \"object\" }");



            inAppConfig.parseModalConfig("test_code");
        }
    }

    @Test
    public void testParseModalConfig_SwipeGesturesNotArray_ContinuesGracefully() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"center\",\n" +
            "    \"swipe_to_dismiss\": \"not_an_array\"\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("Should parse position correctly", ModalRichMediaViewPosition.CENTER, result.getViewPosition());
            assertTrue("Should have empty gesture set when swipe_to_dismiss parsing fails", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_AllValidPositionValues_ParsesCorrectly() throws IOException {
        String[] positions = {"top", "center", "bottom", "fullscreen"};
        ModalRichMediaViewPosition[] expectedPositions = {
            ModalRichMediaViewPosition.TOP,
            ModalRichMediaViewPosition.CENTER,
            ModalRichMediaViewPosition.BOTTOM,
            ModalRichMediaViewPosition.FULLSCREEN
        };

        for (int i = 0; i < positions.length; i++) {
            when(mockConfigFile.exists()).thenReturn(true);

            String jsonContent = "{\n" +
                "  \"style_settings\": {\n" +
                "    \"position\": \"" + positions[i] + "\"\n" +
                "  }\n" +
                "}";

            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);

                ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code_" + i);

                assertNotNull("Should return config object for " + positions[i], result);
                assertEquals("Should parse " + positions[i] + " correctly", 
                           expectedPositions[i], result.getViewPosition());
            }
        }
    }

    @Test
    public void testParseModalConfig_UnknownPositionValue_RemainsNull() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"unknown_position\"\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertNull("Unknown position should remain null", result.getViewPosition());
        }
    }

    @Test
    public void testParseModalConfig_AllValidSwipeGestures_ParsesCorrectly() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"swipe_to_dismiss\": [\"up\", \"down\", \"left\", \"right\"]\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("Should have 4 gestures", 4, result.getSwipeGestures().size());
            assertTrue("Should contain UP", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
            assertTrue("Should contain DOWN", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.DOWN));
            assertTrue("Should contain LEFT", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.LEFT));
            assertTrue("Should contain RIGHT", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.RIGHT));
        }
    }

    @Test
    public void testParseModalConfig_ExplicitFullscreenValue_ParsesCorrectly() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"fullscreen\"\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("Should parse fullscreen explicitly", ModalRichMediaViewPosition.FULLSCREEN, result.getViewPosition());
        }
    }

    @Test
    public void testParseModalConfig_ExplicitNoneValues_ParsesCorrectly() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"present_animation\": \"none\",\n" +
            "    \"dismiss_animation\": \"none\",\n" +
            "    \"swipe_to_dismiss\": [\"none\"]\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("Should parse none present animation", ModalRichMediaPresentAnimationType.NONE, result.getPresentAnimationType());
            assertEquals("Should parse none dismiss animation", ModalRichMediaDismissAnimationType.NONE, result.getDismissAnimationType());
            assertTrue("Should filter out NONE gestures", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testParseModalConfig_ExtraFieldsInStyleSettings_IgnoresExtraFields() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"center\",\n" +
            "    \"unknown_field\": \"unknown_value\",\n" +
            "    \"another_field\": 123\n" +
            "  }\n" +
            "}";

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(jsonContent);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNotNull("Should return config object", result);
            assertEquals("Should parse known position field", ModalRichMediaViewPosition.CENTER, result.getViewPosition());
            assertNull("Other fields should remain null", result.getPresentAnimationType());
        }
    }

    @Test
    public void testParseModalConfig_NullFileContent_ReturnsNull() throws IOException {

        when(mockConfigFile.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.readFile(mockConfigFile)).thenReturn(null);


            ModalRichmediaConfig result = inAppConfig.parseModalConfig("test_code");


            assertNull("Should return null when file content is null", result);
        }
    }
}