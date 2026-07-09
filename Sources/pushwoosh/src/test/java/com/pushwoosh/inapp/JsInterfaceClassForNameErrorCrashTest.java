package com.pushwoosh.inapp;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Map;

// Regression guard for crash-getjavascriptinterfaces-classforname-error: a host that registers a
// JS-interface class by name via InAppManager.registerJavascriptInterface(className, name), where the
// class exists but fails to load (throwing <clinit>), makes PushwooshInAppImpl.getJavascriptInterfaces()
// call the one-arg Class.forName (initialize=true), which throws an ExceptionInInitializerError — a
// subtype of Error, not Exception. Before the fix that Error escaped the catch(Exception) and crashed
// the unguarded WebClient ctor (WebClient.java:94) on the async MODAL rich-media display path; the catch
// was widened to catch(Throwable), so the broken interface is now logged and skipped, exactly the
// graceful degradation the missing-class (ClassNotFoundException) path already gives.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class JsInterfaceClassForNameErrorCrashTest {

    private static final String THROWING_CLINIT_CLASS = "com.pushwoosh.test.inapp.ThrowingClinitJsInterface";

    private PlatformTestManager platformTestManager;
    private PushwooshInAppImpl pushwooshInApp;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        pushwooshInApp = new PushwooshInAppImpl(mock(PushwooshInAppService.class), serverCommunicationManager);
    }

    @After
    public void tearDown() {
        platformTestManager.tearDown();
    }

    // The sole trigger of the throwing-clinit fixture: an ExceptionInInitializerError fires only on
    // the first init attempt per classloader, so keep exactly one initializing test in this file.
    @Test
    public void getJavascriptInterfaces_throwingClinitClass_swallowedNoError() {
        pushwooshInApp.registerJavascriptInterface(THROWING_CLINIT_CLASS, "jsi");

        Map<String, Object> result = pushwooshInApp.getJavascriptInterfaces();

        assertFalse(
                "a registered class whose <clinit> throws an Error must be caught by catch(Throwable) and skipped, not escape getJavascriptInterfaces",
                result.containsKey("jsi"));
    }

    // Negative control: a class name that does NOT resolve throws ClassNotFoundException — an
    // Exception, which the catch(:212) swallows — so getJavascriptInterfaces returns normally with the
    // unresolved name simply absent from the map. Proves the repro above measures the
    // Error-vs-Exception escape, not ambient failure of getJavascriptInterfaces.
    @Test
    public void getJavascriptInterfaces_unknownClass_swallowedNoError() {
        pushwooshInApp.registerJavascriptInterface("com.does.not.Exist", "jsi");

        Map<String, Object> result = pushwooshInApp.getJavascriptInterfaces();

        assertFalse("unresolved class name must not be added to the interface map", result.containsKey("jsi"));
    }
}
