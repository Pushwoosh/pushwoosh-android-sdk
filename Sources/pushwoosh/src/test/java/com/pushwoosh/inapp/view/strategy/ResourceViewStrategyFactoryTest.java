package com.pushwoosh.inapp.view.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.RichMediaWebActivity;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ResourceViewStrategyFactoryTest {

	private ResourceViewStrategyFactory factory;

	@Mock
	private Context mockContext;
	@Mock
	private PushwooshPlatform mockPlatform;
	@Mock
	private PushwooshRepository mockRepository;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		factory = new ResourceViewStrategyFactory();
		when(mockPlatform.pushwooshRepository()).thenReturn(mockRepository);
	}

	// --- Null guard tests ---

	@Test
	public void showResource_nullWrapper_doesNothing() {
		factory.showResource(null);
	}

	@Test
	public void showResource_nullResource_doesNothing() {
		ResourceWrapper wrapper = new ResourceWrapper.Builder().build();
		factory.showResource(wrapper);
	}

	@Test
	public void showResource_nullContext_doesNothing() {
		Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(null);

			factory.showResource(wrapper);
		}
	}

	// --- InApp + MODAL ---

	@Test
	public void showResource_inAppModal_showsModalWindow() {
		Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
			 MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.MODAL);

			factory.showResource(wrapper);

			modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(resource));
			verify(mockRepository).setCurrentInAppCode("code1");
			verify(mockRepository).setCurrentRichMediaCode(null);
		}
	}

	// --- InApp + DEFAULT ---

	@Test
	public void showResource_inAppDefault_startsActivityWithInAppIntent() {
		Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();

		Intent mockIntent = mock(Intent.class);

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
			 MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.DEFAULT);
			webActivity.when(() -> RichMediaWebActivity.createInAppIntent(mockContext, resource)).thenReturn(mockIntent);

			factory.showResource(wrapper);

			webActivity.verify(() -> RichMediaWebActivity.createInAppIntent(mockContext, resource));
			verify(mockRepository).setCurrentInAppCode("code1");
			verify(mockRepository).setCurrentRichMediaCode(null);

			// Execute Handler.post() callback
			ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
			verify(mockContext).startActivity(mockIntent);
		}
	}

	// --- RichMedia + MODAL ---

	@Test
	public void showResource_richMediaModal_showsModalWindow() {
		Resource resource = new Resource("r-rm123", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
			 MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.MODAL);

			factory.showResource(wrapper);

			modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(resource));
			verify(mockRepository).setCurrentRichMediaCode("rm123");
			verify(mockRepository).setCurrentInAppCode(null);
		}
	}

	// --- RichMedia + DEFAULT ---

	@Test
	public void showResource_richMediaDefault_startsActivityWithRichMediaIntent() {
		Resource resource = new Resource("r-rm456", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();

		Intent mockIntent = mock(Intent.class);

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
			 MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.DEFAULT);
			webActivity.when(() -> RichMediaWebActivity.createRichMediaIntent(mockContext, resource)).thenReturn(mockIntent);

			factory.showResource(wrapper);

			webActivity.verify(() -> RichMediaWebActivity.createRichMediaIntent(mockContext, resource));
			verify(mockRepository).setCurrentRichMediaCode("rm456");
			verify(mockRepository).setCurrentInAppCode(null);

			// Execute Handler.postDelayed() callback
			ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
			verify(mockContext).startActivity(mockIntent);
		}
	}

	// --- RichMedia + LockScreen ---

	@Test
	public void showResource_richMediaLockScreen_startsActivityWithLockScreenIntent() {
		Resource resource = new Resource("r-rm789", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.setLockScreen(true)
				.setSound("notification_sound")
				.build();

		Intent mockIntent = mock(Intent.class);

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			webActivity.when(() -> RichMediaWebActivity.createRichMediaLockScreenIntent(mockContext, resource, "notification_sound"))
					.thenReturn(mockIntent);

			factory.showResource(wrapper);

			webActivity.verify(() -> RichMediaWebActivity.createRichMediaLockScreenIntent(mockContext, resource, "notification_sound"));
			verify(mockContext).startActivity(mockIntent);
		}
	}

	// --- Repository code assignment ---

	@Test
	public void showResource_richMedia_setsCodeWithSubstring2() {
		Resource resource = new Resource("r-mycode", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
			 MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.MODAL);

			factory.showResource(wrapper);

			// "r-mycode".substring(2) == "mycode"
			verify(mockRepository).setCurrentRichMediaCode("mycode");
			verify(mockRepository).setCurrentInAppCode(null);
		}
	}

	// --- Lock screen does not call repository codes ---

	@Test
	public void showResource_lockScreen_doesNotSetRepositoryCodes() {
		Resource resource = new Resource("r-rm000", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.setLockScreen(true)
				.setSound("")
				.build();

		Intent mockIntent = mock(Intent.class);

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			webActivity.when(() -> RichMediaWebActivity.createRichMediaLockScreenIntent(any(), any(), any()))
					.thenReturn(mockIntent);

			factory.showResource(wrapper);

			verify(mockRepository, never()).setCurrentInAppCode(any());
			verify(mockRepository, never()).setCurrentRichMediaCode(any());
		}
	}

	// --- InApp routing based on ResourceType ---

	@Test
	public void showResource_inAppType_neverCallsRichMediaIntent() {
		Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.build();

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
			 MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
			 MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.MODAL);

			factory.showResource(wrapper);

			modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(resource));
			webActivity.verify(() -> RichMediaWebActivity.createRichMediaIntent(any(), any()), never());
			webActivity.verify(() -> RichMediaWebActivity.createRichMediaLockScreenIntent(any(), any(), any()), never());
		}
	}

	// --- RichMedia with delay uses postDelayed ---

	@Test
	public void showResource_richMediaWithDelay_activityNotStartedBeforeDelay() {
		Resource resource = new Resource("r-rm222", "http://example.com", "", 0, null, null, false, 0);
		ResourceWrapper wrapper = new ResourceWrapper.Builder()
				.setResource(resource)
				.setDelay(5000)
				.build();

		Intent mockIntent = mock(Intent.class);

		try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
			 MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
			 MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
			 MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {

			platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
			pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
			richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.DEFAULT);
			webActivity.when(() -> RichMediaWebActivity.createRichMediaIntent(mockContext, resource)).thenReturn(mockIntent);

			factory.showResource(wrapper);

			// Before running delayed tasks, startActivity should not have been called
			verify(mockContext, never()).startActivity(any());

			// After running delayed tasks, it should be called
			ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
			verify(mockContext).startActivity(mockIntent);
		}
	}
}
