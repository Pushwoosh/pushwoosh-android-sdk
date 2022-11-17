package com.pushwoosh.internal.platform.utils;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.PushwooshSharedDataProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, Uri.class, Handler.class, DeviceUtils.class, AsyncTask.class})
public class DeviceUtilsTest {
    @Mock
    private RegistrationPrefs registrationPrefs;
    @Mock
    private Context context;
    @Mock
    private Application applicationContext;
    @Mock
    private PackageManager packageManager;
    @Mock
    private ContentResolver contentResolver;
    @Mock
    private PreferenceStringValue deviceId;

    private String _deviceId;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.mockStatic(Uri.class);
        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer( invocation -> {
            CharSequence a = (CharSequence) invocation.getArguments()[0];
            return !(a != null && a.length() > 0);
        });
        PowerMockito.when(Uri.parse(anyString())).then(invocation -> {
            Uri mockUri = mock(Uri.class);
            when(mockUri.toString()).thenReturn(invocation.getArgumentAt(0, String.class));
            return mockUri;
        });

        MockitoAnnotations.initMocks(this);
        when(context.getApplicationContext()).thenReturn(applicationContext);
        when(applicationContext.getPackageManager()).thenReturn(packageManager);
        RepositoryModule.setRegistrationPreferences(registrationPrefs);
        when(registrationPrefs.deviceId()).thenReturn(deviceId);
        when(applicationContext.getContentResolver()).thenReturn(contentResolver);
        when(applicationContext.getPackageName()).thenReturn("com.pushwoosh");
        doAnswer(invocation -> _deviceId).when(deviceId).get();
        doAnswer(invocation -> {
            _deviceId = invocation.getArgumentAt(0, String.class);
            return null;
        }).when(deviceId).set(anyString());

        AndroidPlatformModule.init(context, true);
        DeviceUtils.initHWIDGenerators();

