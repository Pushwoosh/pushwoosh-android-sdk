package com.pushwoosh;


import android.os.Bundle;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryTestManager;
import com.pushwoosh.testutil.Expectation;
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
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.skyscreamer.jsonassert.JSONAssert;


import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Created by etkachenko on 4/11/17.
 */

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class HandleNotificationTest {
	private static final String HASH = "test_hash";

	private PlatformTestManager platformTestManager;

	private RequestManagerMock requestManagerMock;
	private NotificationPrefs notificationPrefs;
	private NotificationServiceExtension notificationService;
	private RegistrationPrefs registrationPrefs;
	private PushwooshRepository pushwooshRepository;
	private PushwooshNotificationManager notificationManager;


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
		notificationManager = platformTestManager.getNotificationManager();
	}

	@After
	public void tearDown() throws Exception {
		platformTestManager.tearDown();
	}

	//
	// handleNotification() part
	//-----------------------------------------------------------------------

	//Tests method handles notification with hash correctly (smoke test)
	@Test
	public void sendStatWithHashTest() throws Exception {
		ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("p", HASH);
		notificationService.handleNotification(testBundle);

		// Postcondition: Verify notification is handled and activity started
		verify(NotificationServiceMock.callbackMock(), timeout(1000)).startActivityForPushMessage(messageCaptor.capture());
		PushMessage message = messageCaptor.getValue();
		assertThat(message.getPushHash(), is(equalTo(HASH)));
	}

	//Tests method handles notification without hash correctly (smoke test)
	@Test
	public void sendStatWithoutHashTest() throws Exception {
		ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		notificationService.handleNotification(testBundle);

		// Postcondition: Verify notification is handled and activity started
		verify(NotificationServiceMock.callbackMock(), timeout(100)).startActivityForPushMessage(messageCaptor.capture());
		PushMessage message = messageCaptor.getValue();
		assertThat(message.getPushHash(), is(nullValue()));
	}

	//Tests method sends no pushStatRequest for local push
	@Test
	public void handleLocalNotificationTest() throws Exception {
		Expectation<JSONObject> expectation = requestManagerMock.expect(RepositoryTestManager.getPushStatClass());
		ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putBoolean("local", true);
		notificationService.handleNotification(testBundle);

		// Postcondition:
		verify(expectation, timeout(100).times(0)).fulfilled(any());
		verify(NotificationServiceMock.callbackMock(), timeout(100)).startActivityForPushMessage(messageCaptor.capture());
		PushMessage message = messageCaptor.getValue();
		assertThat(message.isLocal(), is(true));
	}

	//
	// customData part
	//-----------------------------------------------------------------------

	//Tests getCustomData method
	@Test
	public void customDataTest() throws Exception {
		String testCustomData = "{\"custom\" : \"data\"}";
		ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);

		// Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("u", testCustomData);
		notificationService.handleNotification(testBundle);

		verify(NotificationServiceMock.callbackMock(), timeout(100)).startActivityForPushMessage(messageCaptor.capture());
		PushMessage message = messageCaptor.getValue();

		assertThat(message.getCustomData(), is(equalTo(testCustomData)));
	}

	//
	// getLaunchNotification() part
	//-----------------------------------------------------------------------

	//Tests getLaunchNotification method
	@Test
	public void getLaunchNotificationTest() throws Exception {
		//Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("p", HASH);
		notificationService.handleNotification(testBundle);

		PushMessage launchNotification = notificationManager.getLaunchNotification();
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
		JSONAssert.assertEquals(new JSONObject("{\"pw_msg\" : \"1\", \"p\" : \"test_hash\"}"), launchNotification.toJson(), true);
	}

	//Tests getLaunchNotification method when no value set
	@Test
	public void nullLaunchNotificationTest() throws Exception {
		// Postcondition:
		assertThat(notificationManager.getLaunchNotification(), is(nullValue()));
	}

	//
	// clearLaunchNotification() part
	//-----------------------------------------------------------------------

	//Tests clearLaunchNotification
	@Test
	public void clearLaunchNotificationTest() throws Exception {
		//Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("p", HASH);
		notificationService.handleNotification(testBundle);
		notificationManager.clearLaunchNotification();

		// Postcondition:
		assertThat(notificationManager.getLaunchNotification(), is(nullValue()));
	}

	//Tests clear cleared LaunchNotification
	@Test
	public void clearEmptyLaunchNotificationTest() throws Exception {
		//Steps:
		Bundle testBundle = new Bundle();
		testBundle.putString("pw_msg", "1");
		testBundle.putString("p", HASH);
		notificationService.handleNotification(testBundle);
		notificationManager.clearLaunchNotification();
		notificationManager.clearLaunchNotification();

		// Postcondition:
		assertThat(notificationManager.getLaunchNotification(), is(nullValue()));
	}
}
