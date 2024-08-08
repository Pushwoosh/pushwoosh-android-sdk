package com.pushwoosh;

import android.os.Bundle;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest="AndroidManifest.xml")
public class PushwooshTest {
    private PlatformTestManager platformTestManager;

    private static final String HASH = "test_hash";
    private static final String METADATA = "test_metadata";
    private PushwooshRepository pushwooshRepository;

    private Method sendMessageDelivery;
    private Method sendPushStat;

    @Before
    public void setUp() throws Exception {
        Config configMock = MockConfig.createMock();

        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();

        pushwooshRepository = platformTestManager.getPushwooshRepositoryMock();

        sendPushStat = Pushwoosh.class.getDeclaredMethod("sendPushStat", Bundle.class);
        sendMessageDelivery = Pushwoosh.class.getDeclaredMethod("sendMessageDelivery", Bundle.class);
        sendPushStat.setAccessible(true);
        sendMessageDelivery.setAccessible(true);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void shouldSendMessageDeliveryWithHash() throws InvocationTargetException, IllegalAccessException {
        Bundle mBundle = new Bundle();
        mBundle.putString("p",HASH);
        mBundle.putInt("pw_msg",1);
        mBundle.putString("md", METADATA);

        sendMessageDelivery.invoke(Pushwoosh.getInstance(), mBundle);
        Mockito.verify(pushwooshRepository).sendPushDelivered(HASH, METADATA);
    }

    @Test
    public void shouldNotSendMessageDeliveryWithoutHash() throws InvocationTargetException, IllegalAccessException {
        Bundle mBundle = new Bundle();
        mBundle.putInt("pw_msg",1);
        mBundle.putString("md",METADATA);

        sendMessageDelivery.invoke(Pushwoosh.getInstance(), mBundle);
        Mockito.verify(pushwooshRepository,Mockito.atMost(0)).sendPushDelivered(HASH, METADATA);
    }

    @Test
    public void shouldNotSendMessageDeliveryWithoutPwMsg() throws InvocationTargetException, IllegalAccessException {
        Bundle mBundle = new Bundle();
        mBundle.putString("p",HASH);
        mBundle.putString("md",METADATA);

        sendMessageDelivery.invoke(Pushwoosh.getInstance(), mBundle);
        Mockito.verify(pushwooshRepository,Mockito.atMost(0)).sendPushDelivered(HASH, METADATA);
    }

    @Test
    public void shouldSendPushStatWithHashAndMetadata() throws InvocationTargetException, IllegalAccessException {
        Bundle mBundle = new Bundle();
        mBundle.putString("p",HASH);
        mBundle.putString("md",METADATA);
        mBundle.putInt("pw_msg",1);

        sendPushStat.invoke(Pushwoosh.getInstance(), mBundle);
        Mockito.verify(pushwooshRepository).sendPushOpened(HASH, PushBundleDataProvider.getPushMetadata(mBundle));
    }

    @Test
    public void shouldNotSendPushStatWithoutHash()  throws InvocationTargetException, IllegalAccessException {
        Bundle mBundle = new Bundle();
        mBundle.putString("md",METADATA);
        mBundle.putInt("pw_msg",1);

        sendPushStat.invoke(Pushwoosh.getInstance(), mBundle);
        Mockito.verify(pushwooshRepository,Mockito.atMost(0)).sendPushOpened(HASH, PushBundleDataProvider.getPushMetadata(mBundle));
    }

    @Test
    public void shouldNotSendPushStatWithoutPwMsg() throws InvocationTargetException, IllegalAccessException  {
        Bundle mBundle = new Bundle();
        mBundle.putString("p",HASH);
        mBundle.putString("md",METADATA);

        sendPushStat.invoke(Pushwoosh.getInstance(), mBundle);
        Mockito.verify(pushwooshRepository,Mockito.atMost(0)).sendPushOpened(HASH, PushBundleDataProvider.getPushMetadata(mBundle));
    }
}