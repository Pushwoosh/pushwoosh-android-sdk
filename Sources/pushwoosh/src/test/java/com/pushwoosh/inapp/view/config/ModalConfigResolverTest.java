package com.pushwoosh.inapp.view.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;
import com.pushwoosh.richmedia.RichMediaManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class ModalConfigResolverTest {

    @Mock
    private Resource mockResource;

    @Mock
    private ModalRichmediaConfig mockResourceConfig;

    @Mock
    private ModalRichmediaConfig mockGlobalConfig;

    @Before
    public void setUp() {

        when(mockGlobalConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
        when(mockGlobalConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(mockGlobalConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT);
        when(mockGlobalConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN);
        when(mockGlobalConfig.getAnimationDuration()).thenReturn(1000);
        when(mockGlobalConfig.isStatusBarCovered()).thenReturn(false);

        Set<ModalRichMediaSwipeGesture> globalGestures = new HashSet<>();
        globalGestures.add(ModalRichMediaSwipeGesture.UP);
        when(mockGlobalConfig.getSwipeGestures()).thenReturn(globalGestures);
    }

    @Test
    public void testGetEffectiveConfig_NoResourceConfig_ReturnsGlobalConfig() {

        when(mockResource.hasResourceModalConfig()).thenReturn(false);
        when(mockResource.getCode()).thenReturn("test_resource");

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertSame("Should return global config when no resource config", mockGlobalConfig, result);
        }
    }

    @Test
    public void testGetEffectiveConfig_ResourceConfigAllNull_UsesGlobalValues() {

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");


        when(mockResourceConfig.getViewPosition()).thenReturn(null);
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(null);
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(null);
        when(mockResourceConfig.getWindowWidth()).thenReturn(null);
        when(mockResourceConfig.getAnimationDuration()).thenReturn(null);
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(null);
        when(mockResourceConfig.getSwipeGestures()).thenReturn(null);

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("viewPosition should use global default", ModalRichMediaViewPosition.CENTER, result.getViewPosition());
            assertEquals("presentAnimationType should use global default", ModalRichMediaPresentAnimationType.FADE_IN, result.getPresentAnimationType());
            assertEquals("dismissAnimationType should use global default", ModalRichMediaDismissAnimationType.FADE_OUT, result.getDismissAnimationType());
            assertEquals("windowWidth should use global default", ModalRichMediaWindowWidth.FULL_SCREEN, result.getWindowWidth());
            assertEquals("animationDuration should use global default", Integer.valueOf(1000), result.getAnimationDuration());
            assertFalse("statusBarCovered should use global default", result.isStatusBarCovered());
            assertEquals("swipeGestures should use global default", 1, result.getSwipeGestures().size());
            assertTrue("should contain global UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
        }
    }

    @Test
    public void testGetEffectiveConfig_ResourceConfigAllSet_UsesResourceValues() {

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");


        when(mockResourceConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.TOP);
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.SLIDE_DOWN);
        when(mockResourceConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.WRAP_CONTENT);
        when(mockResourceConfig.getAnimationDuration()).thenReturn(500);
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(true);

        Set<ModalRichMediaSwipeGesture> resourceGestures = new HashSet<>();
        resourceGestures.add(ModalRichMediaSwipeGesture.DOWN);
        resourceGestures.add(ModalRichMediaSwipeGesture.LEFT);
        when(mockResourceConfig.getSwipeGestures()).thenReturn(resourceGestures);

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("viewPosition should use resource value", ModalRichMediaViewPosition.TOP, result.getViewPosition());
            assertEquals("presentAnimationType should use resource value", ModalRichMediaPresentAnimationType.SLIDE_UP, result.getPresentAnimationType());
            assertEquals("dismissAnimationType should use resource value", ModalRichMediaDismissAnimationType.SLIDE_DOWN, result.getDismissAnimationType());
            assertEquals("windowWidth should use resource value", ModalRichMediaWindowWidth.WRAP_CONTENT, result.getWindowWidth());
            assertEquals("animationDuration should use resource value", Integer.valueOf(500), result.getAnimationDuration());
            assertTrue("statusBarCovered should use resource value", result.isStatusBarCovered());
            assertEquals("swipeGestures should use resource values", 2, result.getSwipeGestures().size());
            assertTrue("should contain resource DOWN gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.DOWN));
            assertTrue("should contain resource LEFT gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.LEFT));
        }
    }

    @Test
    public void testGetEffectiveConfig_MixedResourceConfig_UsesMixOfResourceAndGlobal() {

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");


        when(mockResourceConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.BOTTOM); // Set
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(null); // Should use global
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.SLIDE_LEFT); // Set
        when(mockResourceConfig.getWindowWidth()).thenReturn(null); // Should use global
        when(mockResourceConfig.getAnimationDuration()).thenReturn(750); // Set
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(null); // Should use global
        when(mockResourceConfig.getSwipeGestures()).thenReturn(new HashSet<>()); // Empty, should use global

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("viewPosition should use resource value", ModalRichMediaViewPosition.BOTTOM, result.getViewPosition());
            assertEquals("presentAnimationType should use global value", ModalRichMediaPresentAnimationType.FADE_IN, result.getPresentAnimationType());
            assertEquals("dismissAnimationType should use resource value", ModalRichMediaDismissAnimationType.SLIDE_LEFT, result.getDismissAnimationType());
            assertEquals("windowWidth should use global value", ModalRichMediaWindowWidth.FULL_SCREEN, result.getWindowWidth());
            assertEquals("animationDuration should use resource value", Integer.valueOf(750), result.getAnimationDuration());
            assertFalse("statusBarCovered should use global value", result.isStatusBarCovered());
            assertEquals("swipeGestures should use global value for empty set", 1, result.getSwipeGestures().size());
            assertTrue("should contain global UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
        }
    }

    @Test
    public void testGetEffectiveConfig_EmptySwipeGestures_FallsBackToGlobal() {

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");

        when(mockResourceConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT);
        when(mockResourceConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN);
        when(mockResourceConfig.getAnimationDuration()).thenReturn(1000);
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(false);
        when(mockResourceConfig.getSwipeGestures()).thenReturn(new HashSet<>()); // Empty set

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("should use global gestures for empty resource set", 1, result.getSwipeGestures().size());
            assertTrue("should contain global UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
        }
    }

    @Test
    public void testGetEffectiveConfig_NullSwipeGestures_FallsBackToGlobal() {

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");

        when(mockResourceConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT);
        when(mockResourceConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN);
        when(mockResourceConfig.getAnimationDuration()).thenReturn(1000);
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(false);
        when(mockResourceConfig.getSwipeGestures()).thenReturn(null); // Null

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("should use global gestures for null resource gestures", 1, result.getSwipeGestures().size());
            assertTrue("should contain global UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
        }
    }

    @Test
    public void testGetEffectiveConfig_SingleGestureInResource_UsesResourceGesture() {

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");

        Set<ModalRichMediaSwipeGesture> resourceGestures = new HashSet<>();
        resourceGestures.add(ModalRichMediaSwipeGesture.RIGHT);
        when(mockResourceConfig.getSwipeGestures()).thenReturn(resourceGestures);


        when(mockResourceConfig.getViewPosition()).thenReturn(null);
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(null);
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(null);
        when(mockResourceConfig.getWindowWidth()).thenReturn(null);
        when(mockResourceConfig.getAnimationDuration()).thenReturn(null);
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(null);

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("should use resource gesture", 1, result.getSwipeGestures().size());
            assertTrue("should contain resource RIGHT gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.RIGHT));
            assertFalse("should not contain global UP gesture", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
        }
    }

    @Test
    public void testGetEffectiveConfig_HelperMethodBehavior_GetValueOrDefault() {


        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");


        when(mockResourceConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.BOTTOM); // Should use this
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(null); // Should use global
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.SLIDE_UP); // Should use this
        when(mockResourceConfig.getWindowWidth()).thenReturn(null); // Should use global
        when(mockResourceConfig.getAnimationDuration()).thenReturn(null); // Should use global
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(true); // Should use this
        when(mockResourceConfig.getSwipeGestures()).thenReturn(null); // Should use global

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("getValueOrDefault should use resource when not null", ModalRichMediaViewPosition.BOTTOM, result.getViewPosition());
            assertEquals("getValueOrDefault should use global when resource is null", ModalRichMediaPresentAnimationType.FADE_IN, result.getPresentAnimationType());
            assertEquals("getValueOrDefault should use resource when not null", ModalRichMediaDismissAnimationType.SLIDE_UP, result.getDismissAnimationType());
            assertEquals("getValueOrDefault should use global when resource is null", ModalRichMediaWindowWidth.FULL_SCREEN, result.getWindowWidth());
            assertEquals("getValueOrDefault should use global when resource is null", Integer.valueOf(1000), result.getAnimationDuration());
            assertTrue("getValueOrDefault should use resource when not null", result.isStatusBarCovered());
            assertEquals("getSetValueOrDefault should use global for null", 1, result.getSwipeGestures().size());
        }
    }

    @Test
    public void testGetEffectiveConfig_GlobalConfigWithComplexGestures_PreservesGlobalGestures() {

        Set<ModalRichMediaSwipeGesture> complexGlobalGestures = new HashSet<>();
        complexGlobalGestures.add(ModalRichMediaSwipeGesture.UP);
        complexGlobalGestures.add(ModalRichMediaSwipeGesture.DOWN);
        complexGlobalGestures.add(ModalRichMediaSwipeGesture.LEFT);
        complexGlobalGestures.add(ModalRichMediaSwipeGesture.RIGHT);
        when(mockGlobalConfig.getSwipeGestures()).thenReturn(complexGlobalGestures);

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");


        when(mockResourceConfig.getViewPosition()).thenReturn(null);
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(null);
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(null);
        when(mockResourceConfig.getWindowWidth()).thenReturn(null);
        when(mockResourceConfig.getAnimationDuration()).thenReturn(null);
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(null);
        when(mockResourceConfig.getSwipeGestures()).thenReturn(null);

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertEquals("should preserve all global gestures", 4, result.getSwipeGestures().size());
            assertTrue("should contain UP", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.UP));
            assertTrue("should contain DOWN", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.DOWN));
            assertTrue("should contain LEFT", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.LEFT));
            assertTrue("should contain RIGHT", result.getSwipeGestures().contains(ModalRichMediaSwipeGesture.RIGHT));
        }
    }

    @Test
    public void testGetEffectiveConfig_NewConfigObjectCreated_NotSameAsInputs() {

        when(mockResource.hasResourceModalConfig()).thenReturn(true);
        when(mockResource.getResourceModalConfig()).thenReturn(mockResourceConfig);
        when(mockResource.getCode()).thenReturn("test_resource");

        when(mockResourceConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.TOP);
        when(mockResourceConfig.getPresentAnimationType()).thenReturn(null);
        when(mockResourceConfig.getDismissAnimationType()).thenReturn(null);
        when(mockResourceConfig.getWindowWidth()).thenReturn(null);
        when(mockResourceConfig.getAnimationDuration()).thenReturn(null);
        when(mockResourceConfig.isStatusBarCovered()).thenReturn(null);
        when(mockResourceConfig.getSwipeGestures()).thenReturn(null);

        try (MockedStatic<RichMediaManager> richMediaManagerMock = Mockito.mockStatic(RichMediaManager.class)) {
            richMediaManagerMock.when(RichMediaManager::getDefaultRichMediaConfig).thenReturn(mockGlobalConfig);


            ModalRichmediaConfig result = ModalConfigResolver.getEffectiveConfig(mockResource);


            assertTrue("should create new config object", 
                       result != mockResourceConfig && result != mockGlobalConfig);
            assertEquals("should have merged values", ModalRichMediaViewPosition.TOP, result.getViewPosition());
            assertEquals("should have global fallback", ModalRichMediaPresentAnimationType.FADE_IN, result.getPresentAnimationType());
        }
    }
}