package com.pushwoosh;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by etkachenko on 3/30/17.
 */

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushwooshSettingsTest {
	private PlatformTestManager platformTestManager;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		platformTestManager.tearDown();
	}

	//
	// ApplicationId part
	//-----------------------------------------------------------------------

	@Test
	public void setMetaAppIDTest() throws Exception {
		//Preconditions:
		String appIDTest = "Test_AppID";
		String appIDTestMeta = "Test_AppID_Meta";

		Config config = MockConfig.createMock(appIDTestMeta);

		//Steps:
		platformTestManager = new PlatformTestManager(config);
		platformTestManager.setUp();
		platformTestManager.getNotificationManager().setAppId(appIDTest);

		//Postconditions:
		RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();
		assertThat(registrationPrefs.applicationId().get(), is(appIDTest));
	}

	//Tests appID value from setAppId method set in registrationPrefs when AndroidManifest AppId value is not presented
	@Test
	public void setAppIDTest() throws Exception {
		//Preconditions:
		String appIDTest = "Test_AppID";
		String appIDTestMeta = null;

		Config config = MockConfig.createMock(appIDTestMeta);

		//Steps:
		platformTestManager = new PlatformTestManager(config);
		platformTestManager.setUp();
		platformTestManager.getNotificationManager().setAppId(appIDTest);

		//Postconditions:
		RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();
		assertThat(registrationPrefs.applicationId().get(), is(appIDTest));
	}


	//Tests application throws IllegalArgumentException and empty string set in registrationPrefs as AppID value
	//when AndroidManifest AppId = "" and setAppID method called with empty string value
	@Test(expected = IllegalArgumentException.class)
	public void setEmptyAppIDTest() throws Exception {
		//Preconditions:
		String appIDTest = "";
		String appIDTestMeta = null;

		Config config = MockConfig.createMock(appIDTestMeta);

		//Steps:
		platformTestManager = new PlatformTestManager(config);
		platformTestManager.setUp();
		platformTestManager.getNotificationManager().setAppId(appIDTest);
	}

}
