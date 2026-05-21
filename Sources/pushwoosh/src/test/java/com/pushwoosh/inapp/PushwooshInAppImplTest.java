/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.ReloadInAppsException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.richmedia.RichMediaController;
import com.pushwoosh.test.inapp.FakeJsInterface;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshInAppImplTest {
    private PlatformTestManager platformTestManager;

    private PushwooshInAppImpl pushwooshInApp;
    private InAppRepository inAppRepositoryMock;

    @Mock
    PushwooshInAppService pushwooshInAppService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        inAppRepositoryMock = platformTestManager.getInAppRepositoryMock();
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        pushwooshInApp = new PushwooshInAppImpl(pushwooshInAppService, serverCommunicationManager);
    }

    @After
    public void tearDown() throws Exception {
        EventBus.clearSubscribersMap();
        SdkStateProvider.getInstance().resetForTesting();
        platformTestManager.tearDown();
    }

    private static MockedStatic<BackgroundExecutor> stubBackgroundExecutorSynchronous() {
        MockedStatic<BackgroundExecutor> bg = Mockito.mockStatic(BackgroundExecutor.class);
        bg.when(() -> BackgroundExecutor.executeOnPool(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        bg.when(() -> BackgroundExecutor.main(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        return bg;
    }

    private PushwooshInAppImpl createInApp(ServerCommunicationManager scm) {
        return new PushwooshInAppImpl(pushwooshInAppService, scm);
    }

    private void stubCommunicationEnable(boolean enabled) {
        PreferenceBooleanValue pref = mock(PreferenceBooleanValue.class);
        when(pref.get()).thenReturn(enabled);
        RegistrationPrefs prefsMock = mock(RegistrationPrefs.class);
        when(prefsMock.communicationEnable()).thenReturn(pref);
        WhiteboxHelper.setInternalState(pushwooshInApp, "registrationPrefs", prefsMock);
    }

    private static void verifyCallbackInvokedOnce(Callback<?, ?> callback) {
        verify(callback, times(1)).process(any());
    }

    @SuppressWarnings("unchecked")
    private void answerPostEventWith(Result<Resource, PostEventException> result) {
        doAnswer(invocation -> {
                    Callback<Resource, PostEventException> cb = invocation.getArgument(2);
                    if (cb != null) {
                        cb.process(result);
                    }
                    return null;
                })
                .when(inAppRepositoryMock)
                .postEvent(any(String.class), any(), any());
    }

    @Test
    public void checkForUpdates_serverCommunicationForbidden_defersUntilEvent() {
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        when(scm.isServerCommunicationAllowed()).thenReturn(false);
        PushwooshInAppImpl inApp = createInApp(scm);

        inApp.checkForUpdates();

        verify(pushwooshInAppService, never()).startService();

        when(scm.isServerCommunicationAllowed()).thenReturn(true);
        EventBus.sendEvent(new ServerCommunicationStartedEvent());

        ShadowLooper.idleMainLooper();

        verify(pushwooshInAppService, times(1)).startService();
    }

    @Test
    public void checkForUpdates_calledTwiceWhileForbidden_doesNotDoubleSubscribe() {
        ServerCommunicationManager scm = mock(ServerCommunicationManager.class);
        when(scm.isServerCommunicationAllowed()).thenReturn(false);
        PushwooshInAppImpl inApp = createInApp(scm);

        inApp.checkForUpdates();
        inApp.checkForUpdates();

        when(scm.isServerCommunicationAllowed()).thenReturn(true);
        EventBus.sendEvent(new ServerCommunicationStartedEvent());
        ShadowLooper.idleMainLooper();

        verify(pushwooshInAppService, times(1)).startService();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void postEvent_resourceNonNullCommunicationDisabled_doesNotCrash() {
        SdkStateProvider.getInstance().setReady();
        stubCommunicationEnable(false);

        Resource resource = mock(Resource.class);
        answerPostEventWith(Result.fromData(resource));

        RichMediaController richMediaControllerMock = mock(RichMediaController.class);
        PushwooshPlatform platformMock = mock(PushwooshPlatform.class);
        when(platformMock.getRichMediaController()).thenReturn(richMediaControllerMock);

        try (MockedStatic<PushwooshPlatform> platformStatic = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platformMock);

            Callback<Void, PostEventException> callback = mock(Callback.class);
            pushwooshInApp.postEvent("CustomEvent", null, callback);

            ArgumentCaptor<Result<Void, PostEventException>> captor = ArgumentCaptor.forClass(Result.class);
            verify(callback).process(captor.capture());
            assertTrue(captor.getValue().isSuccess());
            verify(inAppRepositoryMock).postEvent(eq("CustomEvent"), isNull(), any());
            verifyNoInteractions(richMediaControllerMock);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void postEvent_failure_callbackReceivesException() {
        SdkStateProvider.getInstance().setReady();
        PostEventException error = new PostEventException("boom");
        answerPostEventWith(Result.fromException(error));

        Callback<Void, PostEventException> callback = mock(Callback.class);
        pushwooshInApp.postEvent("CustomEvent", null, callback);

        ArgumentCaptor<Result<Void, PostEventException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());
        Result<Void, PostEventException> received = captor.getValue();
        assertEquals(false, received.isSuccess());
        assertEquals(error, received.getException());
    }

    // Pins the `if (resource == null) return;` short-circuit — success without a resource must NOT
    // touch RichMediaController. Mirrors testFixDefaultUserIdSkipsWhenUserIdAlreadySet pattern.
    @Test
    @SuppressWarnings("unchecked")
    public void postEvent_successResourceNull_invokesCallbackWithoutShowingResource() {
        SdkStateProvider.getInstance().setReady();
        answerPostEventWith(Result.fromData(null));

        RichMediaController richMediaControllerMock = mock(RichMediaController.class);
        PushwooshPlatform platformMock = mock(PushwooshPlatform.class);
        when(platformMock.getRichMediaController()).thenReturn(richMediaControllerMock);

        try (MockedStatic<PushwooshPlatform> platformStatic = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platformMock);

            Callback<Void, PostEventException> callback = mock(Callback.class);
            pushwooshInApp.postEvent("CustomEvent", null, callback);

            ArgumentCaptor<Result<Void, PostEventException>> captor = ArgumentCaptor.forClass(Result.class);
            verify(callback).process(captor.capture());
            assertTrue(captor.getValue().isSuccess());
            verifyNoInteractions(richMediaControllerMock);
        }
    }

    // Positive branch of the in-app render path: comm enabled + non-null resource → showResource fires.
    // Pairs with existing postEvent_resourceNonNullCommunicationDisabled_doesNotCrash (negative).
    @Test
    @SuppressWarnings("unchecked")
    public void postEvent_successResourceNonNullCommunicationEnabled_showsResource() {
        SdkStateProvider.getInstance().setReady();
        stubCommunicationEnable(true);

        Resource resource = mock(Resource.class);
        answerPostEventWith(Result.fromData(resource));

        RichMediaController richMediaControllerMock = mock(RichMediaController.class);
        PushwooshPlatform platformMock = mock(PushwooshPlatform.class);
        when(platformMock.getRichMediaController()).thenReturn(richMediaControllerMock);

        try (MockedStatic<PushwooshPlatform> platformStatic = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformStatic.when(PushwooshPlatform::getInstance).thenReturn(platformMock);

            Callback<Void, PostEventException> callback = mock(Callback.class);
            pushwooshInApp.postEvent("CustomEvent", null, callback);

            ArgumentCaptor<Result<Void, PostEventException>> captor = ArgumentCaptor.forClass(Result.class);
            verify(callback).process(captor.capture());
            assertTrue(captor.getValue().isSuccess());
            verify(richMediaControllerMock).showResourceWrapper(any());
        }
    }

    @Test
    public void registerJavascriptInterface_instantiatesClassViaReflection() {
        pushwooshInApp.registerJavascriptInterface("com.pushwoosh.test.inapp.FakeJsInterface", "jsi");

        Map<String, Object> result = pushwooshInApp.getJavascriptInterfaces();
        Object instance = result.get("jsi");
        assertNotNull(instance);
        assertTrue(
                "expected FakeJsInterface but got " + instance.getClass().getName(),
                instance instanceof FakeJsInterface);
    }

    @Test
    public void getJavascriptInterfaces_registeredOverridesAddedOnNameCollision() {
        Object sentinel = new Object();
        pushwooshInApp.addJavascriptInterface(sentinel, "x");
        pushwooshInApp.registerJavascriptInterface("com.pushwoosh.test.inapp.FakeJsInterface", "x");

        Map<String, Object> result = pushwooshInApp.getJavascriptInterfaces();
        Object value = result.get("x");
        assertTrue(
                "expected FakeJsInterface but got "
                        + (value == null ? "null" : value.getClass().getName()),
                value instanceof FakeJsInterface);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void reloadInApps_failure_callbackReceivesReloadException() {
        when(inAppRepositoryMock.loadInApps()).thenReturn(Result.fromException(new NetworkException("network down")));

        Callback<Boolean, ReloadInAppsException> callback = mock(Callback.class);
        try (MockedStatic<BackgroundExecutor> bg = stubBackgroundExecutorSynchronous()) {
            pushwooshInApp.reloadInApps(callback);
        }

        verifyCallbackInvokedOnce(callback);
        ArgumentCaptor<Result<Boolean, ReloadInAppsException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());
        Result<Boolean, ReloadInAppsException> received = captor.getValue();
        assertEquals(false, received.isSuccess());
        assertNotNull(received.getException());
        assertEquals("network down", received.getException().getMessage());
    }

    // Pairs the failure tests with a success-side assertion: callback receives Result.success(true).
    // Without it a refactor that flips Result.fromData(true) to .fromData(false) or .fromException(...)
    // passes both failure-path tests silently.
    @Test
    @SuppressWarnings("unchecked")
    public void reloadInApps_success_callbackReceivesTrue() {
        when(inAppRepositoryMock.loadInApps()).thenReturn(Result.fromData(null));

        Callback<Boolean, ReloadInAppsException> callback = mock(Callback.class);
        try (MockedStatic<BackgroundExecutor> bg = stubBackgroundExecutorSynchronous()) {
            pushwooshInApp.reloadInApps(callback);
        }

        verifyCallbackInvokedOnce(callback);
        ArgumentCaptor<Result<Boolean, ReloadInAppsException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());
        Result<Boolean, ReloadInAppsException> received = captor.getValue();
        assertTrue(received.isSuccess());
        assertEquals(Boolean.TRUE, received.getData());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void reloadInApps_failureNullMessage_callbackReceivesNullMessage() {
        when(inAppRepositoryMock.loadInApps()).thenReturn(Result.fromException(new NetworkException(null)));

        Callback<Boolean, ReloadInAppsException> callback = mock(Callback.class);
        try (MockedStatic<BackgroundExecutor> bg = stubBackgroundExecutorSynchronous()) {
            pushwooshInApp.reloadInApps(callback);
        }

        verifyCallbackInvokedOnce(callback);
        ArgumentCaptor<Result<Boolean, ReloadInAppsException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());
        Result<Boolean, ReloadInAppsException> received = captor.getValue();
        assertEquals(false, received.isSuccess());
        assertNotNull(received.getException());
        assertNull(received.getException().getMessage());
    }

    private PreferenceStringValue stubUserIdPref(String currentValue) {
        PreferenceStringValue userIdPref = mock(PreferenceStringValue.class);
        when(userIdPref.get()).thenReturn(currentValue);
        RegistrationPrefs prefsMock = mock(RegistrationPrefs.class);
        when(prefsMock.userId()).thenReturn(userIdPref);
        WhiteboxHelper.setInternalState(pushwooshInApp, "registrationPrefs", prefsMock);
        return userIdPref;
    }

    @Test
    public void setUserId_equalToCurrent_shortCircuits() {
        PreferenceStringValue userIdPref = stubUserIdPref("existing");

        pushwooshInApp.setUserId("existing");

        verify(userIdPref, never()).set(any());
        verify(inAppRepositoryMock, never()).setUserId(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setUserId_newId_writesPrefsAndDispatchesRepositoryCall() {
        PreferenceStringValue userIdPref = stubUserIdPref("old");

        pushwooshInApp.setUserId("new");

        verify(userIdPref, times(1)).set("new");
        ArgumentCaptor<Callback<Boolean, SetUserIdException>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(inAppRepositoryMock, times(1)).setUserId(eq("new"), callbackCaptor.capture());
        assertNotNull(callbackCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setUserId_repositoryReportsFailure_rollsBackPrefs() {
        PreferenceStringValue userIdPref = stubUserIdPref("old");

        pushwooshInApp.setUserId("new");

        ArgumentCaptor<Callback<Boolean, SetUserIdException>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(inAppRepositoryMock).setUserId(eq("new"), callbackCaptor.capture());
        Callback<Boolean, SetUserIdException> repositoryCallback = callbackCaptor.getValue();

        repositoryCallback.process(Result.fromException(new SetUserIdException("server-rejected")));

        InOrder inOrder = Mockito.inOrder(userIdPref);
        inOrder.verify(userIdPref).set("new");
        inOrder.verify(userIdPref).set("old");
    }
}
