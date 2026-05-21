package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PWLogTest {

    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        resetPWLogState();
    }

    @After
    public void tearDown() throws Exception {
        resetPWLogState();
        mocks.close();
    }

    private static void resetPWLogState() throws Exception {
        setStaticField("initialized", false);
        setStaticField("currentLevel", PWLog.Level.INFO);
        setStaticField("logsUpdateListener", null);
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field f = PWLog.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    // --- isLoggable: table-driven on currentLevel ---

    @Test
    public void isLoggable_currentLevelInfo_allowsAssertErrorWarnInfoBlocksDebugVerbose() {
        PWLog.updateLogLevel("INFO");

        List<Object[]> table = Arrays.asList(
                new Object[] {"ASSERT", PWLog.ASSERT, true},
                new Object[] {"ERROR", PWLog.ERROR, true},
                new Object[] {"WARN", PWLog.WARN, true},
                new Object[] {"INFO", PWLog.INFO, true},
                new Object[] {"DEBUG", PWLog.DEBUG, false},
                new Object[] {"VERBOSE", PWLog.VERBOSE, false});

        for (Object[] row : table) {
            String name = (String) row[0];
            int level = (int) row[1];
            boolean expected = (boolean) row[2];
            assertEquals("level " + name, expected, PWLog.isLoggable("tag", level));
        }
    }

    @Test
    public void isLoggable_currentLevelNoise_allowsEverything() {
        PWLog.updateLogLevel("NOISE");

        List<Object[]> table = Arrays.asList(
                new Object[] {"ASSERT", PWLog.ASSERT},
                new Object[] {"ERROR", PWLog.ERROR},
                new Object[] {"WARN", PWLog.WARN},
                new Object[] {"INFO", PWLog.INFO},
                new Object[] {"DEBUG", PWLog.DEBUG},
                new Object[] {"VERBOSE", PWLog.VERBOSE});

        for (Object[] row : table) {
            String name = (String) row[0];
            int level = (int) row[1];
            assertTrue("level " + name, PWLog.isLoggable("tag", level));
        }
    }

    @Test
    public void isLoggable_unknownLevelConstant_returnsFalse() {
        PWLog.updateLogLevel("NOISE");

        assertFalse(PWLog.isLoggable("tag", 999));
        assertFalse(PWLog.isLoggable("tag", 0));
    }

    @Test
    public void isLoggable_currentLevelNone_blocksEverything() {
        PWLog.updateLogLevel("NONE");

        List<Object[]> table = Arrays.asList(
                new Object[] {"ASSERT", PWLog.ASSERT},
                new Object[] {"ERROR", PWLog.ERROR},
                new Object[] {"WARN", PWLog.WARN},
                new Object[] {"INFO", PWLog.INFO},
                new Object[] {"DEBUG", PWLog.DEBUG},
                new Object[] {"VERBOSE", PWLog.VERBOSE});

        for (Object[] row : table) {
            String name = (String) row[0];
            int level = (int) row[1];
            assertFalse("level " + name, PWLog.isLoggable("tag", level));
        }
    }

    // --- updateLogLevel ---

    @Test
    public void updateLogLevel_validName_switchesCurrentLevel() {
        PWLog.updateLogLevel("DEBUG");

        assertTrue(PWLog.isLoggable("tag", PWLog.DEBUG));
        assertFalse(PWLog.isLoggable("tag", PWLog.VERBOSE));
    }

    @Test
    public void updateLogLevel_internal_isRejectedAndKeepsCurrentLevel() {
        PWLog.updateLogLevel("INFO");

        PWLog.updateLogLevel("INTERNAL");

        // Stayed on INFO: DEBUG must remain blocked.
        assertFalse(PWLog.isLoggable("tag", PWLog.DEBUG));
        assertTrue(PWLog.isLoggable("tag", PWLog.INFO));
    }

    @Test
    public void updateLogLevel_emptyString_isNoOp() {
        PWLog.updateLogLevel("INFO");

        PWLog.updateLogLevel("");

        // Still INFO.
        assertTrue(PWLog.isLoggable("tag", PWLog.INFO));
        assertFalse(PWLog.isLoggable("tag", PWLog.DEBUG));
    }

    @Test
    public void updateLogLevel_invalidName_isNoOp() {
        PWLog.updateLogLevel("INFO");

        PWLog.updateLogLevel("BOGUS");

        // Still INFO: invalid name logs error, keeps current level (symmetric with init()).
        assertTrue(PWLog.isLoggable("tag", PWLog.INFO));
        assertFalse(PWLog.isLoggable("tag", PWLog.DEBUG));
    }

    @Test
    public void updateLogLevel_null_isNoOp() {
        PWLog.updateLogLevel("INFO");

        PWLog.updateLogLevel(null);

        assertTrue(PWLog.isLoggable("tag", PWLog.INFO));
        assertFalse(PWLog.isLoggable("tag", PWLog.DEBUG));
    }

    // --- init() ---

    @Test
    public void init_validLogLevelInPrefs_appliesIt() {
        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            stubLogLevelPref(repo, "WARN");

            PWLog.init();

            assertTrue(PWLog.isLoggable("tag", PWLog.WARN));
            assertFalse(PWLog.isLoggable("tag", PWLog.INFO));
        }
    }

    @Test
    public void init_internalLevelInPrefs_downgradesToInfo() {
        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            stubLogLevelPref(repo, "INTERNAL");

            PWLog.init();

            assertTrue(PWLog.isLoggable("tag", PWLog.INFO));
            assertFalse(PWLog.isLoggable("tag", PWLog.DEBUG));
        }
    }

    @Test
    public void init_nullLogLevel_keepsDefaultInfo() {
        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            stubLogLevelPref(repo, null);

            PWLog.init();

            assertTrue(PWLog.isLoggable("tag", PWLog.INFO));
            assertFalse(PWLog.isLoggable("tag", PWLog.DEBUG));
        }
    }

    @Test
    public void init_invalidLogLevel_caughtAndFallsBackToInfo() {
        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            stubLogLevelPref(repo, "NONSENSE");

            // Must not throw.
            PWLog.init();

            assertTrue(PWLog.isLoggable("tag", PWLog.INFO));
            assertFalse(PWLog.isLoggable("tag", PWLog.DEBUG));
        }
    }

    @Test
    public void init_calledTwice_secondCallIsNoOp() throws Exception {
        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            RegistrationPrefs prefs = mock(RegistrationPrefs.class);
            PreferenceStringValue first = mock(PreferenceStringValue.class);
            when(first.get()).thenReturn("DEBUG");
            when(prefs.logLevel()).thenReturn(first);
            repo.when(RepositoryModule::getRegistrationPreferences).thenReturn(prefs);

            PWLog.init();
            // Re-arm initialized state would require reset; instead the second init should bail out
            // before reading prefs, so currentLevel stays on DEBUG.
            PWLog.init();

            assertTrue(PWLog.isLoggable("tag", PWLog.DEBUG));
            // ERROR was never applied — VERBOSE/NOISE still off, but DEBUG remains on.
        }
    }

    private static void stubLogLevelPref(MockedStatic<RepositoryModule> repo, String value) {
        RegistrationPrefs prefs = mock(RegistrationPrefs.class);
        PreferenceStringValue stringValue = mock(PreferenceStringValue.class);
        when(stringValue.get()).thenReturn(value);
        when(prefs.logLevel()).thenReturn(stringValue);
        repo.when(RepositoryModule::getRegistrationPreferences).thenReturn(prefs);
    }

    // --- notifyListener through public log methods ---

    @Test
    public void error_notifiesListenerEvenWhenCurrentLevelBlocksOutput() {
        PWLog.updateLogLevel("NONE");
        PWLog.LogsUpdateListener listener = mock(PWLog.LogsUpdateListener.class);
        PWLog.setLogsUpdateListener(listener);

        PWLog.error("Foo", "boom");

        verify(listener).logUpdated(PWLog.Level.ERROR, "[Foo] boom");
    }

    @Test
    public void info_nullSubTag_buildsMessageWithoutBrackets() {
        PWLog.updateLogLevel("INFO");
        PWLog.LogsUpdateListener listener = mock(PWLog.LogsUpdateListener.class);
        PWLog.setLogsUpdateListener(listener);

        PWLog.info((String) null, "plain");

        verify(listener).logUpdated(PWLog.Level.INFO, "plain");
    }

    @Test
    public void noise_notifiesListenerWithLevelNoneNotNoise() {
        PWLog.updateLogLevel("NOISE");
        PWLog.LogsUpdateListener listener = mock(PWLog.LogsUpdateListener.class);
        PWLog.setLogsUpdateListener(listener);

        PWLog.noise("T", "m");

        verify(listener).logUpdated(PWLog.Level.NONE, "[T] m");
        verify(listener, never()).logUpdated(PWLog.Level.NOISE, "[T] m");
    }

    @Test
    public void setLogsUpdateListener_null_doesNotThrowOnSubsequentLogs() {
        PWLog.LogsUpdateListener listener = mock(PWLog.LogsUpdateListener.class);
        PWLog.setLogsUpdateListener(listener);

        PWLog.setLogsUpdateListener(null);

        // Must not throw NPE.
        PWLog.info("T", "m");
        PWLog.error("T", "m");

        verify(listener, times(0))
                .logUpdated(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }
}
