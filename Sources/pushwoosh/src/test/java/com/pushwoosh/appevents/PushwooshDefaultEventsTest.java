package com.pushwoosh.appevents;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.inapp.PushwooshInApp;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.config.Event;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushwooshDefaultEventsTest {

	@Mock
	private static PushwooshPlatform pushwooshPlatform = mock(PushwooshPlatform.class);

	@Mock
	private static PushwooshRepository pushwooshRepository = mock(PushwooshRepository.class);

	@Mock
	private static AppInfoProvider appInfoProvider = mock(AppInfoProvider.class);

	@Mock
	private static DeviceSpecificProvider deviceSpecificProvider = mock(DeviceSpecificProvider.class);

	@Before
	public void setUp() throws Exception {
		List<Event> events = new ArrayList<>();
		events.add(new Event("PW_ApplicationOpen"));
		events.add(new Event("PW_ScreenOpen"));
		when(deviceSpecificProvider.deviceType()).thenReturn(10);
		when(appInfoProvider.getVersionName()).thenReturn("v1.0.0");
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void checkPostEvent() {
		try (
				MockedStatic<PushwooshInApp> pushwooshInAppMockedStatic = mockStatic(PushwooshInApp.class);
				MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = mockStatic(PushwooshPlatform.class);
				MockedStatic<DeviceSpecificProvider> deviceSpecificProviderMockedStatic = mockStatic(DeviceSpecificProvider.class);
				MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = mockStatic(AndroidPlatformModule.class);
				MockedStatic<InAppManager> inAppManagerMockedStatic = mockStatic(InAppManager.class)
		) {
			pushwooshInAppMockedStatic.when(PushwooshInApp::getInstance).thenReturn(mock(PushwooshInApp.class));
			deviceSpecificProviderMockedStatic.when(DeviceSpecificProvider::getInstance).thenReturn(deviceSpecificProvider);
			platformModuleMockedStatic.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
			pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
			when(pushwooshPlatform.pushwooshRepository()).thenReturn(pushwooshRepository);
			inAppManagerMockedStatic.when(InAppManager::getInstance).thenReturn(mock(InAppManager.class));

			PushwooshDefaultEvents pushwooshDefaultEvents = new PushwooshDefaultEvents();
			WhiteboxHelper.setInternalState(pushwooshDefaultEvents, "isConfigLoaded", true);

			TagsBundle attributes = PushwooshDefaultEvents.buildAttributes(PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, "activityName");

			pushwooshDefaultEvents.postEvent(PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, attributes);
			verify(InAppManager.getInstance(), times(1)).postEvent(PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, attributes, true);

			pushwooshDefaultEvents.postEvent(PushwooshDefaultEvents.SCREEN_OPENED_EVENT, attributes);
			verify(InAppManager.getInstance(), times(1)).postEvent(PushwooshDefaultEvents.SCREEN_OPENED_EVENT, attributes, true);

			pushwooshDefaultEvents.postEvent(PushwooshDefaultEvents.APPLICATION_CLOSED_EVENT, attributes);
			verify(InAppManager.getInstance(), times(1)).postEvent(PushwooshDefaultEvents.APPLICATION_CLOSED_EVENT, attributes, true);
		}
	}

	@Test
	public void checkBuildAttributes() {
		try (
				MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = mockStatic(PushwooshPlatform.class);
				MockedStatic<DeviceSpecificProvider> deviceSpecificProviderMockedStatic = mockStatic(DeviceSpecificProvider.class);
				MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = mockStatic(AndroidPlatformModule.class)
		) {
			deviceSpecificProviderMockedStatic.when(DeviceSpecificProvider::getInstance).thenReturn(deviceSpecificProvider);
			platformModuleMockedStatic.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
			pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);
			when(pushwooshPlatform.pushwooshRepository()).thenReturn(pushwooshRepository);

			TagsBundle appOpenEventAttributes = PushwooshDefaultEvents.buildAttributes(PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, "activityName");

			Assert.assertNotNull(appOpenEventAttributes);
			Assert.assertEquals(appOpenEventAttributes.getInt("device_type", -1), 10);
			Assert.assertEquals(appOpenEventAttributes.getString("application_version"), "v1.0.0");
			Assert.assertNull(appOpenEventAttributes.getString("screen_name"));

			TagsBundle screenOpenAttributes = PushwooshDefaultEvents.buildAttributes(PushwooshDefaultEvents.SCREEN_OPENED_EVENT, "activityName");

			Assert.assertNotNull(screenOpenAttributes);
			Assert.assertEquals(screenOpenAttributes.getInt("device_type", -1), 10);
			Assert.assertEquals(screenOpenAttributes.getString("application_version"), "v1.0.0");
			Assert.assertEquals(screenOpenAttributes.getString("screen_name"), "activityName");
		}
	}
}
