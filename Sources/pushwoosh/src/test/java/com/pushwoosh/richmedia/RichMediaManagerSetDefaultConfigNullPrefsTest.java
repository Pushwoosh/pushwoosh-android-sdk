package com.pushwoosh.richmedia;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
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
public class RichMediaManagerSetDefaultConfigNullPrefsTest {

    @Mock
    private NotificationPrefs mockPrefs;

    @Mock
    private PreferenceIntValue mockIntPref;

    @Mock
    private PreferenceBooleanValue mockBoolPref;

    // Regression guard for crash-setdefaultrichmediaconfig-prefs-null: when
    // RepositoryModule.getNotificationPreferences() is null (init failed / pre-init public-API call),
    // setDefaultRichMediaConfig() used to deref the null prefs at RichMediaManager.java:79 and throw an
    // uncaught NPE on the host stack. It was the missed twin of fix #11; its three prefs-touching
    // siblings (getDefaultRichMediaConfig, setRichMediaType, getRichMediaType) already guard. The fix
    // adds the same guard and makes the setter a no-op on null prefs. Verifies the graceful no-op:
    // the method queries prefs, sees null, and returns without throwing.
    @Test
    public void setDefaultRichMediaConfig_nullPrefs_noOpGracefully() {
        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(null);

            RichMediaManager.setDefaultRichMediaConfig(new ModalRichmediaConfig());

            // reaching here without a throw is the assertion; confirm the guard path actually ran
            // (prefs was queried and found null), so the no-throw is the early-return, not a skipped body
            repositoryMock.verify(RepositoryModule::getNotificationPreferences);
        }
    }

    // Negative control: with non-null prefs (every sub-pref stubbed), setDefaultRichMediaConfig()
    // completes via the normal path and does not throw — proves the guard above is what makes the null
    // case safe, not the config argument or anything else in the method body.
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

        try (MockedStatic<RepositoryModule> repositoryMock = Mockito.mockStatic(RepositoryModule.class)) {
            repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(mockPrefs);

            RichMediaManager.setDefaultRichMediaConfig(new ModalRichmediaConfig());

            // reaching here without a throw is the assertion; ensure the mock was actually wired
            assertNotNull(mockPrefs.richMediaDismissAnimation());
        }
    }
}
