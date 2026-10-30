package com.pushwoosh.location.foregroundservice;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.app.Notification;
import android.app.Service;

import com.pushwoosh.internal.utils.ImageUtils;
import com.pushwoosh.location.AndroidManifestLocationConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
public class ForegroundServiceHelperTest {

    private final Locale defaultLocale = Locale.getDefault();

    @Before
    public void setUp() {
        Application application = RuntimeEnvironment.getApplication();
        if (application.getApplicationInfo().labelRes == 0) {
            application.getApplicationInfo().nonLocalizedLabel = "App";
        }
    }

    @After
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    // Channel ids already created on Turkish devices must survive SDK updates.
    @Test
    public void startForeground_channelIdKeepsDeviceLocaleLowercasing() {
        Locale.setDefault(new Locale("tr", "TR"));
        AndroidManifestLocationConfig config = mock(AndroidManifestLocationConfig.class);
        when(config.getChannelNameForegroundServiceNotification()).thenReturn("GPS Info");
        ForegroundServiceHelper helper =
                new ForegroundServiceHelper(RuntimeEnvironment.getApplication(), config, mock(ImageUtils.class));
        Service service = mock(Service.class);

        helper.startForeground(service);

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(service).startForeground(eq(ForegroundServiceHelper.FOREGROUND_SERVICE_ID), notification.capture());
        assertEquals("gpsınfo", notification.getValue().getChannelId());
    }
}
