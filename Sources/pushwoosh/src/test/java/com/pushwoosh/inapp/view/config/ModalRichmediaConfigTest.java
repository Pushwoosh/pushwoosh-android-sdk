package com.pushwoosh.inapp.view.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ModalRichmediaConfigTest {

    @Test
    public void testDefaultValues_ShouldAllBeNull() {

        ModalRichmediaConfig config = new ModalRichmediaConfig();
        

        assertNull("viewPosition should be null by default", config.getViewPosition());
        assertNull("presentAnimationType should be null by default", config.getPresentAnimationType());
        assertNull("dismissAnimationType should be null by default", config.getDismissAnimationType());
        assertNull("windowWidth should be null by default", config.getWindowWidth());
        assertNull("animationDuration should be null by default", config.getAnimationDuration());
        assertNull("statusBarCovered should be null by default", config.isStatusBarCovered());
        

        assertTrue("swipeGestures should return empty set for null", config.getSwipeGestures().isEmpty());
    }
    
    @Test
    public void testSettersAndGetters_ValidValues() {
        ModalRichmediaConfig config = new ModalRichmediaConfig();


        config.setViewPosition(ModalRichMediaViewPosition.CENTER);
        config.setPresentAnimationType(ModalRichMediaPresentAnimationType.FADE_IN);
        config.setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_DOWN);
        config.setWindowWidth(ModalRichMediaWindowWidth.WRAP_CONTENT);
        config.setAnimationDuration(500);
        config.setStatusBarCovered(true);


        assertEquals("viewPosition should be CENTER", ModalRichMediaViewPosition.CENTER, config.getViewPosition());
        assertEquals("presentAnimationType should be FADE_IN", ModalRichMediaPresentAnimationType.FADE_IN, config.getPresentAnimationType());
        assertEquals("dismissAnimationType should be SLIDE_DOWN", ModalRichMediaDismissAnimationType.SLIDE_DOWN, config.getDismissAnimationType());
        assertEquals("windowWidth should be WRAP_CONTENT", ModalRichMediaWindowWidth.WRAP_CONTENT, config.getWindowWidth());
        assertEquals("animationDuration should be 500", Integer.valueOf(500), config.getAnimationDuration());
        assertTrue("statusBarCovered should be true", config.isStatusBarCovered());
    }

    @Test
    public void testSetSwipeGestures_ValidSet() {

        ModalRichmediaConfig config = new ModalRichmediaConfig();
        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.UP);
        gestures.add(ModalRichMediaSwipeGesture.DOWN);
        

        config.setSwipeGestures(gestures);
        

        Set<ModalRichMediaSwipeGesture> result = config.getSwipeGestures();
        assertEquals("Should have 2 gestures", 2, result.size());
        assertTrue("Should contain UP gesture", result.contains(ModalRichMediaSwipeGesture.UP));
        assertTrue("Should contain DOWN gesture", result.contains(ModalRichMediaSwipeGesture.DOWN));
    }
    
    @Test
    public void testSetSwipeGestures_NullInput_ShouldSetToNull() {

        ModalRichmediaConfig config = new ModalRichmediaConfig();
        

        config.setSwipeGestures(null);
        

        assertTrue("Should return empty set for null input", config.getSwipeGestures().isEmpty());
    }
    
    @Test
    public void testSetSwipeGestures_RemovesNoneGesture() {

        ModalRichmediaConfig config = new ModalRichmediaConfig();
        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.UP);
        gestures.add(ModalRichMediaSwipeGesture.NONE); // Should be filtered out
        gestures.add(ModalRichMediaSwipeGesture.DOWN);
        

        config.setSwipeGestures(gestures);
        

        Set<ModalRichMediaSwipeGesture> result = config.getSwipeGestures();
        assertEquals("Should have 2 gestures (NONE filtered out)", 2, result.size());
        assertTrue("Should contain UP gesture", result.contains(ModalRichMediaSwipeGesture.UP));
        assertTrue("Should contain DOWN gesture", result.contains(ModalRichMediaSwipeGesture.DOWN));
        assertFalse("Should not contain NONE gesture", result.contains(ModalRichMediaSwipeGesture.NONE));
    }
    
    @Test
    public void testSetSwipeGestures_OnlyNoneGesture_ResultsInEmptySet() {

        ModalRichmediaConfig config = new ModalRichmediaConfig();
        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.NONE);
        

        config.setSwipeGestures(gestures);
        

        assertTrue("Should be empty when only NONE gesture is provided", config.getSwipeGestures().isEmpty());
    }
    
    @Test
    public void testSetSwipeGestures_CreatesCopyOfInputSet() {

        ModalRichmediaConfig config = new ModalRichmediaConfig();
        Set<ModalRichMediaSwipeGesture> originalGestures = new HashSet<>();
        originalGestures.add(ModalRichMediaSwipeGesture.UP);
        

        config.setSwipeGestures(originalGestures);
        originalGestures.add(ModalRichMediaSwipeGesture.DOWN); // Modify original
        

        Set<ModalRichMediaSwipeGesture> result = config.getSwipeGestures();
        assertEquals("Should only have 1 gesture (unaffected by external modification)", 1, result.size());
        assertTrue("Should contain UP gesture", result.contains(ModalRichMediaSwipeGesture.UP));
        assertFalse("Should not contain DOWN gesture", result.contains(ModalRichMediaSwipeGesture.DOWN));
    }
    
    @Test
    public void testGetSwipeGestures_ReturnsUnmodifiableSet() {

        ModalRichmediaConfig config = new ModalRichmediaConfig();
        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.LEFT);
        config.setSwipeGestures(gestures);
        

        Set<ModalRichMediaSwipeGesture> returnedGestures = config.getSwipeGestures();
        

        assertThrows(UnsupportedOperationException.class, () -> 
            returnedGestures.add(ModalRichMediaSwipeGesture.RIGHT));
        

        Set<ModalRichMediaSwipeGesture> freshResult = config.getSwipeGestures();
        assertEquals("Should only have 1 gesture", 1, freshResult.size());
        assertTrue("Should contain LEFT gesture", freshResult.contains(ModalRichMediaSwipeGesture.LEFT));
        assertFalse("Should not contain RIGHT gesture", freshResult.contains(ModalRichMediaSwipeGesture.RIGHT));
    }
    
    @Test
    public void testBuilderPattern_ChainingMethods() {

        ModalRichmediaConfig config = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.TOP)
            .setAnimationDuration(1000)
            .setStatusBarCovered(false)
            .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
            .setDismissAnimationType(ModalRichMediaDismissAnimationType.FADE_OUT)
            .setWindowWidth(ModalRichMediaWindowWidth.FULL_SCREEN);
        

        assertEquals("viewPosition should be TOP", ModalRichMediaViewPosition.TOP, config.getViewPosition());
        assertEquals("animationDuration should be 1000", Integer.valueOf(1000), config.getAnimationDuration());
        assertFalse("statusBarCovered should be false", config.isStatusBarCovered());
        assertEquals("presentAnimationType should be SLIDE_UP", ModalRichMediaPresentAnimationType.SLIDE_UP, config.getPresentAnimationType());
        assertEquals("dismissAnimationType should be FADE_OUT", ModalRichMediaDismissAnimationType.FADE_OUT, config.getDismissAnimationType());
        assertEquals("windowWidth should be FULL_SCREEN", ModalRichMediaWindowWidth.FULL_SCREEN, config.getWindowWidth());
    }
    
    @Test
    public void testBuilderPattern_WithSwipeGestures() {

        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.LEFT);
        gestures.add(ModalRichMediaSwipeGesture.RIGHT);
        

        ModalRichmediaConfig config = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
            .setSwipeGestures(gestures);
        

        assertEquals("viewPosition should be BOTTOM", ModalRichMediaViewPosition.BOTTOM, config.getViewPosition());
        assertEquals("Should have 2 swipe gestures", 2, config.getSwipeGestures().size());
        assertTrue("Should contain LEFT gesture", config.getSwipeGestures().contains(ModalRichMediaSwipeGesture.LEFT));
        assertTrue("Should contain RIGHT gesture", config.getSwipeGestures().contains(ModalRichMediaSwipeGesture.RIGHT));
    }
    
    @Test
    public void testNullValues_CanBeSetExplicitly() {

        ModalRichmediaConfig config = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.CENTER)
            .setAnimationDuration(500)
            .setStatusBarCovered(true);
        

        config.setViewPosition(null);
        config.setAnimationDuration(null);
        config.setStatusBarCovered(null);
        config.setSwipeGestures(null);
        

        assertNull("viewPosition should be null", config.getViewPosition());
        assertNull("animationDuration should be null", config.getAnimationDuration());
        assertNull("statusBarCovered should be null", config.isStatusBarCovered());
        assertTrue("swipeGestures should return empty set", config.getSwipeGestures().isEmpty());
    }
    
    @Test
    public void testAllEnumValues_CanBeSet() {

        for (ModalRichMediaViewPosition position : ModalRichMediaViewPosition.values()) {
            ModalRichmediaConfig config = new ModalRichmediaConfig().setViewPosition(position);
            assertEquals("Should store " + position + " correctly", position, config.getViewPosition());
        }
        

        for (ModalRichMediaPresentAnimationType animType : ModalRichMediaPresentAnimationType.values()) {
            ModalRichmediaConfig config = new ModalRichmediaConfig().setPresentAnimationType(animType);
            assertEquals("Should store " + animType + " correctly", animType, config.getPresentAnimationType());
        }
        

        for (ModalRichMediaDismissAnimationType animType : ModalRichMediaDismissAnimationType.values()) {
            ModalRichmediaConfig config = new ModalRichmediaConfig().setDismissAnimationType(animType);
            assertEquals("Should store " + animType + " correctly", animType, config.getDismissAnimationType());
        }
        

        for (ModalRichMediaWindowWidth width : ModalRichMediaWindowWidth.values()) {
            ModalRichmediaConfig config = new ModalRichmediaConfig().setWindowWidth(width);
            assertEquals("Should store " + width + " correctly", width, config.getWindowWidth());
        }
    }
    
    @Test
    public void testAllSwipeGestures_CanBeSet() {

        Set<ModalRichMediaSwipeGesture> allGestures = new HashSet<>();
        for (ModalRichMediaSwipeGesture gesture : ModalRichMediaSwipeGesture.values()) {
            if (gesture != ModalRichMediaSwipeGesture.NONE) {
                allGestures.add(gesture);
            }
        }
        

        ModalRichmediaConfig config = new ModalRichmediaConfig().setSwipeGestures(allGestures);
        

        Set<ModalRichMediaSwipeGesture> result = config.getSwipeGestures();
        assertEquals("Should contain all gestures except NONE", 
                     ModalRichMediaSwipeGesture.values().length - 1, result.size());
        
        for (ModalRichMediaSwipeGesture gesture : ModalRichMediaSwipeGesture.values()) {
            if (gesture != ModalRichMediaSwipeGesture.NONE) {
                assertTrue("Should contain " + gesture, result.contains(gesture));
            } else {
                assertFalse("Should not contain NONE", result.contains(gesture));
            }
        }
    }
    
    @Test
    public void testAnimationDuration_BoundaryValues() {
        ModalRichmediaConfig config = new ModalRichmediaConfig();
        

        config.setAnimationDuration(0);
        assertEquals("Should store 0 duration", Integer.valueOf(0), config.getAnimationDuration());
        

        config.setAnimationDuration(Integer.MAX_VALUE);
        assertEquals("Should store max duration", Integer.valueOf(Integer.MAX_VALUE), config.getAnimationDuration());
        

        config.setAnimationDuration(-100);
        assertEquals("Should store negative duration", Integer.valueOf(-100), config.getAnimationDuration());
    }
}