package com.pushwoosh.inapp.view.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.event.RichMediaErrorEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.nativeui.NativeInAppPresenter;
import com.pushwoosh.inapp.nativeui.NativeInAppPresenterProvider;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.view.ModalRichMediaWindow;
import com.pushwoosh.inapp.view.RichMediaWebActivity;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.RepositoryTestManager;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class ResourceViewStrategyFactoryTest {

    private ResourceViewStrategyFactory factory;
    private AutoCloseable mocks;
    private MockedStatic<BackgroundExecutor> backgroundExecutor;
    private MockedStatic<InAppModule> inAppModule;
    private File nativeConfigDir;
    private RegistrationPrefs registrationPrefs;

    @Mock
    private Context mockContext;

    @Mock
    private PushwooshPlatform mockPlatform;

    @Mock
    private PushwooshRepository mockRepository;

    @Mock
    private InAppRepository mockInAppRepository;

    @Mock
    private InAppFolderProvider mockFolderProvider;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        factory = new ResourceViewStrategyFactory();
        when(mockPlatform.pushwooshRepository()).thenReturn(mockRepository);

        // Run pool and main hops inline so the resolve->detect->route pipeline is synchronous
        // (EventBus.sendEvent dispatches through BackgroundExecutor.main internally).
        backgroundExecutor = Mockito.mockStatic(BackgroundExecutor.class);
        backgroundExecutor
                .when(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });
        backgroundExecutor
                .when(() -> BackgroundExecutor.main(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });

        nativeConfigDir = Files.createTempDirectory("native-inapp-test").toFile();

        inAppModule = Mockito.mockStatic(InAppModule.class);
        inAppModule.when(InAppModule::getInAppRepository).thenReturn(mockInAppRepository);
        inAppModule.when(InAppModule::getInAppFolderProvider).thenReturn(mockFolderProvider);
        // Defaults: resolve succeeds with the same resource, no native config on disk -> HTML route.
        when(mockInAppRepository.ensureResolvedAndDeployed(any(Resource.class)))
                .thenAnswer(invocation -> Result.fromData(invocation.getArgument(0)));
        when(mockFolderProvider.getNativeConfigFile(any()))
                .thenReturn(new File(nativeConfigDir, "absent/native-config.json"));
    }

    @After
    public void tearDown() throws Exception {
        NativeInAppPresenterProvider.set(null);
        inAppModule.close();
        backgroundExecutor.close();
        mocks.close();
        // givenDeviceLanguagePrefs() mutates process-global state (RepositoryModule prefs +
        // AndroidPlatformModule.init); without this reset it leaks into later tests in the same JVM fork.
        if (registrationPrefs != null) {
            RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
            registrationPrefs = null;
        }
        resetAndroidPlatformModule();
    }

    private static void resetAndroidPlatformModule() {
        try {
            Field contextField = AndroidPlatformModule.class.getDeclaredField("context");
            contextField.setAccessible(true);
            contextField.set(AndroidPlatformModule.getInstance(), null);
        } catch (Exception ignored) {
            // best-effort reset — leave the module initialized if reflection fails
        }
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

    private static final String CONFIG_JSON = "{\"displayType\":\"modal\",\"modal\":{\"title\":{\"text\":\"hi\"}}}";

    private void givenNativeConfigOnDisk(String code, String json) throws IOException {
        File configFile = new File(nativeConfigDir, "native-config.json");
        FileUtils.writeFile(configFile, json);
        when(mockFolderProvider.getNativeConfigFile(code)).thenReturn(configFile);
    }

    private void givenPushwooshJsonOnDisk(String code, String json) throws IOException {
        File configFile = new File(nativeConfigDir, "pushwoosh.json");
        FileUtils.writeFile(configFile, json);
        when(mockFolderProvider.getConfigFile(code)).thenReturn(configFile);
    }

    // In-memory registration prefs so InAppConfig.parseLocalizedStrings can resolve the device language
    // (it falls back to default_language when the device language is absent from the localization block).
    private void givenDeviceLanguagePrefs() {
        com.pushwoosh.internal.utils.Config configMock = MockConfig.createMock();
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(configMock, mock(DeviceRegistrar.class));
        RepositoryModule.setRegistrationPreferences(registrationPrefs);
    }

    // pushwoosh.json next to native-config.json -> present() receives the LOCALIZED JSON.
    @Test
    public void showResource_nativeConfigWithPushwooshJson_presenterGetsLocalizedJson() throws Exception {
        givenDeviceLanguagePrefs();
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        givenNativeConfigOnDisk(
                "code1", "{\"displayType\":\"modal\",\"modal\":{\"title\":{\"text\":\"{{Greeting|text|Hello}}\"}}}");
        givenPushwooshJsonOnDisk(
                "code1", "{\"default_language\":\"en\",\"localization\":{\"en\":{\"Greeting\":\"Bonjour\"}}}");

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        when(presenter.present(any(), any())).thenReturn(true);
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            ArgumentCaptor<String> configCaptor = ArgumentCaptor.forClass(String.class);
            verify(presenter).present(configCaptor.capture(), eq(resource));
            JSONObject captured = new JSONObject(configCaptor.getValue());
            assertEquals(
                    "Bonjour",
                    captured.getJSONObject("modal").getJSONObject("title").getString("text"));
        }
    }

    // No pushwoosh.json -> present() receives the config with placeholders collapsed to their defaults.
    @Test
    public void showResource_nativeConfigWithoutPushwooshJson_presenterGetsPlaceholderDefaults() throws Exception {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        givenNativeConfigOnDisk(
                "code1", "{\"displayType\":\"modal\",\"modal\":{\"title\":{\"text\":\"{{Greeting|text|Hello}}\"}}}");
        // getConfigFile left unstubbed -> parseLocalizedStrings throws -> empty map -> defaults applied.

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        when(presenter.present(any(), any())).thenReturn(true);
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            ArgumentCaptor<String> configCaptor = ArgumentCaptor.forClass(String.class);
            verify(presenter).present(configCaptor.capture(), eq(resource));
            JSONObject captured = new JSONObject(configCaptor.getValue());
            assertEquals(
                    "Hello",
                    captured.getJSONObject("modal").getJSONObject("title").getString("text"));
        }
    }

    // Native config on disk -> presenter gets the raw JSON and the resource; no WebView host.
    @Test
    public void showResource_nativeConfig_callsPresenterAndSkipsWebViewHost() throws Exception {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        givenNativeConfigOnDisk("code1", CONFIG_JSON);

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        when(presenter.present(any(), any())).thenReturn(true);
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            ArgumentCaptor<String> configCaptor = ArgumentCaptor.forClass(String.class);
            verify(presenter).present(configCaptor.capture(), eq(resource));
            // The config now round-trips through NativeConfigLocalizer (parse -> localize -> serialize),
            // which normalizes formatting and strips the trailing newline; assert on parsed content.
            JSONObject captured = new JSONObject(configCaptor.getValue());
            assertEquals("modal", captured.getString("displayType"));
            assertEquals(
                    "hi", captured.getJSONObject("modal").getJSONObject("title").getString("text"));
            modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(any()), never());
            verify(mockContext, never()).startActivity(any());
        }
    }

    // Detect is symmetric for r-* codes: rich media resources go native too.
    @Test
    public void showResource_nativeConfigOnRichMedia_callsPresenter() throws Exception {
        Resource resource = new Resource("r-code2", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        givenNativeConfigOnDisk("r-code2", CONFIG_JSON);

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        when(presenter.present(any(), any())).thenReturn(true);
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            verify(presenter).present(any(), eq(resource));
            verify(mockContext, never()).startActivity(any());
        }
    }

    // No presenter registered (module not connected) -> silent skip, nothing opens, no crash.
    @Test
    public void showResource_nativeConfigWithoutPresenter_skips() throws Exception {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        givenNativeConfigOnDisk("code1", CONFIG_JSON);
        NativeInAppPresenterProvider.set(null);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(any()), never());
            verify(mockContext, never()).startActivity(any());
        }
    }

    // Presenter returned false -> skip; no fallback to HTML (native config is terminal).
    @Test
    public void showResource_presenterReturnsFalse_skipsWithoutHtmlFallback() throws Exception {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        givenNativeConfigOnDisk("code1", CONFIG_JSON);

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        when(presenter.present(any(), any())).thenReturn(false);
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            verify(presenter).present(any(), eq(resource));
            modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(any()), never());
            verify(mockContext, never()).startActivity(any());
        }
    }

    // Presenter threw -> caught (symmetric to the existing catch in showResource), no crash.
    @Test
    public void showResource_presenterThrows_exceptionIsCaught() throws Exception {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        givenNativeConfigOnDisk("code1", CONFIG_JSON);

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        when(presenter.present(any(), any())).thenThrow(new RuntimeException("boom"));
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper); // must not throw
            verify(presenter).present(any(), eq(resource));
        }
    }

    // Lockscreen + native config -> skip (v1 has no native lockscreen host).
    @Test
    public void showResource_lockScreenWithNativeConfig_skips() throws Exception {
        Resource resource = new Resource("r-code3", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper = new ResourceWrapper.Builder()
                .setResource(resource)
                .setLockScreen(true)
                .setSound("")
                .build();
        givenNativeConfigOnDisk("r-code3", CONFIG_JSON);

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            verify(presenter, never()).present(any(), any());
            webActivity.verify(
                    () -> RichMediaWebActivity.createRichMediaLockScreenIntent(any(), any(), any()), never());
            verify(mockContext, never()).startActivity(any());
        }
    }

    // delay is honored on the native path: postDelayed before dispatch, like showRichMedia().
    @Test
    public void showResource_nativeWithDelay_presentsAfterDelay() throws Exception {
        Resource resource = new Resource("r-code4", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper = new ResourceWrapper.Builder()
                .setResource(resource)
                .setDelay(500L)
                .build();
        givenNativeConfigOnDisk("r-code4", CONFIG_JSON);

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        when(presenter.present(any(), any())).thenReturn(true);
        NativeInAppPresenterProvider.set(presenter);

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);
            verify(presenter, never()).present(any(), any());

            Robolectric.getForegroundThreadScheduler().advanceBy(500, TimeUnit.MILLISECONDS);
            verify(presenter).present(any(), eq(resource));
        }
    }

    // IOException while reading native-config.json -> RichMediaErrorEvent (like every other failure
    // in the pipeline) and skip; native config is terminal, so no HTML fallback even though
    // presentHtml would succeed.
    @Test
    public void showResource_nativeConfigReadFails_sendsErrorEventWithoutHtmlFallback() {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        // A directory exists() but readFile() on it throws IOException.
        File configAsDirectory = new File(nativeConfigDir, "native-config.json");
        configAsDirectory.mkdirs();
        when(mockFolderProvider.getNativeConfigFile("code1")).thenReturn(configAsDirectory);

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        NativeInAppPresenterProvider.set(presenter);

        List<RichMediaErrorEvent> events = new ArrayList<>();
        Subscription<RichMediaErrorEvent> subscription = EventBus.subscribe(RichMediaErrorEvent.class, events::add);
        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            assertEquals(1, events.size());
            assertEquals(resource, events.get(0).getResource());
            assertTrue(events.get(0).getException().getCause() instanceof IOException);
            verify(presenter, never()).present(any(), any());
            modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(any()), never());
            verify(mockContext, never()).startActivity(any());
        } finally {
            subscription.unsubscribe();
        }
    }

    // ensure failure -> RichMediaErrorEvent (as DownloadHtmlTask sends it today), host never opens.
    @Test
    public void showResource_ensureFails_sendsErrorEventAndStops() {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        when(mockInAppRepository.ensureResolvedAndDeployed(any(Resource.class)))
                .thenReturn(
                        Result.fromException(new ResourceParseException("Can't download or update richMedia: code1")));

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        NativeInAppPresenterProvider.set(presenter);

        List<RichMediaErrorEvent> events = new ArrayList<>();
        Subscription<RichMediaErrorEvent> subscription = EventBus.subscribe(RichMediaErrorEvent.class, events::add);
        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            assertEquals(1, events.size());
            assertEquals(resource, events.get(0).getResource());
            verify(presenter, never()).present(any(), any());
            modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(any()), never());
            verify(mockContext, never()).startActivity(any());
        } finally {
            subscription.unsubscribe();
        }
    }

    // Result.isSuccess() only means exception == null, so "success with null data" is possible
    // by convention; the guard turns it into an error event instead of an NPE on the pool.
    @Test
    public void showResource_ensureSucceedsWithNullData_sendsErrorEventAndStops() {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        when(mockInAppRepository.ensureResolvedAndDeployed(any(Resource.class))).thenReturn(Result.fromData(null));

        NativeInAppPresenter presenter = mock(NativeInAppPresenter.class);
        NativeInAppPresenterProvider.set(presenter);

        List<RichMediaErrorEvent> events = new ArrayList<>();
        Subscription<RichMediaErrorEvent> subscription = EventBus.subscribe(RichMediaErrorEvent.class, events::add);
        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<ModalRichMediaWindow> modalWindow = Mockito.mockStatic(ModalRichMediaWindow.class);
                MockedStatic<RichMediaWebActivity> webActivity = Mockito.mockStatic(RichMediaWebActivity.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            assertEquals(1, events.size());
            assertEquals(resource, events.get(0).getResource());
            verify(presenter, never()).present(any(), any());
            modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(any()), never());
            verify(mockContext, never()).startActivity(any());
        } finally {
            subscription.unsubscribe();
        }
    }

    // Verifies that a null wrapper is a silent no-op: nothing is scheduled on the pool.
    // Kills the mutant dropping the null-guard (would NPE on resourceWrapper.getResource()).
    @Test
    public void showResource_nullWrapper_noCrashAndNothingScheduled() {
        factory.showResource(null);

        backgroundExecutor.verify(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)), never());
    }

    // Verifies that a wrapper without a resource is a silent no-op: nothing is scheduled on the pool.
    // Kills the mutant dropping the null-guard (would NPE on resource.getCode() in the log line).
    // Context is mocked non-null so the later context guard cannot mask that mutant.
    @Test
    public void showResource_nullResource_noCrashAndNothingScheduled() {
        ResourceWrapper wrapper = new ResourceWrapper.Builder().build();

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            backgroundExecutor.verify(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)), never());
        }
    }

    // Verifies that a null application context aborts the show before any background work starts.
    // Kills the mutant dropping the context null-guard (pipeline would run with null context).
    @Test
    public void showResource_nullContext_noCrashAndNothingScheduled() {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();

        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(null);

            factory.showResource(wrapper);

            backgroundExecutor.verify(() -> BackgroundExecutor.executeOnPool(any(Runnable.class)), never());
        }
    }

    // Verifies that a missing InAppRepository is reported via RichMediaErrorEvent instead of crashing.
    // Kills the mutant dropping the repository null-guard (would NPE in ensureResolvedAndDeployed).
    @Test
    public void showResource_repositoryNull_sendsErrorEventWithoutCrash() {
        Resource resource = new Resource("code1", "http://example.com", "", 0, null, null, false, 0);
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();
        inAppModule.when(InAppModule::getInAppRepository).thenReturn(null);

        List<RichMediaErrorEvent> events = new ArrayList<>();
        Subscription<RichMediaErrorEvent> subscription = EventBus.subscribe(RichMediaErrorEvent.class, events::add);
        try (MockedStatic<AndroidPlatformModule> platformModule = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformModule.when(AndroidPlatformModule::getApplicationContext).thenReturn(mockContext);

            factory.showResource(wrapper);

            assertEquals(1, events.size());
            assertTrue(events.get(0).getException().getMessage().contains("InAppRepository is not initialized"));
        } finally {
            subscription.unsubscribe();
        }
    }

    // Verifies that the lockscreen branch launches the activity with the lockscreen intent.
    // Sister test to showResource_lockScreen_doesNotSetRepositoryCodes: kills the mutants that
    // silently drop the showRichMediaLockScreen / startActivity calls.
    @Test
    public void showResource_lockScreen_startsLockScreenActivity() {
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

            verify(mockContext).startActivity(mockIntent);
        }
    }

    // Verifies that a rich media resource with MODAL rich media type opens the modal window.
    // Kills the mutants dropping the MODAL type check / the showModalRichMediaWindow call.
    @Test
    public void showResource_richMediaModalType_showsModalWindow() {
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

            modalWindow.verify(() -> ModalRichMediaWindow.showModalRichMediaWindow(resource));
        }
    }
}
