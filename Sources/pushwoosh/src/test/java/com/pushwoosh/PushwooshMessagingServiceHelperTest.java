package com.pushwoosh;

import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PushwooshMessagingServiceHelperTest {
    private MockedStatic<PushwooshInitializer> initializer;
    private MockedStatic<NotificationRegistrarHelper> registrar;
    private MockedStatic<PushStatisticsScheduler> scheduler;
    private final Context context = mock(Context.class);
    private final Bundle bundle = new Bundle();

    @Before
    public void setUp() {
        SdkStateProvider.getInstance().resetForTesting();
        initializer = mockStatic(PushwooshInitializer.class);
        registrar = mockStatic(NotificationRegistrarHelper.class);
        scheduler = mockStatic(PushStatisticsScheduler.class);
    }

    @After
    public void tearDown() {
        scheduler.close();
        registrar.close();
        initializer.close();
        SdkStateProvider.getInstance().resetForTesting();
    }

    @Test
    public void receiptTimeIsArrivalTime() {
        SdkStateProvider.getInstance().setReady();

        long before = System.currentTimeMillis();
        PushwooshMessagingServiceHelper.onMessageReceived(context, bundle);
        long after = System.currentTimeMillis();

        scheduler.verify(() ->
                PushStatisticsScheduler.scheduleDeliveryEvent(same(bundle), longThat(v -> v >= before && v <= after)));
    }

    @Test
    public void receiptTimeCapturedBeforeSdkReady() throws Exception {
        long before = System.currentTimeMillis();
        PushwooshMessagingServiceHelper.onMessageReceived(context, bundle);
        long after = System.currentTimeMillis();
        while (System.currentTimeMillis() <= after) {
            Thread.sleep(1);
        }

        SdkStateProvider.getInstance().setReady();

        scheduler.verify(
                () -> PushStatisticsScheduler.scheduleDeliveryEvent(
                        same(bundle), longThat(v -> v >= before && v <= after)),
                timeout(2000));
    }
}