        PushwooshPlatform pushwooshPlatform = Mockito.mock(PushwooshPlatform.class);
        try {
            Field instance = PushwooshPlatform.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, pushwooshPlatform);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException();
        }
        Config config = Mockito.mock(Config.class);
        when(PushwooshPlatform.getInstance().getConfig()).thenReturn(config);

        Handler handler = PowerMockito.mock(Handler.class);
        try {
            whenNew(Handler.class).withNoArguments().thenReturn(handler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        doNothing().when(handler).removeCallbacksAndMessages(null);

        PowerMockito.suppress(PowerMockito.method(AsyncTask.class, "onPostExecute", Object.class));
        PowerMockito.replace(PowerMockito.method(AsyncTask.class, "execute", Object.class))
                .with((o, method, objects) -> {
            new Thread(() -> {
                try {
                    Object result = o.getClass().getDeclaredMethod("doInBackground", Void[].class).invoke(o, new Object[]{ new Void[] {} });
                    o.getClass().getDeclaredMethod("onPostExecute", Object.class).invoke(o, result);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
            return o;
        });

    }

    @Test
    @Ignore("java.lang.NoSuchMethodError")
    public void testReturnHWIDIfSaved() {
        when(registrationPrefs.deviceId().get()).thenReturn("test_hwid");
        DeviceUtils.getDeviceUUID();

        verify(applicationContext, never()).getPackageManager();
    }

    private List<ProviderInfo> providerInfoListWithoutPushwoosh() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = "test.test";
        providerInfo.name = "test.TEST";
        return Collections.singletonList(providerInfo);
    }

    private List<ProviderInfo> providerInfoListWithPushwoosh() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = "test.test." + PushwooshSharedDataProvider.class.getSimpleName();
        providerInfo.name = "com.pushwoosh." + PushwooshSharedDataProvider.class.getSimpleName();
        ArrayList<ProviderInfo> infos = new ArrayList<>(providerInfoListWithoutPushwoosh());
        infos.add(providerInfo);
        return infos;
    }

    private void mockQueryProvider(List<ProviderInfo> info) {
        reset(packageManager);
        when(packageManager.queryContentProviders(any(), anyInt(), anyInt())).thenReturn(info);
    }

    @Test
    @Ignore("java.lang.NoSuchMethodError")
    public void testGenerateIfNoContentProvider() {
        when(registrationPrefs.deviceId().get()).thenReturn(null);

        mockQueryProvider(providerInfoListWithoutPushwoosh());

        String deviceId = DeviceUtils.getDeviceUUID();

        Assert.assertTrue(!deviceId.isEmpty());
        verify(packageManager).queryContentProviders(any(), anyInt(), anyInt());
    }

    @Test
    @Ignore("java.lang.NoSuchMethodError")
    public void testSaveAfterGeneration() {
        _deviceId = null;

        mockQueryProvider(providerInfoListWithoutPushwoosh());

        String deviceId = DeviceUtils.getDeviceUUID();

        Assert.assertTrue(!deviceId.isEmpty());
        verify(registrationPrefs.deviceId()).set(anyString());
    }

    private Cursor mockHWIDCursor(String returnValue) {
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.getColumnCount()).thenReturn(1);
        when(mockCursor.getColumnName(0)).thenReturn(PushwooshSharedDataProvider.HWID_COLUMN_NAME);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getString(0)).thenReturn(returnValue);
        return mockCursor;
    }

    @Test
    @Ignore("java.lang.NoSuchMethodError")
    public void testUseFromContentProviderButNoHWIDInsideIt() {
        when(registrationPrefs.deviceId().get()).thenReturn(null);

        mockQueryProvider(providerInfoListWithPushwoosh());

        ArgumentCaptor<Uri> queryUriCaptor = ArgumentCaptor.forClass(Uri.class);
        ArgumentCaptor<String> querySortOrderCaptor = ArgumentCaptor.forClass(String.class);

        Cursor mockCursor = mockHWIDCursor(null);

        when(contentResolver.query(queryUriCaptor.capture(), any(), any(), any(), querySortOrderCaptor.capture())).thenReturn(mockCursor);

        String deviceId = DeviceUtils.getDeviceUUID();

        Assert.assertTrue(!deviceId.isEmpty());
        verify(applicationContext).getContentResolver();
        verify(contentResolver).query(any(), any(), any(), any(), any());
        Assert.assertEquals(queryUriCaptor.getValue().toString(), "content://test.test." + PushwooshSharedDataProvider.class.getSimpleName() + "/" + PushwooshSharedDataProvider.HWID_PATH);
        Assert.assertEquals(querySortOrderCaptor.getValue(), GeneralUtils.md5("com.pushwoosh"));
    }

    @Test
    @Ignore("java.lang.NoSuchMethodError")
    public void testUseFromContentProviderWithHWID() {
        _deviceId = null;

        mockQueryProvider(providerInfoListWithPushwoosh());

        ArgumentCaptor<Uri> queryUriCaptor = ArgumentCaptor.forClass(Uri.class);
        ArgumentCaptor<String> querySortOrderCaptor = ArgumentCaptor.forClass(String.class);

        String testHWID = "testHWID";

        Cursor mockCursor = mockHWIDCursor(testHWID);

        when(contentResolver.query(queryUriCaptor.capture(), any(), any(), any(), querySortOrderCaptor.capture())).thenReturn(mockCursor);

        String deviceId = DeviceUtils.getDeviceUUID();

        Assert.assertEquals(deviceId, testHWID);
        verify(applicationContext).getContentResolver();
        verify(contentResolver).query(any(), any(), any(), any(), any());
        Assert.assertEquals(queryUriCaptor.getValue().toString(), "content://test.test." + PushwooshSharedDataProvider.class.getSimpleName() + "/" + PushwooshSharedDataProvider.HWID_PATH);
        Assert.assertEquals(querySortOrderCaptor.getValue(), GeneralUtils.md5("com.pushwoosh"));
    }
}