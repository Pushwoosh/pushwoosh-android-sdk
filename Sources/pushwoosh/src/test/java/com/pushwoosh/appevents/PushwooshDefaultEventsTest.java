package com.pushwoosh.appevents;

import android.os.Looper;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.inapp.PushwooshInApp;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.config.Event;
import com.pushwoosh.tags.TagsBundle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
		AndroidPlatformModule.class,
		PushwooshDefaultEvents.class,
		PushwooshInApp.class,
		Looper.class,
		PWLog.class,
		PushwooshInApp.class,
		PushwooshAppLifecycleCallbacks.class,
		PushwooshPlatform.class,
		DeviceSpecificProvider.class,
		InAppManager.class
})
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
		mockStatic(Looper.class);
		mockStatic(PWLog.class);
		mockStatic(PushwooshInApp.class);
		mockStatic(PushwooshAppLifecycleCallbacks.class);
		mockStatic(PushwooshPlatform.class);
		mockStatic(DeviceSpecificProvider.class);
		mockStatic(AndroidPlatformModule.class);
		mockStatic(InAppManager.class);

		when(PushwooshInApp.getInstance()).thenReturn(mock(PushwooshInApp.class));
		when(DeviceSpecificProvider.getInstance()).thenReturn(deviceSpecificProvider);
		when(AndroidPlatformModule.getAppInfoProvider()).thenReturn(appInfoProvider);
		when(PushwooshPlatform.getInstance()).thenReturn(pushwooshPlatform);
		when(pushwooshPlatform.pushwooshRepository()).thenReturn(pushwooshRepository);
		when(InAppManager.getInstance()).thenReturn(mock(InAppManager.class));

		List<Event> events = new ArrayList<>();
		events.add(new Event("PW_ApplicationOpen"));
		events.add(new Event("PW_ScreenOpen"));
		when(pushwooshRepository.getEvents()).thenReturn(events);
		when(deviceSpecificProvider.deviceType()).thenReturn(10);
		when(appInfoProvider.getVersionName()).thenReturn("v1.0.0");
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void checkPostEvent() {
		PushwooshDefaultEvents pushwooshDefaultEvents = new PushwooshDefaultEvents();
		Whitebox.setInternalState(pushwooshDefaultEvents, "isConfigLoaded", true);

		TagsBundle attributes = PushwooshDefaultEvents.buildAttributes(PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, "activityName");

		pushwooshDefaultEvents.postEvent(PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, attributes);
 		verify(InAppManager.getInstance(), times(1)).postEvent(PushwooshDefaultEvents.APPLICATION_OPENED_EVENT, attributes);

		pushwooshDefaultEvents.postEvent(PushwooshDefaultEvents.SCREEN_OPENED_EVENT, attributes);
		verify(InAppManager.getInstance(), times(1)).postEvent(PushwooshDefaultEvents.SCREEN_OPENED_EVENT, attributes);

		pushwooshDefaultEvents.postEvent(PushwooshDefaultEvents.APPLICATION_CLOSED_EVENT, attributes);
		verify(InAppManager.getInstance(), times(0)).postEvent(PushwooshDefaultEvents.APPLICATION_CLOSED_EVENT, attributes);
	}

	@Test
	public void checkBuildAttributes() {
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
