package com.pushwoosh.richmedia;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RichMediaManagerGetDefaultConfigNullPrefsTest {

    @Mock
    private NotificationPrefs mockPrefs;

    @Mock
    private PreferenceIntValue mockIntPref;

    @Mock
    private PreferenceBooleanValue mockBoolPref;

    // Regression guard for crash-getdefaultrichmediaconfig-prefs-null: when
    // RepositoryModule.getNotificationPreferences() is null (init failed / pre-init public-API call),
    // getDefaultRichMediaConfig() used to deref the null prefs and throw an uncaught NPE on the host
    // stack. The fix mirrors the sibling guards (setRichMediaType / getRichMediaType) and returns a
    // default ModalRichmediaConfig instead. Verifies the graceful fallback: no throw, non-null config.
    @Test
    public void getDefaultRichMediaConfig_nullPrefs_returnsDefaultConfigGracefully() {
        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(null);

            ModalRichmediaConfig config = RichMediaManager.getDefaultRichMediaConfig();

            assertNotNull("null prefs must yield a default config, not an NPE", config);
        }
    }

    // Negative control: with non-null prefs (every sub-pref stubbed), getDefaultRichMediaConfig()
    // completes via the normal path and returns a config — keeps the null branch above honest by
    // proving the non-null path is exercised differently.
    @Test
    public void negativeControl_nonNullPrefs_noThrow() {
        when(mockPrefs.richMediaDismissAnimation()).thenReturn(mockIntPref);
        when(mockPrefs.richMediaPresentAnimation()).thenReturn(mockIntPref);
        when(mockPrefs.richMediaSwipeGestureBitMask()).thenReturn(mockIntPref);
        when(mockPrefs.richMediaViewPosition()).thenReturn(mockIntPref);
        when(mockPrefs.richMediaWindowWidth()).thenReturn(mockIntPref);
        when(mockPrefs.richMediaAnimationDuration()).thenReturn(mockIntPref);
        when(mockPrefs.richMediaStatusBarCovered()).thenReturn(mockBoolPref);
        when(mockPrefs.richMediaRespectEdgeToEdgeLayout()).thenReturn(mockBoolPref);

        when(mockIntPref.get()).thenReturn(ModalRichMediaDismissAnimationType.FADE_OUT.getCode());
        when(mockBoolPref.get()).thenReturn(false);

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);

            ModalRichmediaConfig config = RichMediaManager.getDefaultRichMediaConfig();
            assertNotNull("with non-null prefs the method must complete", config);
        }
    }
}
