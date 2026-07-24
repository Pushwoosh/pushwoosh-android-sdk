package com.pushwoosh.inbox.internal.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.internal.utils.PWLog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Regression guard for crash-inboxperformaction-jsonobject-null-npe.
 *
 * InboxPerformActionStrategyFactory.onActionPerformed used to build
 * {@code new JSONObject(inboxMessageInternal.getActionParams())} with no null-guard, inside a
 * {@code try { ... } catch (JSONException e)}. On Android org.json {@code new JSONObject((String) null)}
 * throws NullPointerException -- not JSONException -- so a SERVICE inbox message whose payload omits
 * action_params (leaving actionParams null; getInboxType(null) -> PLAIN, no early return) crashed the
 * main Looper, uncaught, at :66. The fix guards the argument the same way the sibling getInboxType does
 * ({@code TextUtils.isEmpty}): a null/empty actionParams is a legitimate empty-params PLAIN message, so
 * an empty JSONObject is passed to the strategy -- graceful, no error logged. These tests assert that
 * behavior and pin it against the two other paths (malformed non-null is still swallowed-and-logged by
 * the JSONException catch; valid JSON is untouched).
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class InboxPerformActionNullActionParamsNpeTest {

    private static InboxMessageInternal messageWithActionParams(String actionParams) {
        InboxMessageInternal.Builder builder = new InboxMessageInternal.Builder()
                .setId("inbox-message-id")
                .setInboxMessageType(InboxMessageType.PLAIN);
        if (actionParams != null) {
            builder.setActionParams(actionParams);
        }
        return builder.build();
    }

    // Regression guard (was: assertThrows NPE at :66). A message with null actionParams (SERVICE push
    // with no action_params field) is now normalised to an empty JSONObject and handled gracefully:
    // it neither throws nor is logged as an error -- null actionParams is a legitimate empty-params
    // PLAIN message, not a parse failure. The absence of an error log is what separates the chosen
    // guard fix from the rejected "widen the catch to also catch NPE" alternative, which would have
    // swallowed-and-logged null as if it were invalid params.
    @Test
    public void onActionPerformed_nullActionParams_handledGracefullyWithoutError() {
        InboxMessageInternal message = messageWithActionParams(null);

        try (MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            InboxPerformActionStrategyFactory.onActionPerformed(message);

            pwLog.verify(() -> PWLog.error(anyString(), any(Throwable.class)), never());
        }
    }

    // Barrier control: the SAME crash point with a malformed-but-non-null actionParams throws
    // JSONException, which the local catch(JSONException) DOES swallow -- so onActionPerformed returns
    // normally AND logs the failure. This pins the fix on the exception-TYPE gap the null case dodged:
    // the guard handles null (no error logged, above), the pre-existing catch keeps handling
    // genuinely-invalid params (error logged, here) -- the symmetric opposite of the null path.
    @Test
    public void onActionPerformed_malformedActionParams_isSwallowedAndLogged() {
        InboxMessageInternal message = messageWithActionParams("{not-valid-json");

        try (MockedStatic<PWLog> pwLog = mockStatic(PWLog.class)) {
            InboxPerformActionStrategyFactory.onActionPerformed(message);

            pwLog.verify(() -> PWLog.error(anyString(), any(Throwable.class)), times(1));
        }
    }

    // Non-vacuity control: a valid (empty) JSON object keeps type PLAIN and reaches PlainActionStrategy,
    // whose performAction is a no-op -- no throw. The happy path is untouched.
    @Test
    public void onActionPerformed_validActionParams_doesNotThrow() {
        InboxMessageInternal message = messageWithActionParams("{}");

        InboxPerformActionStrategyFactory.onActionPerformed(message);
    }
}
