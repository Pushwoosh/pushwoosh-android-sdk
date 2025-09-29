package com.pushwoosh.inapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.view.config.ModalConfigResolver;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.DEFAULT_MANIFEST_NAME)
public class RemoteConfigIntegrationTest {

    private PlatformTestManager platformTestManager;
    private MockedStatic<FileUtils> mockedFileUtils;
    private MockedStatic<RichMediaManager> mockedRichMediaManager;

    private InAppFolderProvider mockFolderProvider;
    private File mockConfigFile;
    private InAppConfig inAppConfig;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        mockedFileUtils = mockStatic(FileUtils.class);
        mockedRichMediaManager = mockStatic(RichMediaManager.class);
        
        mockFolderProvider = mock(InAppFolderProvider.class);
        mockConfigFile = mock(File.class);
        
        when(mockFolderProvider.getConfigFile(anyString())).thenReturn(mockConfigFile);
        when(mockConfigFile.exists()).thenReturn(true);
        
        inAppConfig = new InAppConfig(mockFolderProvider);
    }
    
    private Resource createMockResource(ModalRichmediaConfig config) {
        Resource mockResource = mock(Resource.class);
        when(mockResource.hasResourceModalConfig()).thenReturn(config != null);
        when(mockResource.getResourceModalConfig()).thenReturn(config);
        when(mockResource.getCode()).thenReturn("test_code");
        return mockResource;
    }

    @After
    public void tearDown() {
        if (mockedFileUtils != null) {
            mockedFileUtils.close();
        }
        if (mockedRichMediaManager != null) {
            mockedRichMediaManager.close();
        }
    }

    @Test
    public void testCompleteFlow_NoRemoteConfig_UsesGlobalDefaults() throws IOException {

        when(mockConfigFile.exists()).thenReturn(false);
        

        ModalRichmediaConfig globalConfig = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.CENTER)
            .setAnimationDuration(800)
            .setStatusBarCovered(true);
        mockedRichMediaManager.when(RichMediaManager::getDefaultRichMediaConfig)
            .thenReturn(globalConfig);


        ModalRichmediaConfig resourceConfig = inAppConfig.parseModalConfig("test_code");
        Resource mockResource = createMockResource(resourceConfig);
        ModalRichmediaConfig effectiveConfig = ModalConfigResolver.getEffectiveConfig(mockResource);


        assertNull("Resource config should be null when file doesn't exist", resourceConfig);
        assertNotNull("Effective config should not be null", effectiveConfig);
        assertEquals("Should use global view position", ModalRichMediaViewPosition.CENTER, effectiveConfig.getViewPosition());
        assertEquals("Should use global animation duration", Integer.valueOf(800), effectiveConfig.getAnimationDuration());
        assertTrue("Should use global status bar setting", effectiveConfig.isStatusBarCovered());
    }

    @Test
    public void testCompleteFlow_EmptyRemoteConfig_UsesGlobalDefaults() throws IOException {

        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn("{}");
        

        ModalRichmediaConfig globalConfig = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
            .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
            .setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_DOWN);
        mockedRichMediaManager.when(RichMediaManager::getDefaultRichMediaConfig)
            .thenReturn(globalConfig);


        ModalRichmediaConfig resourceConfig = inAppConfig.parseModalConfig("test_code");
        Resource mockResource = createMockResource(resourceConfig);
        ModalRichmediaConfig effectiveConfig = ModalConfigResolver.getEffectiveConfig(mockResource);


        assertNotNull("Resource config should not be null", resourceConfig);
        assertNull("Resource view position should be null", resourceConfig.getViewPosition());
        
        assertNotNull("Effective config should not be null", effectiveConfig);
        assertEquals("Should use global view position", ModalRichMediaViewPosition.BOTTOM, effectiveConfig.getViewPosition());
        assertEquals("Should use global present animation", ModalRichMediaPresentAnimationType.SLIDE_UP, effectiveConfig.getPresentAnimationType());
        assertEquals("Should use global dismiss animation", ModalRichMediaDismissAnimationType.SLIDE_DOWN, effectiveConfig.getDismissAnimationType());
    }

    @Test
    public void testCompleteFlow_PartialRemoteConfig_MixesResourceAndGlobal() throws IOException {

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"top\",\n" +
            "    \"present_animation\": \"fade_in\"\n" +
            "  }\n" +
            "}";
        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn(jsonContent);
        

        ModalRichmediaConfig globalConfig = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.BOTTOM) // Should be overridden
            .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP) // Should be overridden
            .setDismissAnimationType(ModalRichMediaDismissAnimationType.FADE_OUT) // Should be used
            .setWindowWidth(ModalRichMediaWindowWidth.WRAP_CONTENT) // Should be used
            .setAnimationDuration(1200); // Should be used
        mockedRichMediaManager.when(RichMediaManager::getDefaultRichMediaConfig)
            .thenReturn(globalConfig);


        ModalRichmediaConfig resourceConfig = inAppConfig.parseModalConfig("test_code");
        Resource mockResource = createMockResource(resourceConfig);
        ModalRichmediaConfig effectiveConfig = ModalConfigResolver.getEffectiveConfig(mockResource);


        assertNotNull("Resource config should not be null", resourceConfig);
        assertEquals("Resource should have TOP position", ModalRichMediaViewPosition.TOP, resourceConfig.getViewPosition());
        assertEquals("Resource should have FADE_IN animation", ModalRichMediaPresentAnimationType.FADE_IN, resourceConfig.getPresentAnimationType());
        assertNull("Resource should not have dismiss animation", resourceConfig.getDismissAnimationType());
        
        assertNotNull("Effective config should not be null", effectiveConfig);
        assertEquals("Effective should use resource position", ModalRichMediaViewPosition.TOP, effectiveConfig.getViewPosition());
        assertEquals("Effective should use resource present animation", ModalRichMediaPresentAnimationType.FADE_IN, effectiveConfig.getPresentAnimationType());
        assertEquals("Effective should use global dismiss animation", ModalRichMediaDismissAnimationType.FADE_OUT, effectiveConfig.getDismissAnimationType());
        assertEquals("Effective should use global window width", ModalRichMediaWindowWidth.WRAP_CONTENT, effectiveConfig.getWindowWidth());
        assertEquals("Effective should use global animation duration", Integer.valueOf(1200), effectiveConfig.getAnimationDuration());
    }

    @Test
    public void testCompleteFlow_CompleteRemoteConfig_UsesResourceValues() throws IOException {

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"position\": \"center\",\n" +
            "    \"present_animation\": \"up\",\n" +
            "    \"dismiss_animation\": \"down\",\n" +
            "    \"swipe_to_dismiss\": [\"up\", \"down\", \"left\"]\n" +
            "  }\n" +
            "}";
        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn(jsonContent);
        

        ModalRichmediaConfig globalConfig = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
            .setPresentAnimationType(ModalRichMediaPresentAnimationType.FADE_IN)
            .setDismissAnimationType(ModalRichMediaDismissAnimationType.FADE_OUT)
            .setAnimationDuration(500);
        mockedRichMediaManager.when(RichMediaManager::getDefaultRichMediaConfig)
            .thenReturn(globalConfig);


        ModalRichmediaConfig resourceConfig = inAppConfig.parseModalConfig("test_code");
        Resource mockResource = createMockResource(resourceConfig);
        ModalRichmediaConfig effectiveConfig = ModalConfigResolver.getEffectiveConfig(mockResource);


        assertNotNull("Resource config should not be null", resourceConfig);
        assertEquals("Resource should have CENTER position", ModalRichMediaViewPosition.CENTER, resourceConfig.getViewPosition());
        assertEquals("Resource should have SLIDE_UP animation", ModalRichMediaPresentAnimationType.SLIDE_UP, resourceConfig.getPresentAnimationType());
        assertEquals("Resource should have SLIDE_DOWN dismiss", ModalRichMediaDismissAnimationType.SLIDE_DOWN, resourceConfig.getDismissAnimationType());
        
        Set<ModalRichMediaSwipeGesture> expectedGestures = new HashSet<>();
        expectedGestures.add(ModalRichMediaSwipeGesture.UP);
        expectedGestures.add(ModalRichMediaSwipeGesture.DOWN);
        expectedGestures.add(ModalRichMediaSwipeGesture.LEFT);
        assertEquals("Resource should have correct swipe gestures", expectedGestures, resourceConfig.getSwipeGestures());
        
        assertNotNull("Effective config should not be null", effectiveConfig);
        assertEquals("Effective should use resource position", ModalRichMediaViewPosition.CENTER, effectiveConfig.getViewPosition());
        assertEquals("Effective should use resource present animation", ModalRichMediaPresentAnimationType.SLIDE_UP, effectiveConfig.getPresentAnimationType());
        assertEquals("Effective should use resource dismiss animation", ModalRichMediaDismissAnimationType.SLIDE_DOWN, effectiveConfig.getDismissAnimationType());
        assertEquals("Effective should use resource swipe gestures", expectedGestures, effectiveConfig.getSwipeGestures());
        assertEquals("Effective should use global animation duration", Integer.valueOf(500), effectiveConfig.getAnimationDuration());
    }

    @Test
    public void testCompleteFlow_InvalidJson_ReturnsNull() throws IOException {

        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn("{ invalid json }");


        try {
            inAppConfig.parseModalConfig("test_code");
            assertTrue("Should have thrown IOException for invalid JSON", false);
        } catch (IOException e) {

            assertTrue("Exception message should mention malformed config", 
                e.getMessage().contains("Malformed config file"));
        }
    }

    @Test
    public void testCompleteFlow_SwipeGesturesFiltering() throws IOException {

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"swipe_to_dismiss\": [\"up\", \"none\", \"down\", \"invalid_gesture\"]\n" +
            "  }\n" +
            "}";
        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn(jsonContent);
        

        Set<ModalRichMediaSwipeGesture> globalGestures = new HashSet<>();
        globalGestures.add(ModalRichMediaSwipeGesture.LEFT);
        globalGestures.add(ModalRichMediaSwipeGesture.RIGHT);
        ModalRichmediaConfig globalConfig = new ModalRichmediaConfig()
            .setSwipeGestures(globalGestures);
        mockedRichMediaManager.when(RichMediaManager::getDefaultRichMediaConfig)
            .thenReturn(globalConfig);


        ModalRichmediaConfig resourceConfig = inAppConfig.parseModalConfig("test_code");
        Resource mockResource = createMockResource(resourceConfig);
        ModalRichmediaConfig effectiveConfig = ModalConfigResolver.getEffectiveConfig(mockResource);


        assertNotNull("Resource config should not be null", resourceConfig);
        Set<ModalRichMediaSwipeGesture> resourceGestures = resourceConfig.getSwipeGestures();
        assertEquals("Should have 2 valid gestures", 2, resourceGestures.size());
        assertTrue("Should contain UP gesture", resourceGestures.contains(ModalRichMediaSwipeGesture.UP));
        assertTrue("Should contain DOWN gesture", resourceGestures.contains(ModalRichMediaSwipeGesture.DOWN));
        assertFalse("Should not contain NONE gesture", resourceGestures.contains(ModalRichMediaSwipeGesture.NONE));
        
        assertNotNull("Effective config should not be null", effectiveConfig);
        assertEquals("Effective should use resource gestures", resourceGestures, effectiveConfig.getSwipeGestures());
    }

    @Test
    public void testCompleteFlow_EmptySwipeGesturesArray_UsesGlobalDefaults() throws IOException {

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"swipe_to_dismiss\": []\n" +
            "  }\n" +
            "}";
        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn(jsonContent);
        

        Set<ModalRichMediaSwipeGesture> globalGestures = new HashSet<>();
        globalGestures.add(ModalRichMediaSwipeGesture.UP);
        ModalRichmediaConfig globalConfig = new ModalRichmediaConfig()
            .setSwipeGestures(globalGestures);
        mockedRichMediaManager.when(RichMediaManager::getDefaultRichMediaConfig)
            .thenReturn(globalConfig);


        ModalRichmediaConfig resourceConfig = inAppConfig.parseModalConfig("test_code");
        Resource mockResource = createMockResource(resourceConfig);
        ModalRichmediaConfig effectiveConfig = ModalConfigResolver.getEffectiveConfig(mockResource);


        assertNotNull("Resource config should not be null", resourceConfig);
        assertTrue("Resource gestures should be empty", resourceConfig.getSwipeGestures().isEmpty());
        
        assertNotNull("Effective config should not be null", effectiveConfig);
        assertEquals("Effective should use global gestures", globalGestures, effectiveConfig.getSwipeGestures());
    }

    @Test
    public void testCompleteFlow_OnlyNoneGesturesInArray_UsesGlobalDefaults() throws IOException {

        String jsonContent = "{\n" +
            "  \"style_settings\": {\n" +
            "    \"swipe_to_dismiss\": [\"none\", \"none\"]\n" +
            "  }\n" +
            "}";
        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn(jsonContent);
        

        Set<ModalRichMediaSwipeGesture> globalGestures = new HashSet<>();
        globalGestures.add(ModalRichMediaSwipeGesture.LEFT);
        globalGestures.add(ModalRichMediaSwipeGesture.RIGHT);
        ModalRichmediaConfig globalConfig = new ModalRichmediaConfig()
            .setSwipeGestures(globalGestures);
        mockedRichMediaManager.when(RichMediaManager::getDefaultRichMediaConfig)
            .thenReturn(globalConfig);


        ModalRichmediaConfig resourceConfig = inAppConfig.parseModalConfig("test_code");
        Resource mockResource = createMockResource(resourceConfig);
        ModalRichmediaConfig effectiveConfig = ModalConfigResolver.getEffectiveConfig(mockResource);


        assertNotNull("Resource config should not be null", resourceConfig);
        assertTrue("Resource gestures should be empty after filtering NONE", resourceConfig.getSwipeGestures().isEmpty());
        
        assertNotNull("Effective config should not be null", effectiveConfig);
        assertEquals("Effective should use global gestures", globalGestures, effectiveConfig.getSwipeGestures());
    }

    @Test
    public void testFileUtilsIntegration_CallsCorrectMethods() throws IOException {

        String jsonContent = "{}";
        mockedFileUtils.when(() -> FileUtils.readFile(any(File.class)))
            .thenReturn(jsonContent);


        inAppConfig.parseModalConfig("test_code");


        mockedFileUtils.verify(() -> FileUtils.readFile(mockConfigFile));
        verify(mockFolderProvider).getConfigFile("test_code");
    }
}