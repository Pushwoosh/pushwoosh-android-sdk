package com.pushwoosh.location.foregroundservice;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.pushwoosh.internal.utils.ImageUtils;
import com.pushwoosh.location.AndroidManifestLocationConfig;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = Build.VERSION_CODES.O)
public class ForegroundServiceHelperTest {

    public static final String TITLE_NAME_APP = "title name app";
    public static final String CONTENT_TEXT = "test text";
    private ForegroundServiceHelper foregroundServiceHelper;
    private Context context;

    @Mock
    private AndroidManifestLocationConfig configMock;
    @Mock
    private Service serviceMock;
    @Mock
    private PackageManager packageManager;
    @Mock
    private ImageUtils imageUtils;
    @Mock
    private Bitmap icon;

    @Captor
    private ArgumentCaptor<Intent> intentArgumentCaptor;
    @Captor
    private ArgumentCaptor<Notification> notificationArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = spy(RuntimeEnvironment.application);
        Drawable drawable = context.getDrawable(android.R.drawable.ic_delete);
        when(packageManager.getApplicationIcon(anyString())).thenReturn(drawable);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(imageUtils.drawableToBitmap(any(Drawable.class))).thenReturn(icon);
        when(configMock.getChannelNameForegroundServiceNotification()).thenReturn("channel_name");
        foregroundServiceHelper = new ForegroundServiceHelper(context, configMock, imageUtils);


    }

    @Test
    public void shouldStartService() {
        when(configMock.isStartForegroundService()).thenReturn(true);
        foregroundServiceHelper.startService();

        verify(context).startForegroundService(intentArgumentCaptor.capture());
        Intent intent = intentArgumentCaptor.getValue();
        Assert.assertEquals(ForegroundService.class.getName(), intent.getComponent().getClassName());
    }

    @Test
    public void shouldNotStartIfInConfigEnable() {
        foregroundServiceHelper.startService();
        verify(context, never()).startService(any());
    }

    @Test
    @Ignore("TODO: broken after AndroidX migration")
    public void shouldStartForeground() {
        context.getApplicationInfo().nonLocalizedLabel = TITLE_NAME_APP;
        when(configMock.getTextForegroundServiceNotification()).thenReturn(CONTENT_TEXT);

        foregroundServiceHelper.startForeground(serviceMock);

        verify(serviceMock).startForeground(eq(101), notificationArgumentCaptor.capture());
        Notification notification = notificationArgumentCaptor.getValue();
        Assert.assertNotNull(notification.getLargeIcon());
    }

    @Test
    public void startServiceShouldNotFailIfContextIsNull(){
        foregroundServiceHelper = new ForegroundServiceHelper(null,configMock, imageUtils);
        foregroundServiceHelper.startService();
    }
}