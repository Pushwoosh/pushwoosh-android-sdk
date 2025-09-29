package com.pushwoosh.richmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;

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
public class RichMediaManagerTest {

    @Mock
    private NotificationPrefs mockPrefs;

    @Mock
    private ModalRichmediaConfig mockConfig;


    @Mock
    private PreferenceIntValue mockDismissAnimPref;
    @Mock
    private PreferenceIntValue mockPresentAnimPref;
    @Mock
    private PreferenceIntValue mockSwipeGesturePref;
    @Mock
    private PreferenceIntValue mockViewPositionPref;
    @Mock
    private PreferenceIntValue mockWindowWidthPref;
    @Mock
    private PreferenceBooleanValue mockStatusBarPref;
    @Mock
    private PreferenceBooleanValue mockRespectEdgeToEdgePref;
    @Mock
    private PreferenceIntValue mockAnimationDurationPref;

    @Before
    public void setUp() {

        when(mockPrefs.richMediaDismissAnimation()).thenReturn(mockDismissAnimPref);
        when(mockPrefs.richMediaPresentAnimation()).thenReturn(mockPresentAnimPref);
        when(mockPrefs.richMediaSwipeGestureBitMask()).thenReturn(mockSwipeGesturePref);
        when(mockPrefs.richMediaViewPosition()).thenReturn(mockViewPositionPref);
        when(mockPrefs.richMediaWindowWidth()).thenReturn(mockWindowWidthPref);
        when(mockPrefs.richMediaStatusBarCovered()).thenReturn(mockStatusBarPref);
        when(mockPrefs.richMediaRespectEdgeToEdgeLayout()).thenReturn(mockRespectEdgeToEdgePref);
        when(mockPrefs.richMediaAnimationDuration()).thenReturn(mockAnimationDurationPref);
    }

    @Test
    public void testSetDefaultRichMediaConfig_AllValuesSet_StoresAllValues() {

        when(mockConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.SLIDE_DOWN);
        when(mockConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_UP);
        when(mockConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.TOP);
        when(mockConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.WRAP_CONTENT);
        when(mockConfig.isStatusBarCovered()).thenReturn(true);
        when(mockConfig.shouldRespectEdgeToEdgeLayout()).thenReturn(false);
        when(mockConfig.getAnimationDuration()).thenReturn(500);

        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.UP);
        gestures.add(ModalRichMediaSwipeGesture.DOWN);
        when(mockConfig.getSwipeGestures()).thenReturn(gestures);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            RichMediaManager.setDefaultRichMediaConfig(mockConfig);


            verify(mockDismissAnimPref).set(ModalRichMediaDismissAnimationType.SLIDE_DOWN.getCode());
            verify(mockPresentAnimPref).set(ModalRichMediaPresentAnimationType.SLIDE_UP.getCode());
            verify(mockViewPositionPref).set(ModalRichMediaViewPosition.TOP.getCode());
            verify(mockWindowWidthPref).set(ModalRichMediaWindowWidth.WRAP_CONTENT.getCode());
            verify(mockStatusBarPref).set(true);
            verify(mockRespectEdgeToEdgePref).set(false);
            verify(mockAnimationDurationPref).set(500);


