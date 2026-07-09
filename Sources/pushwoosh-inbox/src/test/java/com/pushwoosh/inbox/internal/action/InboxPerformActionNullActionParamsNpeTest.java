package com.pushwoosh.inbox.internal.action;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Reproduces crash-inboxperformaction-jsonobject-null-npe.
 *
 * InboxPerformActionStrategyFactory.onActionPerformed (InboxPerformActionStrategyFactory.java:66)
 * builds {@code new JSONObject(inboxMessageInternal.getActionParams())} with no null-guard on the
 * argument, inside a {@code try { ... } catch (JSONException e)} (:65-69). On Android org.json
 * (which Robolectric uses) {@code new JSONObject((String) null)} throws NullPointerException -- not
 * JSONException -- because JSONTokener stores the null string and nextCleanInternal() dereferences
 * {@code in.length()}. The NPE slips past the JSONException-only catch. A SERVICE inbox message whose
 * payload omits action_params leaves actionParams null; getInboxType(null) -> PLAIN still walks into
 * :66 (no early return for PLAIN). In production this NPE is uncaught because the callback runs on the
 * main Looper via a raw Handler (InboxRepository.java:92/:185), bypassing BackgroundExecutor.main's
 * catch(Throwable).
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class InboxPerformActionNullActionParamsNpeTest {

    private static boolean stackReachesOnActionPerformed(Throwable t) {
        for (StackTraceElement frame : t.getStackTrace()) {
            if (frame.getClassName().equals("com.pushwoosh.inbox.internal.action.InboxPerformActionStrategyFactory")
                    && frame.getMethodName().equals("onActionPerformed")) {
                return true;
            }
        }
        return false;
    }

    private static InboxMessageInternal messageWithActionParams(String actionParams) {
        InboxMessageInternal.Builder builder = new InboxMessageInternal.Builder()
                .setId("inbox-message-id")
                .setInboxMessageType(InboxMessageType.PLAIN);
        if (actionParams != null) {
            builder.setActionParams(actionParams);
        }
        return builder.build();
    }

    // Point of defect: a message with null actionParams (SERVICE push with no action_params field)
    // makes new JSONObject((String) null) throw NPE at :66, escaping the JSONException catch.
    @Test
    public void onActionPerformed_nullActionParams_throwsNpeAtCrashPoint() {
        InboxMessageInternal message = messageWithActionParams(null);

        NullPointerException npe = assertThrows(
                NullPointerException.class, () -> InboxPerformActionStrategyFactory.onActionPerformed(message));

        assertTrue(
                "NPE must originate in InboxPerformActionStrategyFactory.onActionPerformed "
                        + "(new JSONObject on a null actionParams string)",
                stackReachesOnActionPerformed(npe));
    }

    // Barrier control: the SAME crash point with a malformed-but-non-null actionParams throws
    // JSONException, which the local catch(JSONException):67 DOES swallow -- so onActionPerformed
    // returns normally. This pins the defect on the exception-TYPE mismatch (NPE vs JSONException),
    // not on reachability: the guard that exists catches exactly the type the null case dodges.
    @Test
    public void onActionPerformed_malformedActionParams_isSwallowedByCatch() {
        InboxMessageInternal message = messageWithActionParams("{not-valid-json");

        InboxPerformActionStrategyFactory.onActionPerformed(message);
    }

    // Non-vacuity control: a valid (empty) JSON object keeps type PLAIN and reaches PlainActionStrategy,
    // whose performAction is a no-op -- no throw. The necessary condition removed (actionParams is a
    // parseable string) => the happy path is untouched; only null triggers the crash.
    @Test
    public void onActionPerformed_validActionParams_doesNotThrow() {
        InboxMessageInternal message = messageWithActionParams("{}");

        InboxPerformActionStrategyFactory.onActionPerformed(message);
    }
}
