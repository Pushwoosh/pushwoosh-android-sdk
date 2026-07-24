package com.pushwoosh.inapp.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import com.pushwoosh.inapp.ui.InAppModule
import com.pushwoosh.inapp.ui.NativeInAppAnalytics
import com.pushwoosh.inapp.ui.action.InAppActionDispatcher
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.parser.InAppConfigParser
import com.pushwoosh.inapp.ui.view.InAppTemplateView
import com.pushwoosh.inapp.ui.view.InAppViewFactory

/**
 * Shared translucent Activity hosting a blocking native in-app template. The message is
 * handed in as raw JSON and re-parsed here, so the hand-off survives process death and
 * configuration changes. Dismissal (close / action / back) advances the queue via
 * [InAppModule].
 *
 * A recreate (config change outside the manifest's configChanges list, or process death)
 * must not replay queue/analytics side effects: the dying instance would fire
 * onDismissed/onClosed and the new one didPresent/onShown, double-advancing the queue —
 * hence the [isChangingConfigurations] guard in [onDestroy] and the savedInstanceState
 * guard in [onCreate].
 */
class InAppOverlayActivity : Activity() {

    private var messageId: String? = null
    private var rawJson: String? = null
    private var templateView: InAppTemplateView? = null

    /** Latches the one-time dismissal so [onDestroy] reports it to the queue/analytics at most once. */
    private var notifiedDismiss = false

    /**
     * Builds and shows the template, then fires the one-time "presented" side effects.
     *
     * Re-parses the raw JSON handed in via the intent and asks [InAppViewFactory] to build
     * the template view, finishing early (which releases the queue slot via [onDestroy]) if
     * either step fails. The queue confirmation (`onPresentConfirmed`), delegate
     * `didPresent` and analytics `onShown` run only on first creation
     * (`savedInstanceState == null`), so a config-change recreate does not replay them.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = InAppConfigParser.parse(intent.getStringExtra(EXTRA_RAW_JSON))
        if (message == null) {
            finish()
            return
        }
        messageId = message.id
        rawJson = message.rawJson

        val view = InAppViewFactory.create(this, message.layout, object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {
                if (action is InAppAction.Url) {
                    InAppModule.delegate?.clickedAction(action.url, messageId)
                    NativeInAppAnalytics.onClicked(rawJson)
                    InAppActionDispatcher.open(this@InAppOverlayActivity, action)
                }
                close()
            }

            override fun onClose() = close()
        })
        if (view == null) {
            finish()
            return
        }

        templateView = view
        current = this
        // Capture the weakly-held delegate once so willPresent and didPresent reach the same
        // instance — the host dropping its strong ref mid-present cannot orphan willPresent.
        val delegate = InAppModule.delegate
        if (savedInstanceState == null) {
            // Emit willPresent only once the view is really built (iOS parity); a failed
            // launch fires nothing. Paired with didPresent below.
            delegate?.willPresent(messageId)
        }
        setContentView(view, ViewGroup.LayoutParams(MATCH, MATCH))
        view.animateIn()
        if (savedInstanceState == null) {
            InAppModule.queueManager(applicationContext).onPresentConfirmed()
            delegate?.didPresent(messageId)
            NativeInAppAnalytics.onShown(rawJson)
        }
    }

    /**
     * Animates the template out and finishes; the dismissal side effects (queue advance,
     * analytics) run in [onDestroy], not here. Reached from a close tap, an action tap, or
     * back press.
     */
    private fun close() {
        val view = templateView
        if (view == null) {
            finish()
            return
        }
        view.animateOut { finish() }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() = close()

    /** Programmatic dismissal from [com.pushwoosh.inapp.ui.PushwooshInAppUi.dismiss]; runs the
     *  normal animated close path. */
    internal fun dismissFromApi() = close()

    /** Finishes with no system transition — the template view runs its own exit animation. */
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    /**
     * Fires the one-time "dismissed" side effects: advances the queue via [InAppModule] and
     * reports analytics `onClosed`.
     *
     * Skipped while [isChangingConfigurations], because a config-change recreate is not a
     * real close and the Activity will come back — firing here would double-advance the
     * queue. The [notifiedDismiss] latch guarantees the dismissal is reported at most once.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isChangingConfigurations) return
        if (current === this) current = null
        if (!notifiedDismiss) {
            notifiedDismiss = true
            InAppModule.queueManager(applicationContext).onDismissed(messageId)
            // Send the close event only if the view was actually shown; a launch whose view
            // never built (parse / view-build fail) must not fire an unpaired onClosed.
            if (templateView != null) NativeInAppAnalytics.onClosed(rawJson)
        }
    }

    companion object {
        private const val EXTRA_RAW_JSON = "pw_inapp_raw_json"
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

        /**
         * The overlay Activity currently on screen, or `null`. Read by
         * [com.pushwoosh.inapp.ui.PushwooshInAppUi.isPresenting]; set on a successful build and
         * cleared on a real destroy (not on a config-change recreate). `@Volatile` so the getter
         * can read it from any thread.
         */
        @Volatile
        internal var current: InAppOverlayActivity? = null

        /**
         * Builds the launch intent, carrying the message as raw JSON (re-parsed in [onCreate]).
         *
         * [Intent.FLAG_ACTIVITY_NEW_TASK] is required because the presentation channel
         * launches from the application context, not an Activity context.
         */
        fun intent(context: Context, rawJson: String): Intent =
            Intent(context, InAppOverlayActivity::class.java)
                .putExtra(EXTRA_RAW_JSON, rawJson)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
