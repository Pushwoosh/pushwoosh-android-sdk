/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.repository;

import static com.pushwoosh.internal.utils.MockConfig.APP_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class UpdateRegistrationTest {
    private static final String PUSH_TOKEN = "test_pushToken";
    private Config config;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() throws Exception {
        config = MockConfig.createMock();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    // Tests updateRegistration method with testRegId !=null, forceRegister == true, fresh lastPushRegistration
    @Test
    public void updateRegistrationTest() throws Exception {
        platformTestManager = new PlatformTestManager(config);
        FakeRequestManager fake = platformTestManager.getRequestManager();
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();

        registrationPrefs.pushToken().set(PUSH_TOKEN);
        registrationPrefs.forceRegister().set(true);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 10);

        // Steps:
        platformTestManager.onApplicationCreated();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationOpenEvent());

        // Postconditions:
        fake.awaitCount("registerDevice", 1);
        JSONObject params = fake.last("registerDevice").params;

        assertThat(params.getString("application"), is(equalTo(APP_ID)));
        assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
    }

    @Test
    public void registerDeviceIfRegisteredBefore_changeAppIdTest() throws JSONException {
        String newAppId = APP_ID + "new";

        // init
        initForRegistration();
        FakeRequestManager fake = platformTestManager.getRequestManager();

        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationOpenEvent());

        // Steps
        SdkStateProvider.getInstance().executeOrQueue(() -> {
            platformTestManager.getNotificationManager().setAppId(newAppId);
        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Postconditions:
        fake.awaitCount("registerDevice", 2);
        JSONObject params = fake.last("registerDevice").params;

        assertThat(params.getString("application"), is(equalTo(newAppId)));
        assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
    }

    private void initForRegistration() {
        platformTestManager = new PlatformTestManager(config);
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();

        registrationPrefs.pushToken().set(PUSH_TOKEN);
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.forceRegister().set(true);
        platformTestManager.onApplicationCreated();
    }

    @Test
    public void unregistrationMethodInvoke_changeAppIdTest() throws JSONException {
        String newAppId = APP_ID + "new";

        // init
        initForRegistration();
        FakeRequestManager fake = platformTestManager.getRequestManager();

        // Steps
        platformTestManager.getNotificationManager().setAppId(newAppId);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Postconditions:
        fake.awaitCount("unregisterDevice", 1);
        JSONObject params = fake.last("unregisterDevice").params;

        assertThat(params.getString("application"), is(equalTo(APP_ID)));
    }

    @Test
    public void unregistrationMethodNotInvokedIfNotRegistered_changeAppIdTest() throws JSONException {
        String newAppId = APP_ID + "new";

        // init
        initForRegistration();
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();
        registrationPrefs.registeredOnServer().set(false);
        FakeRequestManager fake = platformTestManager.getRequestManager();

        // Steps
        platformTestManager.getNotificationManager().setAppId(newAppId);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Postconditions:
        assertEquals(0, fake.count("unregisterDevice"));
    }

    // Tests updateRegistration method with testToken == null, forceRegister == true, outdated lastPushRegistration
    @Test
    public void nullRegIdUpdateRegistrationTest() throws Exception {
        platformTestManager = new PlatformTestManager(config);
        FakeRequestManager fake = platformTestManager.getRequestManager();
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();

        // save same appId for correct behavior
        registrationPrefs.applicationId().set(APP_ID);
        registrationPrefs.pushToken().set(null);
        registrationPrefs.forceRegister().set(true);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 1000000);

        // Steps:
        platformTestManager.setUp();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Postconditions:
        assertEquals(0, fake.count("registerDevice"));
    }

    // Tests updateRegistration method with testRegId != null, forceRegister == false, fresh lastPushRegistration
    @Test
    public void falseForceRegisterUpdateRegistrationTest() throws Exception {
        platformTestManager = new PlatformTestManager(config);
        FakeRequestManager fake = platformTestManager.getRequestManager();
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();

        registrationPrefs.applicationId().set(APP_ID);
        registrationPrefs.pushToken().set(PUSH_TOKEN);
        registrationPrefs.forceRegister().set(false);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 10);

        // Steps:
        platformTestManager.setUp();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Postconditions:
        assertEquals(0, fake.count("registerDevice"));
    }

    // Tests updateRegistration method with testRegId != null, forceRegister == false, outdated lastPushRegistration
    @Test
    public void outdatedLastPushRegistrationUpdateRegistrationTest() throws Exception {
        platformTestManager = new PlatformTestManager(config);
        FakeRequestManager fake = platformTestManager.getRequestManager();
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();

        registrationPrefs.pushToken().set(PUSH_TOKEN);
        registrationPrefs.forceRegister().set(false);
        registrationPrefs.lastPushRegistration().set(System.currentTimeMillis() - 1000000);

        // Steps:
        platformTestManager.onApplicationCreated();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationOpenEvent());

        // Postconditions:
        fake.awaitCount("registerDevice", 1);
        JSONObject params = fake.last("registerDevice").params;

        assertThat(params.getString("application"), is(equalTo(APP_ID)));
        assertThat(params.getString("push_token"), is(equalTo(PUSH_TOKEN)));
    }
}
