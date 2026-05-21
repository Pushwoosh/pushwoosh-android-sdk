package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.DisplayMetrics;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.preference.PreferenceSoundTypeValue;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

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

/**
 * Skipped methods (and why) so reviewer doesn't ask:
 * <ul>
 *   <li>rebuildWithDefaultValuesIfNeeded (Notification.Builder.recoverBuilder + AppIconHelper + NotificationManager service)</li>
 *   <li>turnScreenOn (PowerManager wake lock side-effect)</li>
 *   <li>getPathFileInAssets / tryGetBitmap / tryToGetBitmapFromInternet / tryToGetBitmapFromDisk (real I/O)</li>
 *   <li>getUriForAssetPath (file system + AssetManager copy)</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class NotificationUtilsTest {

    @Mock
    private ResourceProvider resourceProvider;

    @Mock
    private ManagerProvider managerProvider;

    @Mock
    private AppInfoProvider appInfoProvider;

    @Mock
    private AudioManager audioManager;

    @Mock
    private NotificationPrefs notificationPrefs;

    @Mock
    private PreferenceSoundTypeValue soundTypePref;

    @Mock
    private PushwooshPlatform pushwooshPlatform;

    @Mock
    private com.pushwoosh.internal.utils.Config pushwooshConfig;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // ===== tryToGetIconFormStringOrGetFromApplication =====

    @Test
    public void tryToGetIconFormStringOrGetFromApplication_remoteIconNameResolves_returnsRemoteId() {
        when(resourceProvider.getIdentifier("custom_icon", "drawable")).thenReturn(4242);
        when(resourceProvider.getIdentifier("pw_notification", "drawable")).thenReturn(0);

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> platformInstanceMock = mockStatic(PushwooshPlatform.class)) {
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
            platformInstanceMock.when(PushwooshPlatform::getInstance).thenReturn(null);

            int result = NotificationUtils.tryToGetIconFormStringOrGetFromApplication("custom_icon");

            assertEquals(4242, result);
        }
    }

    @Test
    public void tryToGetIconFormStringOrGetFromApplication_onlyPwNotificationDrawableSet_returnsPwId() {
        when(resourceProvider.getIdentifier("pw_notification", "drawable")).thenReturn(777);

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> platformInstanceMock = mockStatic(PushwooshPlatform.class)) {
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
            platformInstanceMock.when(PushwooshPlatform::getInstance).thenReturn(null);

            int result = NotificationUtils.tryToGetIconFormStringOrGetFromApplication(null);

            assertEquals(777, result);
        }
    }

    @Test
    public void tryToGetIconFormStringOrGetFromApplication_manifestIconOverridesPwNotification() {
        when(resourceProvider.getIdentifier("pw_notification", "drawable")).thenReturn(100);
        when(pushwooshPlatform.getConfig()).thenReturn(pushwooshConfig);
        when(pushwooshConfig.getNotificationIcon()).thenReturn(200);

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> platformInstanceMock = mockStatic(PushwooshPlatform.class)) {
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
            platformInstanceMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);

            int result = NotificationUtils.tryToGetIconFormStringOrGetFromApplication(null);

            assertEquals(200, result);
        }
    }

    @Test
    public void tryToGetIconFormStringOrGetFromApplication_remoteIconWinsOverManifest() {
        when(resourceProvider.getIdentifier("pw_notification", "drawable")).thenReturn(0);
        when(resourceProvider.getIdentifier("custom_icon", "drawable")).thenReturn(300);
        when(pushwooshPlatform.getConfig()).thenReturn(pushwooshConfig);
        when(pushwooshConfig.getNotificationIcon()).thenReturn(200);

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> platformInstanceMock = mockStatic(PushwooshPlatform.class)) {
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
            platformInstanceMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatform);

            int result = NotificationUtils.tryToGetIconFormStringOrGetFromApplication("custom_icon");

            assertEquals(300, result);
        }
    }

    @Test
    public void tryToGetIconFormStringOrGetFromApplication_nothingResolves_returnsMinusOne() {
        when(resourceProvider.getIdentifier(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(0);

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> platformInstanceMock = mockStatic(PushwooshPlatform.class)) {
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
            platformInstanceMock.when(PushwooshPlatform::getInstance).thenReturn(null);

            int result = NotificationUtils.tryToGetIconFormStringOrGetFromApplication("unresolved_icon");

            assertEquals(-1, result);
        }
    }

    // ===== getScaleBitmap =====

    @Test
    public void getScaleBitmap_withDisplayMetrics_scalesByDensity() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.density = 2.0f;
        when(resourceProvider.getDisplayMetrics()).thenReturn(displayMetrics);

        Bitmap source = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class)) {
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);

            Bitmap result = NotificationUtils.getScaleBitmap(source, 100);

            // outHeight = 100 * 2.0 = 200; outWidth = 100 * 200 / 200 = 100
            assertEquals(200, result.getHeight());
            assertEquals(100, result.getWidth());
        }
    }

    @Test
    public void getScaleBitmap_nullDisplayMetrics_usesRawOutHeight() {
        when(resourceProvider.getDisplayMetrics()).thenReturn(null);

        Bitmap source = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class)) {
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);

            Bitmap result = NotificationUtils.getScaleBitmap(source, 50);

            // outHeight = 50; outWidth = 100 * 50 / 200 = 25
            assertEquals(50, result.getHeight());
            assertEquals(25, result.getWidth());
        }
    }

    // ===== getSoundUri =====

    private void stubSoundUriEnv(
            MockedStatic<AndroidPlatformModule> platformMock,
            MockedStatic<RepositoryModule> repositoryMock,
            MockedStatic<RingtoneManager> ringtoneMock,
            Uri defaultUri) {
        when(notificationPrefs.soundType()).thenReturn(soundTypePref);
        when(managerProvider.getAudioManager()).thenReturn(audioManager);
        platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
        repositoryMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
        ringtoneMock
                .when(() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .thenReturn(defaultUri);
    }

    @Test
    public void getSoundUri_soundTypeNoSound_returnsNull() {
        when(soundTypePref.get()).thenReturn(SoundType.NO_SOUND);
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        Uri defaultUri = Uri.parse("content://default");

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryMock = mockStatic(RepositoryModule.class);
                MockedStatic<RingtoneManager> ringtoneMock = mockStatic(RingtoneManager.class)) {
            stubSoundUriEnv(platformMock, repositoryMock, ringtoneMock, defaultUri);

            assertNull(NotificationUtils.getSoundUri("anything"));
        }
    }

    @Test
    public void getSoundUri_alwaysWithEmptySound_returnsNull() {
        when(soundTypePref.get()).thenReturn(SoundType.ALWAYS);
        Uri defaultUri = Uri.parse("content://default");

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryMock = mockStatic(RepositoryModule.class);
                MockedStatic<RingtoneManager> ringtoneMock = mockStatic(RingtoneManager.class)) {
            stubSoundUriEnv(platformMock, repositoryMock, ringtoneMock, defaultUri);

            assertNull(NotificationUtils.getSoundUri(""));
        }
    }

    @Test
    public void getSoundUri_alwaysWithDefaultSoundName_returnsDefaultUri() {
        when(soundTypePref.get()).thenReturn(SoundType.ALWAYS);
        Uri defaultUri = Uri.parse("content://default");

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryMock = mockStatic(RepositoryModule.class);
                MockedStatic<RingtoneManager> ringtoneMock = mockStatic(RingtoneManager.class)) {
            stubSoundUriEnv(platformMock, repositoryMock, ringtoneMock, defaultUri);

            assertSame(defaultUri, NotificationUtils.getSoundUri("default"));
        }
    }

    @Test
    public void getSoundUri_defaultModeWithSilentRinger_returnsNull() {
        when(soundTypePref.get()).thenReturn(SoundType.DEFAULT_MODE);
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_SILENT);
        Uri defaultUri = Uri.parse("content://default");

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryMock = mockStatic(RepositoryModule.class);
                MockedStatic<RingtoneManager> ringtoneMock = mockStatic(RingtoneManager.class)) {
            stubSoundUriEnv(platformMock, repositoryMock, ringtoneMock, defaultUri);

            assertNull(NotificationUtils.getSoundUri("custom_sound"));
        }
    }

    // Verifies that DEFAULT_MODE + NORMAL ringer + resolvable raw resource → android.resource:// URI.
    // Restored from cross-check: covers the only happy path that resolves a custom sound to a resource URI
    // (other getSoundUri tests exit via "default" shortcut, empty-sound, NO_SOUND, or SILENT ringer).
    @Test
    public void getSoundUri_defaultModeWithNormalRingerAndResolvableSound_returnsResourceUri() {
        when(soundTypePref.get()).thenReturn(SoundType.DEFAULT_MODE);
        when(audioManager.getRingerMode()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        when(resourceProvider.getIdentifier("custom_sound", "raw")).thenReturn(123);
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");
        Uri defaultUri = Uri.parse("content://default");

        try (MockedStatic<AndroidPlatformModule> platformMock = mockStatic(AndroidPlatformModule.class);
                MockedStatic<RepositoryModule> repositoryMock = mockStatic(RepositoryModule.class);
                MockedStatic<RingtoneManager> ringtoneMock = mockStatic(RingtoneManager.class)) {
            stubSoundUriEnv(platformMock, repositoryMock, ringtoneMock, defaultUri);
            platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
            platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);

            Uri result = NotificationUtils.getSoundUri("custom_sound");

            assertEquals(Uri.parse("android.resource://com.pushwoosh.test/123"), result);
        }
    }
}