            int expectedMask = ModalRichMediaSwipeGesture.UP.getBit() | ModalRichMediaSwipeGesture.DOWN.getBit();
            verify(mockSwipeGesturePref).set(expectedMask);
        }
    }

    @Test
    public void testSetDefaultRichMediaConfig_NullValues_UsesDefaults() {

        when(mockConfig.getDismissAnimationType()).thenReturn(null);
        when(mockConfig.getPresentAnimationType()).thenReturn(null);
        when(mockConfig.getViewPosition()).thenReturn(null);
        when(mockConfig.getWindowWidth()).thenReturn(null);
        when(mockConfig.isStatusBarCovered()).thenReturn(null);
        when(mockConfig.shouldRespectEdgeToEdgeLayout()).thenReturn(null);
        when(mockConfig.getAnimationDuration()).thenReturn(null);
        when(mockConfig.getSwipeGestures()).thenReturn(null);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            RichMediaManager.setDefaultRichMediaConfig(mockConfig);


            verify(mockDismissAnimPref).set(ModalRichMediaDismissAnimationType.FADE_OUT.getCode());
            verify(mockPresentAnimPref).set(ModalRichMediaPresentAnimationType.FADE_IN.getCode());
            verify(mockViewPositionPref).set(ModalRichMediaViewPosition.CENTER.getCode());
            verify(mockWindowWidthPref).set(ModalRichMediaWindowWidth.FULL_SCREEN.getCode());
            verify(mockStatusBarPref).set(false);
            verify(mockRespectEdgeToEdgePref).set(true); // Default true for edge-to-edge
            verify(mockAnimationDurationPref).set(1000);
            verify(mockSwipeGesturePref).set(0); // Empty gesture set = 0 mask
        }
    }

    @Test
    public void testSetDefaultRichMediaConfig_EmptySwipeGestures_StoresZeroMask() {

        when(mockConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT);
        when(mockConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(mockConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
        when(mockConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN);
        when(mockConfig.isStatusBarCovered()).thenReturn(false);
        when(mockConfig.shouldRespectEdgeToEdgeLayout()).thenReturn(true);
        when(mockConfig.getAnimationDuration()).thenReturn(1000);
        when(mockConfig.getSwipeGestures()).thenReturn(new HashSet<>()); // Empty set

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            RichMediaManager.setDefaultRichMediaConfig(mockConfig);


            verify(mockSwipeGesturePref).set(0);
        }
    }

    @Test
    public void testSetDefaultRichMediaConfig_MixedNullAndValidValues_HandlesCorrectly() {

        when(mockConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.SLIDE_LEFT);
        when(mockConfig.getPresentAnimationType()).thenReturn(null); // Null
        when(mockConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.BOTTOM);
        when(mockConfig.getWindowWidth()).thenReturn(null); // Null
        when(mockConfig.isStatusBarCovered()).thenReturn(true);
        when(mockConfig.shouldRespectEdgeToEdgeLayout()).thenReturn(null); // Null
        when(mockConfig.getAnimationDuration()).thenReturn(null); // Null

        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.LEFT);
        when(mockConfig.getSwipeGestures()).thenReturn(gestures);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            RichMediaManager.setDefaultRichMediaConfig(mockConfig);


            verify(mockDismissAnimPref).set(ModalRichMediaDismissAnimationType.SLIDE_LEFT.getCode());
            verify(mockPresentAnimPref).set(ModalRichMediaPresentAnimationType.FADE_IN.getCode()); // Default
            verify(mockViewPositionPref).set(ModalRichMediaViewPosition.BOTTOM.getCode());
            verify(mockWindowWidthPref).set(ModalRichMediaWindowWidth.FULL_SCREEN.getCode()); // Default
            verify(mockStatusBarPref).set(true);
            verify(mockRespectEdgeToEdgePref).set(true); // Default true for edge-to-edge
            verify(mockAnimationDurationPref).set(1000); // Default
            verify(mockSwipeGesturePref).set(ModalRichMediaSwipeGesture.LEFT.getBit());
        }
    }

    @Test
    public void testGetDefaultRichMediaConfig_ReturnsConfigFromPreferences() {

        when(mockDismissAnimPref.get()).thenReturn(ModalRichMediaDismissAnimationType.SLIDE_LEFT.getCode());
        when(mockPresentAnimPref.get()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_FROM_RIGHT.getCode());
        when(mockViewPositionPref.get()).thenReturn(ModalRichMediaViewPosition.BOTTOM.getCode());
        when(mockWindowWidthPref.get()).thenReturn(ModalRichMediaWindowWidth.WRAP_CONTENT.getCode());
        when(mockStatusBarPref.get()).thenReturn(true);
        when(mockRespectEdgeToEdgePref.get()).thenReturn(false);
        when(mockAnimationDurationPref.get()).thenReturn(750);


        int gestureMask = ModalRichMediaSwipeGesture.UP.getBit() | ModalRichMediaSwipeGesture.RIGHT.getBit();
        when(mockSwipeGesturePref.get()).thenReturn(gestureMask);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            ModalRichmediaConfig result = RichMediaManager.getDefaultRichMediaConfig();


            assertEquals("should restore dismiss animation", ModalRichMediaDismissAnimationType.SLIDE_LEFT, result.getDismissAnimationType());
            assertEquals("should restore present animation", ModalRichMediaPresentAnimationType.SLIDE_FROM_RIGHT, result.getPresentAnimationType());
            assertEquals("should restore view position", ModalRichMediaViewPosition.BOTTOM, result.getViewPosition());
            assertEquals("should restore window width", ModalRichMediaWindowWidth.WRAP_CONTENT, result.getWindowWidth());
            assertTrue("should restore status bar covered", result.isStatusBarCovered());
            assertFalse("should restore edge-to-edge respect", result.shouldRespectEdgeToEdgeLayout());
            assertEquals("should restore animation duration", Integer.valueOf(750), result.getAnimationDuration());

            Set<ModalRichMediaSwipeGesture> gestures = result.getSwipeGestures();
            assertEquals("should have 2 gestures", 2, gestures.size());
            assertTrue("should contain UP gesture", gestures.contains(ModalRichMediaSwipeGesture.UP));
            assertTrue("should contain RIGHT gesture", gestures.contains(ModalRichMediaSwipeGesture.RIGHT));
        }
    }

    @Test
    public void testGetDefaultRichMediaConfig_ZeroGestureMask_ReturnsEmptyGestureSet() {

        when(mockDismissAnimPref.get()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT.getCode());
        when(mockPresentAnimPref.get()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN.getCode());
        when(mockViewPositionPref.get()).thenReturn(ModalRichMediaViewPosition.CENTER.getCode());
        when(mockWindowWidthPref.get()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN.getCode());
        when(mockStatusBarPref.get()).thenReturn(false);
        when(mockRespectEdgeToEdgePref.get()).thenReturn(true);
        when(mockAnimationDurationPref.get()).thenReturn(1000);
        when(mockSwipeGesturePref.get()).thenReturn(0); // No gestures

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            ModalRichmediaConfig result = RichMediaManager.getDefaultRichMediaConfig();


            assertTrue("should have empty gesture set for zero mask", result.getSwipeGestures().isEmpty());
        }
    }

    @Test
    public void testSwipeGestureBitmaskCalculation_SingleGesture() {

        when(mockConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT);
        when(mockConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(mockConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
        when(mockConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN);
        when(mockConfig.isStatusBarCovered()).thenReturn(false);
        when(mockConfig.shouldRespectEdgeToEdgeLayout()).thenReturn(true);
        when(mockConfig.getAnimationDuration()).thenReturn(1000);

        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.UP);
        when(mockConfig.getSwipeGestures()).thenReturn(gestures);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            RichMediaManager.setDefaultRichMediaConfig(mockConfig);


            verify(mockSwipeGesturePref).set(ModalRichMediaSwipeGesture.UP.getBit());
        }
    }

    @Test
    public void testSwipeGestureBitmaskCalculation_MultipleGestures() {

        when(mockConfig.getDismissAnimationType()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT);
        when(mockConfig.getPresentAnimationType()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN);
        when(mockConfig.getViewPosition()).thenReturn(ModalRichMediaViewPosition.CENTER);
        when(mockConfig.getWindowWidth()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN);
        when(mockConfig.isStatusBarCovered()).thenReturn(false);
        when(mockConfig.shouldRespectEdgeToEdgeLayout()).thenReturn(true);
        when(mockConfig.getAnimationDuration()).thenReturn(1000);

        Set<ModalRichMediaSwipeGesture> gestures = new HashSet<>();
        gestures.add(ModalRichMediaSwipeGesture.UP);
        gestures.add(ModalRichMediaSwipeGesture.DOWN);
        gestures.add(ModalRichMediaSwipeGesture.LEFT);
        gestures.add(ModalRichMediaSwipeGesture.RIGHT);
        when(mockConfig.getSwipeGestures()).thenReturn(gestures);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            RichMediaManager.setDefaultRichMediaConfig(mockConfig);


            int expectedMask = ModalRichMediaSwipeGesture.UP.getBit() |
                              ModalRichMediaSwipeGesture.DOWN.getBit() |
                              ModalRichMediaSwipeGesture.LEFT.getBit() |
                              ModalRichMediaSwipeGesture.RIGHT.getBit();
            verify(mockSwipeGesturePref).set(expectedMask);
        }
    }

    @Test
    public void testSwipeGestureBitmaskDecoding_AllGestures() {

        int allGesturesMask = ModalRichMediaSwipeGesture.UP.getBit() |
                             ModalRichMediaSwipeGesture.DOWN.getBit() |
                             ModalRichMediaSwipeGesture.LEFT.getBit() |
                             ModalRichMediaSwipeGesture.RIGHT.getBit();

        when(mockDismissAnimPref.get()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT.getCode());
        when(mockPresentAnimPref.get()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN.getCode());
        when(mockViewPositionPref.get()).thenReturn(ModalRichMediaViewPosition.CENTER.getCode());
        when(mockWindowWidthPref.get()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN.getCode());
        when(mockStatusBarPref.get()).thenReturn(false);
        when(mockRespectEdgeToEdgePref.get()).thenReturn(true);
        when(mockAnimationDurationPref.get()).thenReturn(1000);
        when(mockSwipeGesturePref.get()).thenReturn(allGesturesMask);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            ModalRichmediaConfig result = RichMediaManager.getDefaultRichMediaConfig();


            Set<ModalRichMediaSwipeGesture> gestures = result.getSwipeGestures();
            assertEquals("should have all 4 gestures", 4, gestures.size());
            assertTrue("should contain UP", gestures.contains(ModalRichMediaSwipeGesture.UP));
            assertTrue("should contain DOWN", gestures.contains(ModalRichMediaSwipeGesture.DOWN));
            assertTrue("should contain LEFT", gestures.contains(ModalRichMediaSwipeGesture.LEFT));
            assertTrue("should contain RIGHT", gestures.contains(ModalRichMediaSwipeGesture.RIGHT));
        }
    }

    @Test
    public void testRoundTripConfigStorage_PreservesAllValues() {


        ModalRichmediaConfig originalConfig = new ModalRichmediaConfig()
            .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
            .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_FROM_RIGHT)
            .setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_LEFT)
            .setWindowWidth(ModalRichMediaWindowWidth.WRAP_CONTENT)
            .setAnimationDuration(2000)
            .setStatusBarCovered(true);

        Set<ModalRichMediaSwipeGesture> originalGestures = new HashSet<>();
        originalGestures.add(ModalRichMediaSwipeGesture.DOWN);
        originalGestures.add(ModalRichMediaSwipeGesture.RIGHT);
        originalConfig.setSwipeGestures(originalGestures);


        when(mockDismissAnimPref.get()).thenReturn(ModalRichMediaDismissAnimationType.SLIDE_LEFT.getCode());
        when(mockPresentAnimPref.get()).thenReturn(ModalRichMediaPresentAnimationType.SLIDE_FROM_RIGHT.getCode());
        when(mockViewPositionPref.get()).thenReturn(ModalRichMediaViewPosition.BOTTOM.getCode());
        when(mockWindowWidthPref.get()).thenReturn(ModalRichMediaWindowWidth.WRAP_CONTENT.getCode());
        when(mockStatusBarPref.get()).thenReturn(true);
        when(mockRespectEdgeToEdgePref.get()).thenReturn(false);
        when(mockAnimationDurationPref.get()).thenReturn(2000);
        
        int expectedMask = ModalRichMediaSwipeGesture.DOWN.getBit() | ModalRichMediaSwipeGesture.RIGHT.getBit();
        when(mockSwipeGesturePref.get()).thenReturn(expectedMask);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);




            ModalRichmediaConfig retrievedConfig = RichMediaManager.getDefaultRichMediaConfig();


            assertEquals("viewPosition should match", ModalRichMediaViewPosition.BOTTOM, retrievedConfig.getViewPosition());
            assertEquals("presentAnimationType should match", ModalRichMediaPresentAnimationType.SLIDE_FROM_RIGHT, retrievedConfig.getPresentAnimationType());
            assertEquals("dismissAnimationType should match", ModalRichMediaDismissAnimationType.SLIDE_LEFT, retrievedConfig.getDismissAnimationType());
            assertEquals("windowWidth should match", ModalRichMediaWindowWidth.WRAP_CONTENT, retrievedConfig.getWindowWidth());
            assertEquals("animationDuration should match", Integer.valueOf(2000), retrievedConfig.getAnimationDuration());
            assertTrue("statusBarCovered should match", retrievedConfig.isStatusBarCovered());
            assertFalse("respectEdgeToEdgeLayout should match", retrievedConfig.shouldRespectEdgeToEdgeLayout());
            
            Set<ModalRichMediaSwipeGesture> retrievedGestures = retrievedConfig.getSwipeGestures();
            assertEquals("should have same number of gestures", 2, retrievedGestures.size());
            assertTrue("should contain DOWN gesture", retrievedGestures.contains(ModalRichMediaSwipeGesture.DOWN));
            assertTrue("should contain RIGHT gesture", retrievedGestures.contains(ModalRichMediaSwipeGesture.RIGHT));
        }
    }

    @Test
    public void testSwipeGestureBitmaskHandling_NoGesturesWithNonZeroBit() {


        when(mockDismissAnimPref.get()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT.getCode());
        when(mockPresentAnimPref.get()).thenReturn(ModalRichMediaPresentAnimationType.FADE_IN.getCode());
        when(mockViewPositionPref.get()).thenReturn(ModalRichMediaViewPosition.CENTER.getCode());
        when(mockWindowWidthPref.get()).thenReturn(ModalRichMediaWindowWidth.FULL_SCREEN.getCode());
        when(mockStatusBarPref.get()).thenReturn(false);
        when(mockRespectEdgeToEdgePref.get()).thenReturn(true);
        when(mockAnimationDurationPref.get()).thenReturn(1000);


        int gestureMask = ModalRichMediaSwipeGesture.LEFT.getBit();
        when(mockSwipeGesturePref.get()).thenReturn(gestureMask);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);


            ModalRichmediaConfig result = RichMediaManager.getDefaultRichMediaConfig();


            Set<ModalRichMediaSwipeGesture> gestures = result.getSwipeGestures();
            assertEquals("should have 1 gesture", 1, gestures.size());
            assertTrue("should contain LEFT gesture", gestures.contains(ModalRichMediaSwipeGesture.LEFT));
            

            assertFalse("should not contain NONE gesture", gestures.contains(ModalRichMediaSwipeGesture.NONE));
        }
    }
}