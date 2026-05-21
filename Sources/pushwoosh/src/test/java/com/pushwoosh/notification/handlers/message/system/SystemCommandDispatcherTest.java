package com.pushwoosh.notification.handlers.message.system;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class SystemCommandDispatcherTest {

    @Mock
    private RegistrationPrefs registrationPrefs;

    @Mock
    private PreferenceStringValue logLevelPref;

    @Mock
    private RequestManager requestManager;

    private AutoCloseable mocks;
    private SystemCommandDispatcher dispatcher;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(registrationPrefs.logLevel()).thenReturn(logLevelPref);
        dispatcher = new SystemCommandDispatcher();
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    private Bundle systemPushBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_system_push", "1");
        return bundle;
    }

    // Verifies that non-system push is ignored and dispatcher returns false.
    @Test
    public void preHandleMessage_nonSystemPush_returnsFalseWithoutTouchingStatics() {
        Bundle bundle = new Bundle();

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            boolean result = dispatcher.preHandleMessage(bundle);

            assertFalse(result);
            repoMock.verifyNoInteractions();
            netMock.verifyNoInteractions();
        }
    }

    // Verifies that system push with no command fields is consumed but no handler fires.
    @Test
    public void preHandleMessage_systemPushWithoutCommand_returnsTrueAndConsumes() {
        Bundle bundle = systemPushBundle();

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            repoMock.verifyNoInteractions();
            netMock.verifyNoInteractions();
        }
    }

    // Verifies that legacy setLogLevel command writes the value to prefs and updates PWLog level.
    @Test
    public void preHandleMessage_legacySetLogLevelWithValue_writesPrefAndUpdatesLogLevel() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "setLogLevel");
        bundle.putString("value", "INFO");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<PWLog> pwLogMock = mockStatic(PWLog.class)) {
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            verify(logLevelPref, times(1)).set("INFO");
            pwLogMock.verify(() -> PWLog.updateLogLevel("INFO"), times(1));
        }
    }

    // Verifies that legacy set_base_url command delegates to RequestManager and propagates its result.
    @Test
    public void preHandleMessage_legacySetBaseUrlWithValue_delegatesToRequestManager() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "set_base_url");
        bundle.putString("value", "https://new.example.com");

        try (MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);
            when(requestManager.updateBaseUrl("https://new.example.com")).thenReturn(true);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            verify(requestManager, times(1)).updateBaseUrl("https://new.example.com");
        }
    }

    // Verifies that legacy setLogLevel without value short-circuits handler to false.
    @Test
    public void preHandleMessage_legacySetLogLevelWithoutValue_returnsFalseAndSkipsPref() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "setLogLevel");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<PWLog> pwLogMock = mockStatic(PWLog.class)) {
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertFalse(result);
            verify(logLevelPref, never()).set(anyString());
            pwLogMock.verify(() -> PWLog.updateLogLevel(anyString()), never());
        }
    }

    // Verifies that an unknown legacy command returns false without touching any handler-backing statics.
    @Test
    public void preHandleMessage_legacyUnknownCommand_returnsFalse() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "unknownCmd");
        bundle.putString("value", "x");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            boolean result = dispatcher.preHandleMessage(bundle);

            assertFalse(result);
            repoMock.verifyNoInteractions();
            netMock.verifyNoInteractions();
        }
    }

    // Verifies that set_base_url returns false when RequestManager is not initialized.
    @Test
    public void preHandleMessage_legacySetBaseUrlNullRequestManager_returnsFalse() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "set_base_url");
        bundle.putString("value", "https://x");

        try (MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            netMock.when(NetworkModule::getRequestManager).thenReturn(null);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertFalse(result);
            verifyNoInteractions(requestManager);
        }
    }

    // Verifies that a pw_commands array with two valid commands executes both and returns true.
    @Test
    public void preHandleMessage_multipleCommandsBothValid_executesBoth() {
        Bundle bundle = systemPushBundle();
        bundle.putString(
                "pw_commands",
                "[{\"command\":\"setLogLevel\",\"value\":\"DEBUG\"},"
                        + "{\"command\":\"set_base_url\",\"value\":\"https://m.example.com\"}]");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class);
                MockedStatic<PWLog> pwLogMock = mockStatic(PWLog.class)) {
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);
            when(requestManager.updateBaseUrl("https://m.example.com")).thenReturn(true);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            verify(logLevelPref, times(1)).set("DEBUG");
            pwLogMock.verify(() -> PWLog.updateLogLevel("DEBUG"), times(1));
            verify(requestManager, times(1)).updateBaseUrl("https://m.example.com");
        }
    }

    // Verifies that array entries with missing/empty command field are skipped and the valid one runs.
    @Test
    public void preHandleMessage_multipleCommandsSkipsBlankCommandFields() {
        Bundle bundle = systemPushBundle();
        bundle.putString(
                "pw_commands",
                "[{\"value\":\"orphan\"},"
                        + "{\"command\":\"\",\"value\":\"x\"},"
                        + "{\"command\":\"setLogLevel\",\"value\":\"WARN\"}]");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class)) {
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            verify(logLevelPref, times(1)).set("WARN");
            verifyNoMoreInteractions(logLevelPref);
        }
    }

    // Verifies that an unknown command inside a non-empty array still consumes the push.
    @Test
    public void preHandleMessage_multipleCommandsUnknownCommand_returnsTrueViaLengthBranch() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_commands", "[{\"command\":\"unknownCmd\",\"value\":\"x\"}]");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            repoMock.verifyNoInteractions();
            netMock.verifyNoInteractions();
        }
    }

    // Verifies that array elements which are not JSON objects are skipped without crashing.
    @Test
    public void preHandleMessage_multipleCommandsNonObjectElement_skipsSilently() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_commands", "[\"string-not-object\",{\"command\":\"setLogLevel\",\"value\":\"INFO\"}]");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class)) {
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            verify(logLevelPref, times(1)).set("INFO");
        }
    }

    // Verifies that an empty pw_commands array returns false (nothing handled, length() == 0).
    @Test
    public void preHandleMessage_multipleCommandsEmptyArray_returnsFalse() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_commands", "[]");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            boolean result = dispatcher.preHandleMessage(bundle);

            assertFalse(result);
            repoMock.verifyNoInteractions();
            netMock.verifyNoInteractions();
        }
    }

    // Verifies that malformed pw_commands JSON returns false and does not fall back to the legacy path.
    @Test
    public void preHandleMessage_multipleCommandsMalformedJson_returnsFalseAndDoesNotFallBack() {
        Bundle bundle = systemPushBundle();
        bundle.putString("pw_commands", "not-json[");
        bundle.putString("pw_command", "setLogLevel");
        bundle.putString("value", "INFO");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class)) {
            boolean result = dispatcher.preHandleMessage(bundle);

            assertFalse(result);
            repoMock.verifyNoInteractions();
        }
    }

    // Verifies that a custom registered handler is invoked when its command name arrives.
    @Test
    public void registerHandler_customHandler_invokedOnMatchingCommand() {
        SystemCommandDispatcher.SystemCommandHandler customHandler =
                mock(SystemCommandDispatcher.SystemCommandHandler.class);
        when(customHandler.getCommandName()).thenReturn("custom");
        when(customHandler.handleCommand("custom", "v")).thenReturn(true);
        dispatcher.registerHandler(customHandler);

        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "custom");
        bundle.putString("value", "v");

        boolean result = dispatcher.preHandleMessage(bundle);

        assertTrue(result);
        verify(customHandler, times(1)).handleCommand("custom", "v");
    }

    // Verifies that registering a handler for an existing command overrides the built-in one.
    @Test
    public void registerHandler_duplicateCommandName_overridesBuiltIn() {
        SystemCommandDispatcher.SystemCommandHandler override =
                mock(SystemCommandDispatcher.SystemCommandHandler.class);
        when(override.getCommandName()).thenReturn("setLogLevel");
        when(override.handleCommand("setLogLevel", "INFO")).thenReturn(false);
        dispatcher.registerHandler(override);

        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "setLogLevel");
        bundle.putString("value", "INFO");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<PWLog> pwLogMock = mockStatic(PWLog.class)) {
            boolean result = dispatcher.preHandleMessage(bundle);

            assertFalse(result);
            verify(override, times(1)).handleCommand("setLogLevel", "INFO");
            verify(logLevelPref, never()).set(anyString());
            pwLogMock.verify(() -> PWLog.updateLogLevel(anyString()), never());
            repoMock.verifyNoInteractions();
        }
    }

    // Verifies that null handler and handler with null command name are silently ignored.
    @Test
    public void registerHandler_nullHandlerAndNullName_ignoredWithoutAffectingValidDispatch() {
        SystemCommandDispatcher.SystemCommandHandler nullNamed =
                mock(SystemCommandDispatcher.SystemCommandHandler.class);
        when(nullNamed.getCommandName()).thenReturn(null);

        dispatcher.registerHandler(null);
        dispatcher.registerHandler(nullNamed);

        Bundle bundle = systemPushBundle();
        bundle.putString("pw_command", "setLogLevel");
        bundle.putString("value", "X");

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<PWLog> pwLogMock = mockStatic(PWLog.class)) {
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            boolean result = dispatcher.preHandleMessage(bundle);

            assertTrue(result);
            verify(logLevelPref, times(1)).set("X");
            verify(nullNamed, never()).handleCommand(anyString(), anyString());
        }
    }
}
