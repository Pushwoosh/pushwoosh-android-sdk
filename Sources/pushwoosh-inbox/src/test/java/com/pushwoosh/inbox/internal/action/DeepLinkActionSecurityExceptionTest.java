package com.pushwoosh.inbox.internal.action;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
import java.util.function.Supplier;

/**
 * Reproduces crash-deeplinkaction-startactivity-securityexception.
 *
 * DeepLinkActionStrategy.performAction wraps context.startActivity(intent)
 * (DeepLinkActionStrategy.java:60) in a try { ... } catch (ActivityNotFoundException e) (:61) that
 * catches ONLY ActivityNotFoundException. Any other RuntimeException from startActivity -- realistically
 * a SecurityException when the deep-link target is permission-protected / not-exported (belongs to another
 * app or the system) -- slips past that narrow catch, then past the dispatcher's equally narrow
 * catch (JSONException) (InboxPerformActionStrategyFactory.java:67), and escapes onActionPerformed. In
 * production this escaped throw is uncaught because the callback runs on the main Looper via a raw Handler
 * (InboxRepository.java:92/:185), bypassing BackgroundExecutor.main's catch(Throwable).
 *
 * Sibling asymmetry (signal): UrlActionStrategy gates on intent.resolveActivity(pm) != null AND catches
 * catch (Exception e) -- so the same SecurityException would be swallowed there. DeepLinkActionStrategy
 * does neither, so the exact same throw escapes.
 *
 * Stand-in: the strategy reads its context field from AndroidPlatformModule.getApplicationContext() at
 * construction, and onActionPerformed builds the strategy internally (no injection seam). We install a
 * ContextWrapper whose startActivity(Intent) throws a chosen RuntimeException directly into the platform
 * module's context field via reflection -- this bypasses AndroidPlatformModule.init(), which would blow up
 * on the (Application) cast in ApplicationOpenDetector when handed a plain wrapper. This substitutes a
 * deterministic throw for the runtime-dependent "protected target on the user's device"; the outcome
 * (a non-ANFE RuntimeException escaping the narrow catches) is faithful, the mechanism of how a real target
 * comes to throw is the stand-in.
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

    // The exception is built INSIDE startActivity (via the supplier) so its stack trace, frozen by
    // fillInStackTrace at construction, captures the real throw path (startActivity <- performAction <-
    // onActionPerformed) -- not the test method where a pre-built exception would have been created.
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

    private static boolean stackReaches(Throwable t, String className, String methodName) {
        for (StackTraceElement frame : t.getStackTrace()) {
            if (frame.getClassName().equals(className) && frame.getMethodName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    // Point of defect: a DEEP_LINK message whose target throws SecurityException (protected / not-exported
    // activity) makes context.startActivity(intent) throw at :60. SecurityException is not an
    // ActivityNotFoundException, so it escapes catch(ANFE):61 in the strategy AND catch(JSONException):67 in
    // the dispatcher, escaping onActionPerformed entirely.
    @Test
    public void onActionPerformed_deepLinkSecurityException_escapesNarrowCatches() throws Exception {
        installApplicationContext(startActivityThrowing(
                () -> new SecurityException("Permission Denial: starting Intent { act=android.intent.action.VIEW "
                        + "dat=pwsample://deeplink/protected } not exported from uid 10123")));

        SecurityException e = assertThrows(
                SecurityException.class, () -> InboxPerformActionStrategyFactory.onActionPerformed(deepLinkMessage()));

        assertTrue(
                "SecurityException must originate at DeepLinkActionStrategy.performAction "
                        + "(context.startActivity on the deep-link intent)",
                stackReaches(e, "com.pushwoosh.inbox.internal.action.DeepLinkActionStrategy", "performAction"));
        assertTrue(
                "and must propagate out through InboxPerformActionStrategyFactory.onActionPerformed "
                        + "(neither narrow catch stops it)",
                stackReaches(
                        e,
                        "com.pushwoosh.inbox.internal.action.InboxPerformActionStrategyFactory",
                        "onActionPerformed"));
    }

    // Barrier control: the SAME crash point with an ActivityNotFoundException (which IS what the narrow
    // catch:61 is written for) is swallowed -> onActionPerformed returns normally. This pins the defect on
    // the exception-TYPE narrowness: the guard that exists catches exactly the type the SecurityException
    // dodges. (ActivityNotFoundException extends RuntimeException, so the throwing-context stand-in is valid.)
    @Test
    public void onActionPerformed_deepLinkActivityNotFound_isSwallowedByCatch() throws Exception {
        installApplicationContext(startActivityThrowing(
                () -> new android.content.ActivityNotFoundException("No Activity found to handle Intent")));

        InboxPerformActionStrategyFactory.onActionPerformed(deepLinkMessage());
    }

    // Non-vacuity control: with a non-throwing context, startActivity succeeds (Robolectric records it) and
    // onActionPerformed completes -- no throw. The necessary condition removed (target does not throw) =>
    // the happy path is untouched; only a non-ANFE throw from startActivity triggers the crash.
    @Test
    public void onActionPerformed_deepLinkNonThrowingContext_doesNotThrow() throws Exception {
        installApplicationContext(RuntimeEnvironment.getApplication());

        InboxPerformActionStrategyFactory.onActionPerformed(deepLinkMessage());
    }
}
