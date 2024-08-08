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

package com.pushwoosh.testutil;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.inapp.PushwooshInAppServiceImpl;
import com.pushwoosh.inapp.mapper.ResourceMapper;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.downloader.InAppDownloader;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.AndroidPlatformModuleTest;
import com.pushwoosh.internal.prefs.TestPrefsProvider;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.specific.TestDeviceSpecific;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.Map;

import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PlatformTestManager {
	private final RequestManagerMock requestManagerMock;
	private final PushwooshPlatform pushwooshPlatform;
	private final PushRegistrar pushRegistrarMock;
	private PushwooshRepository pushwooshRepositoryMock;

	private final RegistrationPrefs registrationPrefs;

	public PlatformTestManager() {
		this(MockConfig.createMock(), true);
	}

	public PlatformTestManager(Config config) {
		this(config, true);
	}

	public PlatformTestManager(Config config, boolean needSpyContext) {
		ShadowLog.stream = System.out;

		Configuration workConfig = new Configuration.Builder()
				.setMinimumLoggingLevel(Log.DEBUG)
				.setExecutor(new SynchronousExecutor())
				.build();
		WorkManagerTestInitHelper.initializeTestWorkManager(
				RuntimeEnvironment.application, workConfig);

		registrationPrefs = Mockito.mock(RegistrationPrefs.class);
		// when(config.getNotificationFactory()).thenReturn(NotificationFactoryMock.class); does not work :(
		when(config.getNotificationFactory()).thenAnswer((invocation) -> NotificationFactoryMock.class);
		when(config.getNotificationService()).thenAnswer((invocation) -> NotificationServiceMock.class);

		pushRegistrarMock = mock(PushRegistrar.class);

		new DeviceSpecificProvider.Builder()
				.setDeviceSpecific(new TestDeviceSpecific(pushRegistrarMock))
				.build(true);


		requestManagerMock = Mockito.spy(new RequestManagerMock());

		NetworkModule.setRequestManager(requestManagerMock);
		if (needSpyContext) {
			Context spyContext = spy(RuntimeEnvironment.application);
			when(spyContext.getApplicationContext()).thenReturn(spyContext);
			AndroidPlatformModule.init(spyContext, true);
		} else {
			AndroidPlatformModule.init(RuntimeEnvironment.application, true);
		}

		AndroidPlatformModuleTest.changePrefsProvider(new TestPrefsProvider());

		final InAppStorage inAppStorage = mock(InAppStorage.class);
		InAppRepository inAppRepository = new InAppRepository(
				requestManagerMock,
				inAppStorage,
				mock(InAppDownloader.class),
				mock(ResourceMapper.class),
				mock(InAppFolderProvider.class),
				registrationPrefs);

		InAppModule.setInAppRepository(inAppRepository);
		InAppModule.setInAppStorage(inAppStorage);

		pushwooshPlatform = new PushwooshPlatform.Builder()
				.setConfig(config)
				.setPushRegistrar(pushRegistrarMock)
				.build();

		PushwooshInAppServiceImpl pushwooshInAppServiceMock = Mockito.mock(PushwooshInAppServiceImpl.class);

		PushwooshInAppImpl pushwooshInApp= (PushwooshInAppImpl)
				WhiteboxHelper.getInternalState(pushwooshPlatform, "pushwooshInApp");
		WhiteboxHelper.setInternalState(pushwooshInApp, "pushwooshInAppService", pushwooshInAppServiceMock);
	}

	public RequestManagerMock getRequestManager() {
		return requestManagerMock;
	}

	public PushwooshRepository getPushwooshRepository() {
		return pushwooshPlatform.pushwooshRepository();
	}

	public PushwooshRepository getPushwooshRepositoryMock() {
		PushwooshRepository pushwooshRepository = (PushwooshRepository)
				WhiteboxHelper.getInternalState(pushwooshPlatform, "pushwooshRepository");
		pushwooshRepositoryMock = Mockito.spy(pushwooshRepository);
		WhiteboxHelper.setInternalState(pushwooshPlatform,"pushwooshRepository", pushwooshRepositoryMock);
		return pushwooshRepositoryMock;
	}

	public RegistrationPrefs getRegistrationPrefsMock() {
		return registrationPrefs;
	}

	public PushwooshInAppImpl getPushwooshInApp() {
		return pushwooshPlatform.pushwooshInApp();
	}

	public NotificationPrefs getNotificationPrefs() {
		return RepositoryModule.getNotificationPreferences();
	}

	public RegistrationPrefs getRegistrationPrefs() {
		return RepositoryModule.getRegistrationPreferences();
	}

	public PushwooshNotificationManager getNotificationManager() {
		return pushwooshPlatform.notificationManager();
	}

	public PushRegistrar getPushRegistrar() {
		return pushRegistrarMock;
	}

	public NotificationServiceExtension getNotificationService() {
		return pushwooshPlatform.notificationService();
	}

	public AudioManager getAudioManager() {
		return (AudioManager) RuntimeEnvironment.application.getSystemService(Context.AUDIO_SERVICE);
	}

	public void setUp() {
	//	pushwooshPlatform.onApplicationCreated();
	}

	public void onApplicationCreated() {
		pushwooshPlatform.onApplicationCreated();
	}

	public InAppRepository getInAppRepository() {
		return InAppModule.getInAppRepository();
	}

	public InAppRepository getInAppRepositoryMock() {
		InAppModule.setInAppRepository(mock(InAppRepository.class));
		return InAppModule.getInAppRepository();
	}

	public InAppStorage getInAppStorage() {
		return InAppModule.getInAppStorage();
	}

	public void tearDown() {
		PrefsHelper.tearDownPrefs();
//		Map<Class<? extends Event>, List<EventListener<?>>> mapEvent = (Map<Class<? extends Event>, List<EventListener<?>>>) WhiteboxHelper.getInternalState(EventBus.class, "SUBSCRIBERS_MAP");
//		mapEvent.clear();
	}
}
