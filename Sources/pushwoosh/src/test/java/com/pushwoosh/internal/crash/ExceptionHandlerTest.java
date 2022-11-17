package com.pushwoosh.internal.crash;

import android.content.Context;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.RepositoryTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.ref.WeakReference;

import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ExceptionHandlerTest {
    private ExceptionHandler exceptionHandler;
    private Thread testThread;

    private final Context context = RuntimeEnvironment.application;
    private final CrashManagerTestUtils utils = new CrashManagerTestUtils();

    private final Exception pushwooshException = new Exception();
    private final Exception notPushwooshException = utils.getNotPushwooshException();

    @Before
    public void setUp() throws Exception {
        testThread = new Thread();
        testThread.start();
        testThread.join();

        WeakReference<Context> weakContext = new WeakReference<>(context);
        exceptionHandler = new ExceptionHandler(testThread.getUncaughtExceptionHandler(), weakContext);

        Config configMock = MockConfig.createMock();
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        RegistrationPrefs registrationPrefs = RepositoryTestManager.createRegistrationPrefs(configMock, mock(DeviceRegistrar.class));
        RepositoryModule.setRegistrationPreferences(registrationPrefs);
    }

    @After
    public void tearDown() {
        utils.removeCrashReports(context);
    }

    @Test
    public void testPushwooshError() {
        Assert.assertTrue(exceptionHandler.isPushwooshError(RuntimeEnvironment.application, pushwooshException));
    }

    @Test
    public void testNotPushwooshError() {
        Assert.assertFalse(exceptionHandler.isPushwooshError(context, notPushwooshException));
    }

    @Test
    public void saveExceptionTest() throws Exception {
        exceptionHandler.saveException(pushwooshException);

        Assert.assertEquals(1, utils.getSavedCrashReportsNumber(context));
    }

    @Test
    public void testDefaultExceptionHandlerCaughtException() throws Exception {
        exceptionHandler.uncaughtException(testThread, pushwooshException);

        Assert.assertEquals(1, utils.getSavedCrashReportsNumber(context));
    }

    @Test
    public void testUncaughtExceptionNullContext() throws Exception {
        ExceptionHandler handler = new ExceptionHandler(testThread.getUncaughtExceptionHandler(), null);
        handler.uncaughtException(testThread, pushwooshException);

        Assert.assertEquals(0, utils.getSavedCrashReportsNumber(context));
    }
}
