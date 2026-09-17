/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.firebase.internal.registrar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import android.util.Log;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.tags.TagsBundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class FcmRegistrarTest {
    public static final String DEVICE_ID = "deviceID";
    public static final String TOKEN = "token123";
    private FcmRegistrar fcmRegistrar;
    private WorkManager workManager;
    //    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        //        platformTestManager = new PlatformTestManager();
        //        platformTestManager.onApplicationCreated();

        fcmRegistrar = new FcmRegistrar();
        fcmRegistrar.init();

        final Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(RuntimeEnvironment.application, config);
        workManager = WorkManager.getInstance(RuntimeEnvironment.application);
    }

    @After
    public void tearDown() {
        //        platformTestManager.tearDown();
    }

    // Verifies that registerPW schedules the 14-day periodic refresh and unregisterPW cancels it (SDK-990).
    @Test
    public void unregisterCancelsPeriodicRegistration() throws Exception {
        fcmRegistrar.registerPW(null);

        List<WorkInfo> periodic = workManager
                .getWorkInfosForUniqueWork(FcmRegistrarWorker.PERIODIC_WORK_NAME)
                .get();
        assertEquals(1, periodic.size());
        assertNotEquals(WorkInfo.State.CANCELLED, periodic.get(0).getState());

        fcmRegistrar.unregisterPW();

        periodic = workManager
                .getWorkInfosForUniqueWork(FcmRegistrarWorker.PERIODIC_WORK_NAME)
                .get();
        assertEquals(WorkInfo.State.CANCELLED, periodic.get(0).getState());
    }

    // Verifies that a fresh registration replaces the periodic refresh already scheduled, so the SDK-990 fix
    // reaches installs whose periodic was enqueued before it.
    @Test
    public void reRegistrationReplacesExistingPeriodicWork() throws Exception {
        fcmRegistrar.registerPW(null);
        UUID firstPeriodicId = workManager
                .getWorkInfosForUniqueWork(FcmRegistrarWorker.PERIODIC_WORK_NAME)
                .get()
                .get(0)
                .getId();

        fcmRegistrar.registerPW(null);

        List<WorkInfo> periodic = workManager
                .getWorkInfosForUniqueWork(FcmRegistrarWorker.PERIODIC_WORK_NAME)
                .get();
        assertEquals(1, periodic.size());
        assertNotEquals(firstPeriodicId, periodic.get(0).getId());
    }

    // Verifies that only the immediate request carries tags; the periodic refresh must not re-send them (SDK-990).
    @Test
    public void periodicRegistrationCarriesNoTags() {
        TagsBundle tags =
                new TagsBundle.Builder().putString("test_tag", "test_value").build();

        try (MockedStatic<PushwooshWorkManagerHelper> helperMock =
                Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            helperMock
                    .when(PushwooshWorkManagerHelper::getNetworkAvailableConstraints)
                    .thenCallRealMethod();

            fcmRegistrar.registerPW(tags);

            ArgumentCaptor<OneTimeWorkRequest> immediateCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<PeriodicWorkRequest> periodicCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);
            helperMock.verify(() -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    immediateCaptor.capture(), eq(FcmRegistrarWorker.TAG), any()));
            helperMock.verify(() -> PushwooshWorkManagerHelper.enqueuePeriodicUniqueWork(
                    periodicCaptor.capture(), eq(FcmRegistrarWorker.PERIODIC_WORK_NAME), any()));

            Data immediateInput = immediateCaptor.getValue().getWorkSpec().input;
            Data periodicInput = periodicCaptor.getValue().getWorkSpec().input;

            assertNotNull(immediateInput.getString(FcmRegistrarWorker.DATA_TAGS));
            assertTrue(immediateInput.getBoolean(FcmRegistrarWorker.DATA_REGISTER, false));
            assertNull(periodicInput.getString(FcmRegistrarWorker.DATA_TAGS));
            assertTrue(periodicInput.getBoolean(FcmRegistrarWorker.DATA_REGISTER, false));
        }
    }
}
