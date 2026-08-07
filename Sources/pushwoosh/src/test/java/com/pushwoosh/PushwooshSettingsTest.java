package com.pushwoosh;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushwooshSettingsTest {
    private static final String FIELD_MANAGER = "notificationManager";

    private PlatformTestManager platformTestManager;
    private PushwooshNotificationManager originalNotificationManager;

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {
        if (originalNotificationManager != null) {
            WhiteboxHelper.setInternalState(Pushwoosh.getInstance(), FIELD_MANAGER, originalNotificationManager);
            originalNotificationManager = null;
        }
        platformTestManager.tearDown();
    }

    //
    // Facade validation part
    // -----------------------------------------------------------------------

    // Pushwoosh.INSTANCE captures PushwooshPlatform at first touch; every facade test builds the
    // platform first, then swaps in its own manager mock so facade validation is observed directly.
    // The mock MUST be restored in tearDown: INSTANCE is static and outlives the test class in a
    // shared Robolectric sandbox, so a leaked mock silently disarms Pushwoosh in later test classes.
    private PushwooshNotificationManager injectNotificationManagerMock() {
        PushwooshNotificationManager managerMock = Mockito.mock(PushwooshNotificationManager.class);
        originalNotificationManager =
                (PushwooshNotificationManager) WhiteboxHelper.getInternalState(Pushwoosh.getInstance(), FIELD_MANAGER);
        WhiteboxHelper.setInternalState(Pushwoosh.getInstance(), FIELD_MANAGER, managerMock);
        return managerMock;
    }

    // A valid pair reaches the manager as one call, the app code trimmed and the url passed raw —
    // normalization and validation happen once, in RegistrationPrefs.applyAppCode.
    @Test
    public void setAppId_validPair_passesTrimmedCodeAndRawUrlToManager() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager managerMock = injectNotificationManagerMock();

        Pushwoosh.getInstance().setAppId("  XXXXX-XXXXX  ", "https://api.example.com/json/1.3");

        verify(managerMock).setAppId("XXXXX-XXXXX", "https://api.example.com/json/1.3");
    }

    // An invalid url is not filtered by the facade: it goes through to the manager and is rejected
    // at the single validation point (applyAppCode), which cancels the whole call there.
    @Test
    public void setAppId_invalidBaseUrl_passedThroughToManager() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager managerMock = injectNotificationManagerMock();

        Pushwoosh.getInstance().setAppId("XXXXX-XXXXX", "not-a-url");

        verify(managerMock).setAppId("XXXXX-XXXXX", "not-a-url");
    }

    // Invalid app code cancels the whole call: the url must not be applied either.
    @Test
    public void setAppId_emptyAppIdWithBaseUrl_ignoresWholeCall() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager managerMock = injectNotificationManagerMock();

        Pushwoosh.getInstance().setAppId("   ", "https://api.example.com/json/1.3/");

        Mockito.verifyNoInteractions(managerMock);
    }

    // A null url means "no explicit url", not an invalid one: a wrapper bridge that passes an absent
    // optional argument straight through must still apply the application code.
    @Test
    public void setAppId_nullBaseUrl_appliesAppIdWithDerivedUrl() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager managerMock = injectNotificationManagerMock();

        Pushwoosh.getInstance().setAppId("  XXXXX-XXXXX  ", null);

        verify(managerMock).setAppId("XXXXX-XXXXX");
        verify(managerMock, never()).setAppId(anyString(), any());
    }

    // An empty url is the other shape of "no explicit url": bridges pass "" when the optional
    // argument is omitted, and the application code must not be dropped because of it.
    @Test
    public void setAppId_emptyBaseUrl_appliesAppIdWithDerivedUrl() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager managerMock = injectNotificationManagerMock();

        Pushwoosh.getInstance().setAppId("  XXXXX-XXXXX  ", "");

        verify(managerMock).setAppId("XXXXX-XXXXX");
        verify(managerMock, never()).setAppId(anyString(), any());
    }
}
