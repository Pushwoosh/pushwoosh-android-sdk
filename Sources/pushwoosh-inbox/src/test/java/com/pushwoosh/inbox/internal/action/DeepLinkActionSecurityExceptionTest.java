package com.pushwoosh.inbox.internal.action;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Regression guard for crash-deeplinkaction-startactivity-securityexception.
 *
 * DeepLinkActionStrategy.performAction wraps context.startActivity(intent) in a try/catch. It used to
 * catch ONLY ActivityNotFoundException, so any other RuntimeException from startActivity -- realistically
 * a SecurityException when the deep-link target is permission-protected / not-exported (belongs to another
 * app or the system) -- slipped past that narrow catch, then past the dispatcher's equally narrow
 * catch (JSONException) (InboxPerformActionStrategyFactory.java:67), and escaped onActionPerformed. In
 * production that escaped throw was uncaught because the callback runs on the main Looper via a raw Handler
 * (InboxRepository.java:92/:185), bypassing BackgroundExecutor.main's catch(Throwable) -> app crash.
 *
 * The fix widens the catch to catch (Exception e), mirroring the sibling UrlActionStrategy (which already
 * uses catch (Exception e) + a resolveActivity gate): a SecurityException is now swallowed and logged, and
 * onActionPerformed returns normally. These tests assert that graceful handling and pin down that the
 * originally-handled ActivityNotFoundException path was not lost.
 *
 * Stand-in: the strategy reads its context field from AndroidPlatformModule.getApplicationContext() at
 * construction, and onActionPerformed builds the strategy internally (no injection seam). We install a
 * ContextWrapper whose startActivity(Intent) throws a chosen RuntimeException directly into the platform
 * module's context field via reflection -- this bypasses AndroidPlatformModule.init(), which would blow up
 * on the (Application) cast in ApplicationOpenDetector when handed a plain wrapper. This substitutes a
 * deterministic throw for the runtime-dependent "protected target on the user's device".
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class DeepLinkActionSecurityExceptionTest {

    @After
    public void tearDown() throws Exception {
        installApplicationContext(null);
    }

    private static void installApplicationContext(Context ctx) throws Exception {
        Field field = AndroidPlatformModule.class.getDeclaredField("context");
        field.setAccessible(true);
        field.set(AndroidPlatformModule.getInstance(), ctx == null ? null : new WeakReference<>(ctx));
    }

    private static Context startActivityThrowing(Supplier<RuntimeException> factory) {
        return new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public void startActivity(Intent intent) {
                throw factory.get();
            }
        };
    }

    private static InboxMessageInternal deepLinkMessage() {
        // getInboxType routes on actionParams["l"]: a non-http URL => DEEP_LINK (InboxPayloadDataProvider).
        return new InboxMessageInternal.Builder()
                .setId("inbox-deeplink-id")
                .setInboxMessageType(InboxMessageType.DEEP_LINK)
                .setActionParams("{\"l\":\"pwsample://deeplink/protected\"}")
                .build();
    }

    // Regression guard: a DEEP_LINK message whose target throws SecurityException (protected / not-exported
    // activity) makes context.startActivity(intent) throw at the crash point. The widened catch (Exception)
    // in performAction now swallows it (logged), so onActionPerformed returns normally instead of letting
    // the SecurityException escape both narrow catches and crash the app.
    @Test
    public void onActionPerformed_deepLinkSecurityException_isSwallowedGracefully() throws Exception {
        AtomicInteger startActivityCalls = new AtomicInteger();
        installApplicationContext(startActivityThrowing(() -> {
            startActivityCalls.incrementAndGet();
            return new SecurityException("Permission Denial: starting Intent { act=android.intent.action.VIEW "
                    + "dat=pwsample://deeplink/protected } not exported from uid 10123");
        }));

        // No throw escapes onActionPerformed: the widened catch swallows the SecurityException.
        InboxPerformActionStrategyFactory.onActionPerformed(deepLinkMessage());

        // Non-vacuity: startActivity was actually reached and did throw the SecurityException the guard now
        // catches -- the no-throw above is the guard swallowing a real throw, not startActivity never running.
        assertEquals(1, startActivityCalls.get());
    }

    // ActivityNotFoundException control: the originally-handled type (the exact type the old catch was
    // written for) is still swallowed after the catch was widened -> onActionPerformed returns normally.
    // Guards against the fix accidentally dropping the ANFE handling it inherited.
    // (ActivityNotFoundException extends RuntimeException, so the throwing-context stand-in is valid.)
    @Test
    public void onActionPerformed_deepLinkActivityNotFound_isSwallowedGracefully() throws Exception {
        AtomicInteger startActivityCalls = new AtomicInteger();
        installApplicationContext(startActivityThrowing(() -> {
            startActivityCalls.incrementAndGet();
            return new android.content.ActivityNotFoundException("No Activity found to handle Intent");
        }));

        InboxPerformActionStrategyFactory.onActionPerformed(deepLinkMessage());

        // Non-vacuity: startActivity was actually reached and threw the ANFE the widened catch still handles,
        // not swallowed by an earlier guard before the try/catch was ever entered.
        assertEquals(1, startActivityCalls.get());
    }

    // Non-vacuity control: with a non-throwing context, startActivity is reached and succeeds, and
    // onActionPerformed completes -- no throw. The necessary condition removed (target does not throw) =>
    // the happy path is untouched; only a throw from startActivity exercises the catch.
    @Test
    public void onActionPerformed_deepLinkNonThrowingContext_startsActivityAndCompletes() throws Exception {
        AtomicInteger startActivityCalls = new AtomicInteger();
        installApplicationContext(new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public void startActivity(Intent intent) {
                startActivityCalls.incrementAndGet();
            }
        });

        InboxPerformActionStrategyFactory.onActionPerformed(deepLinkMessage());

        // Non-vacuity: routing reached DeepLinkActionStrategy and startActivity was invoked exactly once on
        // the happy path -- the "no throw" is a real success, not the message being dropped before :60.
        assertEquals(1, startActivityCalls.get());
    }
}
