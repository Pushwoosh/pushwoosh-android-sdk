package com.pushwoosh.internal.crash;

import android.content.Context;
import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.RepositoryTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.pushwoosh.internal.crash.CrashConfig.API_TOKEN;
import static com.pushwoosh.internal.crash.CrashConfig.API_TOKEN_HEADER;
import static com.pushwoosh.internal.crash.CrashManager.CRASH_ANALYTICS_VERSION_KEY;
import static com.pushwoosh.internal.crash.CrashManager.PREFERENCES_NAME;
import static com.pushwoosh.internal.crash.CrashManager.STACK_TRACES_FOUND_CONFIRMED;
import static com.pushwoosh.internal.crash.CrashManager.STACK_TRACES_FOUND_NEW;
import static com.pushwoosh.internal.crash.CrashManager.STACK_TRACES_FOUND_NONE;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class CrashManagerTest {
    private final Context context = RuntimeEnvironment.application;
    private final WeakReference<Context> weakContext = new WeakReference<>(context);

    private final CrashManagerTestUtils utils = new CrashManagerTestUtils();
    private final Exception pushwooshException = new Exception();

    private CrashManager crashManager = new CrashManager(context);
    private Thread testThread;

    @Before
    public void setUp() throws Exception {
        testThread = new Thread();
        testThread.start();
        testThread.join();

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
    public void testAnalyticsIsUpdated() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(CRASH_ANALYTICS_VERSION_KEY, 1);
        editor.apply();

        Assert.assertFalse(crashManager.isCrashAnalyticsUpdated());

        editor.putInt(CRASH_ANALYTICS_VERSION_KEY, 0);
        editor.apply();

        Assert.assertTrue(crashManager.isCrashAnalyticsUpdated());

        crashManager = new CrashManager(null);
        Assert.assertFalse(crashManager.isCrashAnalyticsUpdated());
    }

    @Test
    public void migrateCrashReportsTest() throws Exception {
        // saving crash report
        ExceptionHandler exceptionHandler = new ExceptionHandler(testThread.getUncaughtExceptionHandler(), weakContext);
        exceptionHandler.saveException(pushwooshException);

        // migrating
        Assert.assertTrue(crashManager.migrateCrashReports());
        // checking saved crash report was removed after migration
        Assert.assertEquals(0, utils.getSavedCrashReportsNumber(context));
        // should return true even there is no crash reports
        Assert.assertTrue(crashManager.migrateCrashReports());

        crashManager = new CrashManager(null);
        // should return false as context is null
        Assert.assertFalse(crashManager.migrateCrashReports());
    }

    @Test
    public void searchForStackTracesTest() {
        String[] stackTraces = crashManager.searchForStackTraces();
        // expecting no stack traces
        Assert.assertEquals(0, stackTraces.length);

        // saving crash report
        ExceptionHandler exceptionHandler = new ExceptionHandler(Mockito.mock(Thread.UncaughtExceptionHandler.class), weakContext);
        exceptionHandler.saveException(pushwooshException);

        stackTraces = crashManager.searchForStackTraces();
        // expecting to find 1 stack trace
        Assert.assertEquals(1, stackTraces.length);

        // null context
        crashManager = new CrashManager(null);
        stackTraces = crashManager.searchForStackTraces();
        Assert.assertNull(stackTraces);
    }

    @Test
    public void hasStackTracesTest() {
        // STACK_TRACES_FOUND_NONE
        Assert.assertEquals(STACK_TRACES_FOUND_NONE, crashManager.hasStackTraces());

        // save crash report
        ExceptionHandler exceptionHandler = new ExceptionHandler(testThread.getUncaughtExceptionHandler(), weakContext);
        exceptionHandler.saveException(pushwooshException);
        // STACK_TRACES_FOUND_NEW
        Assert.assertEquals(STACK_TRACES_FOUND_NEW, crashManager.hasStackTraces());

        String[] stackTraces = crashManager.searchForStackTraces();
        crashManager.saveConfirmedStackTraces(stackTraces);

        // STACK_TRACES_FOUND_CONFIRMED
        Assert.assertEquals(STACK_TRACES_FOUND_CONFIRMED, crashManager.hasStackTraces());
    }

    @Test
    public void deleteStackTraceTest() throws Exception {
        // save crash report
        ExceptionHandler exceptionHandler = new ExceptionHandler(testThread.getUncaughtExceptionHandler(), weakContext);
        exceptionHandler.saveException(pushwooshException);

        String[] fileNames = crashManager.searchForStackTraces();
        crashManager.deleteStackTrace(fileNames[0]);

        Assert.assertEquals(0, utils.getSavedCrashReportsNumber(context));
    }

    @Test
    public void registerHandlerTest() throws Exception {
        Thread testThread = new Thread();
        testThread.start();
        testThread.join();

        Thread.UncaughtExceptionHandler handler = testThread.getUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);

        crashManager.registerHandler();

        Thread.UncaughtExceptionHandler actual = Thread.getDefaultUncaughtExceptionHandler();
        Assert.assertNotEquals(handler, actual);
    }

    @Test
    public void migrateCrashAnalyticsTest() {
        int previousVersion = 0;
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(CRASH_ANALYTICS_VERSION_KEY, previousVersion);
        editor.apply();

        crashManager.migrateCrashAnalytics();

        int actualVersion = preferences.getInt(CRASH_ANALYTICS_VERSION_KEY, 0);
        Assert.assertNotEquals(previousVersion, actualVersion);
    }

    @Test
    public void contentsOfFileTest() {
        String testString = "test string";

        String filename = UUID.randomUUID().toString();
        File file = new File(FileProvider.getCrashDir(context), filename + FileProvider.getPushwooshStacktraceSuffix());
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(testString);
            writer.flush();
        } catch (IOException ioException) {
            PWLog.error("Failed to save exception:\n" + ioException.getMessage());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e1) {
                PWLog.error("Error saving crash report!", e1);
            }
        }
        Assert.assertEquals(
                testString + System.getProperty("line.separator"),
                crashManager.contentsOfFile(file.getName())
        );

        // check with null context
        crashManager = new CrashManager(null);
        crashManager.contentsOfFile(file.getName());

        Assert.assertEquals("", crashManager.contentsOfFile(file.getName()));
    }

    @Test
    public void submitStackTraceTest() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse());

        // save crash report
        ExceptionHandler exceptionHandler = new ExceptionHandler(testThread.getUncaughtExceptionHandler(), weakContext);
        exceptionHandler.saveException(pushwooshException);

        String[] fileNames = crashManager.searchForStackTraces();
        String url = mockWebServer.url("").toString();
        crashManager.submitStackTrace(fileNames[0], url);

        RecordedRequest request = mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS);

        // check request header
        Assert.assertEquals(API_TOKEN, request.getHeaders().get(API_TOKEN_HEADER));

        JSONObject testRequestBody = CrashReport.createCrashReport(
                pushwooshException,
                CrashConfig.appCode(),
                CrashConfig.hwid(),
                CrashConfig.framework(),
                CrashConfig.isCollectingDeviceModelAllowed(),
                CrashConfig.isCollectingDeviceOsVersionAllowed()
        );
        JSONObject testData = testRequestBody.getJSONObject("data");
        JSONObject testCustomData = testData.getJSONObject("custom");

        // check body
        JSONObject actualRequestBody = new JSONObject(request.getBody().readUtf8());
        JSONObject actualData = actualRequestBody.getJSONObject("data");
        JSONObject actualCustomData = actualData.getJSONObject("custom");
        Assert.assertEquals(testData.getString("environment"), actualData.getString("environment"));
        // framework must be mocked before comparison
        // Assert.assertEquals(testData.getString("framework"), actualData.getString("framework"));
        Assert.assertEquals(testData.getString("level"), actualData.getString("level"));
        Assert.assertEquals(testData.getString("code_version"), actualData.getString("code_version"));
        Assert.assertEquals(testData.getString("language"), actualData.getString("language"));
        Assert.assertEquals(testData.getString("platform"), actualData.getString("platform"));

        Assert.assertEquals(testCustomData.getString("application"), actualCustomData.getString("application"));
        Assert.assertEquals(testCustomData.getString("hwid"), actualCustomData.getString("hwid"));

        Assert.assertEquals(0, utils.getSavedCrashReportsNumber(context));
    }
}
