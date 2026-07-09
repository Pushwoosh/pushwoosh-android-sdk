package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collection;

// Regression guard for crash-action-classforname-error-payload: a push whose pw_actions[].class
// names a host class that exists but fails to load (throwing <clinit>) makes Action.java:45
// Class.forName (one-arg, initialize=true) throw an ExceptionInInitializerError — a subtype of
// LinkageError/Error, not Exception. Before the fix it escaped Action's catch (which only caught
// ClassNotFoundException) and PushBundleDataProvider.getActions' catch(JSONException), crashing the
// messaging thread. The fix widened Action's catch to ClassNotFoundException | LinkageError, so the
// load-time Error is now swallowed exactly like the missing-class path: the action is kept with a
// null actionClass and nothing escapes.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class ActionClassForNameErrorCrashTest {

    private static final String THROWING_CLINIT_CLASS = "com.pushwoosh.test.manifest.ThrowingClinitActionClass";

    // The fixture's <clinit> is triggered exactly once per classloader (an ExceptionInInitializerError
    // fires only on the first init attempt; later re-resolves throw NoClassDefFoundError), so keep
    // exactly one method that initializes it in this file. This differs from the negative control
    // below by using a class that EXISTS but fails to load — the only way its actionClass ends up null
    // without a throw is Action's LinkageError catch, so the assertion is non-vacuous.
    @Test
    public void getActions_throwingClinitActionClass_swallowedNoError() {
        Bundle bundle = new Bundle();
        bundle.putString(
                "pw_actions",
                "[{\"type\":\"ACTIVITY\",\"title\":\"Open\",\"class\":\"" + THROWING_CLINIT_CLASS + "\"}]");

        Collection<Action> actions = PushBundleDataProvider.getActions(bundle);

        assertEquals(1, actions.size());
        assertNull(actions.iterator().next().getActionClass());
    }

    // Negative control: an action class that does NOT resolve throws ClassNotFoundException — an
    // Exception, which Action's catch(:46) swallows — so getActions completes and the action is kept
    // with a null actionClass. Proves the repro above measures the Error-vs-Exception escape, not
    // ambient failure of getActions.
    @Test
    public void getActions_unknownActionClass_swallowedNoError() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_actions", "[{\"type\":\"ACTIVITY\",\"title\":\"Open\",\"class\":\"com.does.not.Exist\"}]");

        Collection<Action> actions = PushBundleDataProvider.getActions(bundle);

        assertEquals(1, actions.size());
        assertNull(actions.iterator().next().getActionClass());
    }
}
