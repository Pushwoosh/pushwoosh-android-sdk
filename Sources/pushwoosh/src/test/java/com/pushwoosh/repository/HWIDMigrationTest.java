package com.pushwoosh.repository;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceLongValue;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class HWIDMigrationTest {

    public static final String TEST_EXCEPTION = "test exception";
    public static final PushwooshException EXCEPTION = new PushwooshException(TEST_EXCEPTION);
    public static final Result<Void, PushwooshException> RESULT_WITH_EXCEPTION = Result.fromException(EXCEPTION);
    public static final String HWID = "hwid";
    public static final String ANOTHER_HWID = "hwid2";
    public static final String TEST_TOKEN = "test token";
    private com.pushwoosh.repository.HWIDMigration HWIDMigration;

    @Mock
    private RequestManager requestManager;
    @Mock
    private SendTagsProcessor sendTagsProcessor;
    @Mock
    private AppVersionProvider appVersionProvider;
    @Mock
    private PreferenceBooleanValue isTagMigrationDone;
    @Mock
    private PushwooshNotificationManager notificationManager;
    @Mock
    private RegistrationPrefs registrationPrefs;
    @Mock
    private DeviceRegistrar deviceRegistrar;
    @Mock
    private PreferenceLongValue lastPushRegistration;
    @Mock
    private PreferenceBooleanValue isRegisteredForPush;

    @Captor
    ArgumentCaptor<Callback<TagsBundle, NetworkException>> callbackGatTagArgumentCaptor;
    @Captor
    ArgumentCaptor<PushRequest<TagsBundle>> responseArgumentCaptor;

    @Captor
    ArgumentCaptor<JSONObject> jsonExceptionArgumentCaptor;
    @Captor
    ArgumentCaptor<Callback<Void, PushwooshException>> callbackSetTagArgumentCaptor;

    @Captor
    ArgumentCaptor<JSONObject> tagsJsonArgumentCaptor;
    @Captor
    ArgumentCaptor<Callback<TagsBundle, NetworkException>> callbackArgumentCaptor;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        MockitoAnnotations.initMocks(this);
        HWIDMigration = new HWIDMigration(
                requestManager,
                sendTagsProcessor,
                isTagMigrationDone,
                appVersionProvider,
                notificationManager,
                registrationPrefs,
                deviceRegistrar);

        when(appVersionProvider.isFirstLaunch()).thenReturn(false);


        when(registrationPrefs.lastPushRegistration()).thenReturn(lastPushRegistration);
        when(registrationPrefs.isRegisteredForPush()).thenReturn(isRegisteredForPush);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void shouldGetOldTagFromServer() {
        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, HWID);

        verify(requestManager).sendRequest(responseArgumentCaptor.capture(), callbackGatTagArgumentCaptor.capture());

        PushRequest<TagsBundle> tagsBundlePushRequest = responseArgumentCaptor.getValue();
        Assert.assertEquals("getTags", tagsBundlePushRequest.getMethod());
    }

    @Test
    public void shouldSendOldTagToServerWithNewHwid() throws JSONException {
        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, HWID);

        verify(requestManager).sendRequest(
                responseArgumentCaptor.capture(),
                callbackGatTagArgumentCaptor.capture());

        Callback<TagsBundle, NetworkException> callbackTags = callbackGatTagArgumentCaptor.getValue();
        TagsBundle tagsBundle = processGetTag(callbackTags);

        checkTagJsonSended(tagsBundle);
    }

    @Test
    public void shouldSaveInPrefsIfTagMigrationResultSuccess() throws JSONException {
        when(isTagMigrationDone.get()).thenReturn(false);

        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, HWID);

        checkSuccessMigration();
    }

    @Test
    public void shouldDoMigrationIfHWIDNotEquals() throws JSONException {
        when(isTagMigrationDone.get()).thenReturn(true);

        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, ANOTHER_HWID);

        checkSuccessMigration();
    }

    private void checkSuccessMigration() throws JSONException {
        verify(requestManager).sendRequest(
                responseArgumentCaptor.capture(),
                callbackGatTagArgumentCaptor.capture());

        Callback<TagsBundle, NetworkException> callbackTags = callbackGatTagArgumentCaptor.getValue();
        TagsBundle tagsBundle = processGetTag(callbackTags);

        checkTagJsonSended(tagsBundle);

        Callback<Void, PushwooshException> pushwooshExceptionCallback = callbackSetTagArgumentCaptor.getValue();

        pushwooshExceptionCallback.process(Result.fromData(null));

        verify(isTagMigrationDone).set(true);
    }

    @Test
    public void shouldNotSaveInPrefsIfTagMigrationResultFail() throws JSONException {
        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, HWID);

        verify(requestManager).sendRequest(
                responseArgumentCaptor.capture(),
                callbackGatTagArgumentCaptor.capture());

        Callback<TagsBundle, NetworkException> callbackTags = callbackGatTagArgumentCaptor.getValue();
        TagsBundle tagsBundle = processGetTag(callbackTags);

        checkTagJsonSended(tagsBundle);

        Callback<Void, PushwooshException> pushwooshExceptionCallback = callbackSetTagArgumentCaptor.getValue();

        pushwooshExceptionCallback.process(RESULT_WITH_EXCEPTION);

        verify(isTagMigrationDone, never()).set(anyBoolean());
    }

    private void checkTagJsonSended(TagsBundle tagsBundle) throws JSONException {
        verify(sendTagsProcessor).sendTags(jsonExceptionArgumentCaptor.capture(), callbackSetTagArgumentCaptor.capture());
        JSONAssert.assertEquals(tagsBundle.toJson(), jsonExceptionArgumentCaptor.getValue(), true);
    }


    private TagsBundle processGetTag(Callback<TagsBundle, NetworkException> callbackTags) {
        TagsBundle tagsBundle =
                new TagsBundle.Builder()
                        .putString("tag1", "123")
                        .putInt("tag2", 2)
                        .build();
        Result<TagsBundle, NetworkException> tags = Result.fromData(tagsBundle);
        callbackTags.process(tags);
        return tagsBundle;
    }

    @Test
    public void shouldDoNotMigrationAndSetMigrationSuccessInPrefsApplicationJustInstall() {
        when(appVersionProvider.isFirstLaunch()).thenReturn(true);
        doAnswer(invocation -> {
            when(isTagMigrationDone.get()).thenReturn(invocation.getArgumentAt(0, Boolean.class));
            return null;
        }).when(isTagMigrationDone).set(anyBoolean());

        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, HWID);

        verify(isTagMigrationDone).set(true);
        verify(requestManager, never()).sendRequest(any(), any());
        verify(sendTagsProcessor, never()).sendTags(any(), any());
    }

    @Test
    public void shouldDoNothingIfMigrationDone() {
        when(isTagMigrationDone.get()).thenReturn(true);
        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, HWID);

        verify(requestManager, never()).sendRequest(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRegisterDeviceIfDeviceHasToken() throws JSONException {
        doAnswer(invocation -> {
            EventBus.sendEvent(new RegistrationSuccessEvent(new RegisterForPushNotificationsResultData(TEST_TOKEN, true)));
            return null;
        }).when(deviceRegistrar).updateRegistration();

        when(notificationManager.getPushToken()).thenReturn(TEST_TOKEN);
        when(registrationPrefs.isRegisteredForPush()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.isRegisteredForPush().get()).thenReturn(true);
        when(registrationPrefs.lastPushRegistration()).thenReturn(mock(PreferenceLongValue.class));

        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, ANOTHER_HWID);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        checkSuccessMigration();

        verify(registrationPrefs.lastPushRegistration()).set(0);
        verify(deviceRegistrar).updateRegistration();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMarkAsMigratedIfServiceRespondedWithDeviceNotFound() {
        doAnswer(invocation -> {
            invocation.getArgumentAt(1, Callback.class)
                    .process(Result.fromException(new NetworkException("{\"status_code\":210,\"status_message\":\"Device not found\",\"response\":null}")));
            return null;
        }).when(requestManager).sendRequest(any(GetTagsRequestWithOldHWID.class), any());
        HWIDMigration.prepare();
        HWIDMigration.executeMigration(HWID, ANOTHER_HWID);
        verify(isTagMigrationDone).set(true);
    }

    @Test
    public void executeMigrationShouldContinueMigrationIfIsTagsFromOldHWIDLoadedAndIsTagMigrationDoneFalse() {
        AtomicBoolean continueMigration = new AtomicBoolean();
        HWIDMigration = new HWIDMigration(
                requestManager,
                sendTagsProcessor,
                isTagMigrationDone,
                appVersionProvider,
                notificationManager,
                registrationPrefs,
                deviceRegistrar){
            @Override
            protected void continueMigration() {
                continueMigration.set(true);
            }
        };
        isTagMigrationDone.set(false);
        Whitebox.setInternalState(HWIDMigration, "isTagsFromOldHWIDLoaded", true);




        HWIDMigration.executeMigration(HWID, ANOTHER_HWID);


        Assert.assertTrue(continueMigration.get());
    }

    @Test
    public void executeMigrationShouldContinueMigrationIfHWIDEqsualsAndIsTagMigrationDoneTrue() {
        isTagMigrationDone.set(false);
        Whitebox.setInternalState(HWIDMigration, "isTagsFromOldHWIDLoaded", true);

        HWIDMigration.executeMigration(HWID, HWID);
        // huistan we have problem
    }

    @Test
    public void executeMigrationShouldSaveTagIfLastPushRegistrationNotZeroAndHWIDEqualse() throws JSONException {
        isTagMigrationDone.set(false);
        Whitebox.setInternalState(HWIDMigration, "isTagsFromOldHWIDLoaded", true);


        when(lastPushRegistration.get()).thenReturn(100L);
        when(notificationManager.getPushToken()).thenReturn(TEST_TOKEN);
        when(registrationPrefs.isRegisteredForPush().get()).thenReturn(true);
        TagsBundle tagsBundle = new TagsBundle.Builder().putString("1", "1").putInt("2", 2).build();
        Whitebox.setInternalState(HWIDMigration, "tagsFromOldHWID", tagsBundle);

        HWIDMigration.executeMigration(HWID, HWID);

        verify(sendTagsProcessor).sendTags(tagsJsonArgumentCaptor.capture(), any());
        JSONObject value = tagsJsonArgumentCaptor.getValue();
        JSONObject expected = tagsBundle.toJson();
        JSONAssert.assertEquals(expected, value, true);
    }

    @Test
    public void prepareShouldTagsMigrationPrefsSetTrueIfOldTagsIsEmpty(){
        when(appVersionProvider.isFirstLaunch()).thenReturn(false);
        TagsBundle tagsBundle = new TagsBundle.Builder().build();
        Result<TagsBundle, NetworkException> result = Result.fromData(tagsBundle);

        HWIDMigration.prepare();
        verify(requestManager).sendRequest(any(), callbackArgumentCaptor.capture());
        Callback<TagsBundle, NetworkException> callback = callbackArgumentCaptor.getValue();
        callback.process(result);

        verify(isTagMigrationDone).set(true);
    }
}