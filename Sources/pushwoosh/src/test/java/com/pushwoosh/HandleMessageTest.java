/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh;

import java.util.List;

import android.app.Notification;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryTestManager;
import com.pushwoosh.testutil.Expectation;
import com.pushwoosh.testutil.NotificationFactoryMock;
import com.pushwoosh.testutil.NotificationServiceMock;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.RequestManagerMock;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;


import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Created by etkachenko on 4/20/17.
 */

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class HandleMessageTest {
	private static final String HASH = "test_hash";

	private PlatformTestManager platformTestManager;

	private RequestManagerMock requestManagerMock;
	private NotificationPrefs notificationPrefs;
	private NotificationServiceExtension notificationService;
	private RegistrationPrefs registrationPrefs;
	private PushwooshRepository pushwooshRepository;


	@Before
	public void setUp() throws Exception {
		Config configMock = MockConfig.createMock();

		platformTestManager = new PlatformTestManager(configMock);
		platformTestManager.onApplicationCreated();

		requestManagerMock = platformTestManager.getRequestManager();
		notificationPrefs = platformTestManager.getNotificationPrefs();
		notificationService = platformTestManager.getNotificationService();
		registrationPrefs = platformTestManager.getRegistrationPrefs();
		pushwooshRepository = platformTestManager.getPushwooshRepository();
	}

	@After
	public void tearDown() throws Exception {
		platformTestManager.tearDown();
	}

	//Tests preHandleMessage method sets logLevel
	@Test
	public void setLogLevelTest() throws Exception {
		String testLogLevel = "ERROR";

		//Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_system_push", "1");
		testBundle.putString("pw_command", "setLogLevel");
		testBundle.putString("value", testLogLevel);

		notificationService.handleMessage(testBundle);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		//Postconditions:
		assertThat(registrationPrefs.logLevel().get(), is(equalTo(testLogLevel)));
		verify(NotificationServiceMock.callbackMock(), timeout(100).times(0)).onMessageReceived(any());
		verify(NotificationFactoryMock.callbackMock(), timeout(100).times(0)).onGenerateNotification(any());
	}

	//Tests handleMessage with notificationDisabled
	@Test
	public void notificationDisabledTest() throws Exception {
		notificationPrefs.notificationEnabled().set(false);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		verify(NotificationServiceMock.callbackMock(), timeout(100)).onMessageReceived(any());
		verify(NotificationFactoryMock.callbackMock(), timeout(100).times(0)).onGenerateNotification(any());
	}

	@Test
	public void silentPushTest() throws Exception {
		ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("pw_silent", "1");
		notificationService.handleMessage(testBundle);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		verify(NotificationServiceMock.callbackMock(), timeout(100)).onMessageReceived(messageCaptor.capture());
		PushMessage message = messageCaptor.getValue();
		assertThat(message.isSilent(), is(true));
		verify(NotificationFactoryMock.callbackMock(), timeout(100).times(0)).onGenerateNotification(any());
	}

	//Tests handleMessage method sends messageDeliveryRequest
	@Test
	public void sendDeliveryRequestWithHashTest() throws Exception {
		Expectation<JSONObject> expectation = requestManagerMock.expect(RepositoryTestManager.getMessageDeliveryClass());
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("p", HASH);

		PushwooshMessagingServiceHelper.onMessageReceived(RuntimeEnvironment.application, testBundle);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();


		// Postconditions:
		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
		assertThat(params.getString("hash"), is(equalTo(HASH)));
	}

	//Tests handleMessage method sends messageDeliveryRequest
	@Test
	public void sendDeliveryRequestWithoutHashTest() throws Exception {
		Expectation<JSONObject> expectation = requestManagerMock.expect(RepositoryTestManager.getMessageDeliveryClass());
		ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");

		PushwooshMessagingServiceHelper.onMessageReceived(RuntimeEnvironment.application, testBundle);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();


		// Postconditions:
		verify(expectation, timeout(100)).fulfilled(requestCaptor.capture());
		JSONObject params = requestCaptor.getValue();

		assertThat(params.getString("application"), is(equalTo(APP_ID)));
	}

	//Tests there is no messageDeliveredRequest for local message
	@Test
	public void localMessageTest() throws Exception {
		ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);
		Expectation<JSONObject> expectation = requestManagerMock.expect(RepositoryTestManager.getMessageDeliveryClass());

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putBoolean("local", true);

		notificationService.handleMessage(testBundle);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();


		// Postconditions:
		verify(NotificationServiceMock.callbackMock(), timeout(100)).onMessageReceived(messageCaptor.capture());
		PushMessage message = messageCaptor.getValue();
		assertThat(message.isLocal(), is(true));

		verify(expectation, timeout(100).times(0)).fulfilled(any());
	}

	//
	//void setSoundNotificationType(SoundType soundNotificationType)
	//

	//Tests setSoundType ALWAYS
	@Test
	public void setAlwaysSoundTypeTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		notificationPrefs.soundType().set(SoundType.ALWAYS);
		platformTestManager.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_SILENT);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.sound, is(notNullValue()));
	}

	//Tests setSoundType NO_SOUND
	@Test
	public void setNoSoundSoundTypeTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		notificationPrefs.soundType().set(SoundType.NO_SOUND);
		platformTestManager.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_NORMAL);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.sound, is(nullValue()));
	}

	//Tests setSoundType DEFAULT_MODE
	@Test
	public void setDefaultSoundTypeTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		notificationPrefs.soundType().set(SoundType.DEFAULT_MODE);
		platformTestManager.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_NORMAL);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.sound, is(notNullValue()));
	}

	//Tests setSoundType DEFAULT_MODE in silentMode
	@Test
	public void setDefaultSoundTypeInSilentModeTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		notificationPrefs.soundType().set(SoundType.DEFAULT_MODE);
		platformTestManager.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_SILENT);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.sound, is(nullValue()));
	}

	//Tests setSoundType DEFAULT_MODE in vibrateMode
	@Test
	public void setDefaultSoundTypeInVibrateModeTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		notificationPrefs.soundType().set(SoundType.DEFAULT_MODE);
		platformTestManager.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.sound, is(nullValue()));
	}

	//
	//iconBackgroundColor settings part
	//

	//Tests setBgColor
	@Test
	@org.robolectric.annotation.Config(sdk = 23)
	public void setBgColorTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("ibc", "#FF00FF");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.color, is(Color.MAGENTA));
	}

	//
	//LED settings part
	//

	//Tests setLedColor, MAGENTA == #F0F
	@Test
	public void setLedColorTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		notificationPrefs.ledColor().set(Color.RED);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("led", "#F0F");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.ledARGB, is(Color.MAGENTA));
	}

	//Tests setLedColor uses defaultValue
	@Test
	public void defaultLedColorTest() throws Exception {
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		notificationPrefs.ledEnabled().set(true);
		notificationPrefs.ledColor().set(Color.RED);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);

		// Postcondition:
		verify(NotificationFactoryMock.callbackMock(), timeout(100)).onNotifactionGenerated(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertThat(notification, is(notNullValue()));
		assertThat(notification.ledARGB, is(Color.RED));
	}

	//
	//PushHistory part
	//

	//Tests getPushHistory returns handledMassage
	@Test
	public void testGetPushHistory() throws Exception {

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		List<PushMessage> history = pushwooshRepository.getPushHistory();
		assertThat(history.size(), is(equalTo(1)));
		JSONAssert.assertEquals(new JSONObject("{ \"pw_msg\" : \"1\"}"), history.get(0).toJson(), true);
	}

	//Tests clearPushHistory
	@Test
	public void testClearPushHistory() throws Exception {
		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleMessage(testBundle);
		notificationPrefs.pushHistory().clear();
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		List<PushMessage> history = pushwooshRepository.getPushHistory();
		assertThat(history.size(), is(equalTo(0)));
	}

	//Tests getPushHistory with silentPush and servicePush
	@Test
	public void testSilentPushHistory() throws Exception {
		// Steps:
		Bundle testSilentBundle = new Bundle();
		testSilentBundle.putString("pw_msg", "1");
		testSilentBundle.putString("pw_silent", "1");
		notificationService.handleMessage(testSilentBundle);


		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "2");
		notificationService.handleMessage(testBundle);

		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		// Postcondition:
		List<PushMessage> history = pushwooshRepository.getPushHistory();
		assertThat(history.size(), is(equalTo(0)));
	}
}