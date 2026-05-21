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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class ResourceViewStrategyFactoryTest {

    private ResourceViewStrategyFactory factory;
    private AutoCloseable mocks;

    @Mock
    private Context mockContext;

    @Mock
    private PushwooshPlatform mockPlatform;

    @Mock
    private PushwooshRepository mockRepository;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        factory = new ResourceViewStrategyFactory();
        when(mockPlatform.pushwooshRepository()).thenReturn(mockRepository);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Restored from cross-check: pins the IN_APP branch's repository contract — sets inAppCode VERBATIM
    // (no substring transform, in contrast with the rich-media branch) and nulls richMediaCode.
    // A refactor that accidentally unifies the two code-setting paths would silently break rich-media
    // tracking; this test guards the asymmetry from the IN_APP side.
    @Test
    public void showResource_inApp_setsInAppCodeWithoutTransformAndNullsRichMediaCode() {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatform = Mockito.mockStatic(PushwooshPlatform.class);
                MockedStatic<RichMediaManager> richMediaManager = Mockito.mockStatic(RichMediaManager.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class)) {

            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);
            pushwooshPlatform.when(PushwooshPlatform::getInstance).thenReturn(mockPlatform);
            richMediaManager.when(RichMediaManager::getRichMediaType).thenReturn(RichMediaType.MODAL);

            factory.showResource(wrapper);

            // IN_APP must NOT call .substring(2) — the code is the in-app code verbatim.
            verify(mockRepository).setCurrentInAppCode("code1");
            verify(mockRepository).setCurrentRichMediaCode(null);
        }
    }

    // Verifies the substring(2) transformation on rich-media code (drops "r-" prefix to derive richMediaCode).
    // Also pins the rich-media repository contract: sets richMediaCode, nulls inAppCode.
    @Test
    public void showResource_richMedia_setsCodeWithSubstring2() {
        Resource resource = new Resource("r-mycode", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();

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

    // Pins the asymmetry: lockScreen branch writes nothing to the repository.
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
            webActivity
                    .when(() -> RichMediaWebActivity.createRichMediaLockScreenIntent(any(), any(), any()))
                    .thenReturn(mockIntent);

            factory.showResource(wrapper);

            verify(mockRepository, never()).setCurrentInAppCode(any());
            verify(mockRepository, never()).setCurrentRichMediaCode(any());
        }
    }

    // Pins the branch-priority contract: IN_APP type wins over isLockScreen() flag.
    // Reordering the conditionals would silently change behaviour.
    @Test
    public void showResource_inAppWithLockScreen_inAppBranchTakesPriority() {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper = new ResourceWrapper.Builder()
                .setResource(resource)
                .setLockScreen(true)
                .setSound("snd")
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
            webActivity.verify(
                    () -> RichMediaWebActivity.createRichMediaLockScreenIntent(any(), any(), any()), never());
            verify(mockContext, never()).startActivity(any());
            verify(mockRepository).setCurrentInAppCode("code1");
        }
    }
}
