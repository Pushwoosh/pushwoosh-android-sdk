package com.pushwoosh.firebase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.PushwooshMessagingServiceHelper;
import com.pushwoosh.firebase.internal.specific.FcmDeviceSpecificIniter;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshFcmHelperReceivedFlagTest {

    private PreferenceBooleanValue flag;
    private RegistrationPrefs prefs;
    private Context context;

    @Before
    public void setUp() {
        new DeviceSpecificProvider.Builder()
                .setDeviceSpecific(FcmDeviceSpecificIniter.create())
                .build(true);
        flag = mock(PreferenceBooleanValue.class);
        prefs = mock(RegistrationPrefs.class);
        when(prefs.fcmMessageReceivedSinceInstallOrUpdate()).thenReturn(flag);
        context = mock(Context.class);
    }

    private static RemoteMessage pushwooshMessage() {
        Map<String, String> data = new HashMap<>();
        data.put("pw_msg", "1");
        return new RemoteMessage.Builder("123").clearData().setData(data).build();
    }

    private static RemoteMessage foreignMessage() {
        Map<String, String> data = new HashMap<>();
        data.put("another_provider_payload", "1");
        return new RemoteMessage.Builder("123").clearData().setData(data).build();
    }

    private void deliver(RemoteMessage message) {
        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshMessagingServiceHelper> messaging =
                        mockStatic(PushwooshMessagingServiceHelper.class)) {
            repo.when(RepositoryModule::getRegistrationPreferences).thenReturn(prefs);
            messaging
                    .when(() ->
                            PushwooshMessagingServiceHelper.onMessageReceived(any(Context.class), any(Bundle.class)))
                    .thenReturn(true);
            PushwooshFcmHelper.onMessageReceived(context, message);
        }
    }

    @Test
    public void onMessageReceived_pushwooshMessage_marksFlag() {
        deliver(pushwooshMessage());

        verify(flag).set(true);
    }

    @Test
    public void onMessageReceived_flagAlreadySet_doesNotWriteAgain() {
        when(flag.get()).thenReturn(true);

        deliver(pushwooshMessage());

        verify(flag, never()).set(anyBoolean());
    }

    @Test
    public void onMessageReceived_foreignMessage_leavesFlagUntouched() {
        deliver(foreignMessage());

        verify(flag, never()).set(anyBoolean());
    }

    @Test
    public void onMessageReceived_nullMessage_leavesFlagUntouched() {
        deliver(null);

        verify(flag, never()).set(anyBoolean());
    }
}
