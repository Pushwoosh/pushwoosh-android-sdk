package com.pushwoosh.inapp.ui.overlay

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.pushwoosh.PushwooshPlatform
import com.pushwoosh.inapp.ui.InAppModule
import com.pushwoosh.inapp.ui.NativeInAppAnalytics
import com.pushwoosh.inapp.ui.action.InAppActionDispatcher
import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.view.BannerInAppView
import com.pushwoosh.inapp.ui.view.InAppTemplateView
import com.pushwoosh.inapp.ui.view.ModalInAppView

/**
 * Presents non-blocking templates (banner, floating modal) by attaching the view directly
 * to the foreground Activity's decorView — no extra Activity, no focus theft, no overlay
 * permission. Driven by the FIFO queue through
 * [com.pushwoosh.inapp.ui.presentation.InAppOverlayChannel], so it shares the single
 * presentation slot with the Activity path (one native in-app on screen at a time).
 */
internal object InAppOverlayController {

    /** The single overlay currently on screen (this controller shows one at a time), or `null` when none.
     *  `@Volatile` so [isActive] can be read from any thread; mutations stay main-only. */
    @Volatile
    private var activeView: InAppTemplateView? = null
    private var activeMessage: InAppMessage? = null

    /** `true` while an overlay (banner / floating modal) is attached and on screen. */
    internal val isActive: Boolean
        get() = activeView != null

    /**
     * Closes the on-screen overlay through the normal animated dismiss (fires delegate
     * `didClose` and analytics `onClosed`); no-op when nothing is shown. Main-thread only.
     */
    internal fun dismissActive() {
        val view = activeView ?: return
        val message = activeMessage ?: return
        dismiss(view, message)
    }

    /**
     * Presents a non-blocking template by attaching its view to the current Activity's
     * decorView. The FIFO queue guarantees an empty slot before this is reached (one show at
     * a time across all templates), so there is no same-id guard and no eviction here.
     *
     * Returns `false` without side effects when there is no host Activity yet. Otherwise it
     * builds the view for the layout type (banner pinned top/bottom, or centered floating
     * modal), attaches it, and fires the present side effects in order: delegate
     * `willPresent`, animate in, delegate `didPresent`, analytics `onShown`. A banner with a
     * positive `autoDismissMs` schedules its own dismissal.
     *
     * Show policies (pause, frequency caps, delegate veto) are enforced by the queue before
     * this is reached, not here.
     *
     * @param message the non-blocking message (banner / floating modal) to present
     * @param onDismissed reports the eventual dismissal to the queue (the single source of
     *   `didClose` and the slot release). Threaded in from the channel and bound to the queue it
     *   already holds a strong reference to, so a close never depends on re-resolving the nullable
     *   application-context global from inside this controller (a null there would strand the slot).
     * @return `true` when the overlay was actually attached; `false` when there is no host
     *   Activity yet (a transient, retryable state)
     * @throws IllegalStateException if [message]'s layout is not an overlay template — a routing
     *   contract violation the channel logs and drops (never a `false`, which would be retried)
     */
    fun show(message: InAppMessage, onDismissed: (String?) -> Unit): Boolean {
        val activity = PushwooshPlatform.getInstance()?.topActivity ?: return false
        val root = activity.window?.decorView as? ViewGroup ?: return false

        val view: InAppTemplateView
        val height: Int
        val gravity: Int
        when (val layout = message.layout) {
            is InAppLayout.Banner -> {
                view = BannerInAppView(activity, layout.content)
                height = FrameLayout.LayoutParams.WRAP_CONTENT
                gravity = if (layout.content.position == BannerPosition.TOP) Gravity.TOP else Gravity.BOTTOM
            }
            is InAppLayout.Modal -> {
                view = ModalInAppView(activity, layout.content)
                height = FrameLayout.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
            }
            // Routing (InAppRoutingChannel.isNonBlocking) must only send layouts this when()
            // can build. A future non-blocking layout added there but not here is a contract
            // violation — throw so the channel logs and drops it. A bare `return false` here
            // would be misread as "no host Activity yet" and re-deferred onto the head of the
            // queue forever (see InAppOverlayChannel.present), wedging every later in-app.
            else -> error("InAppOverlayController cannot present ${message.layout}: routing sent a non-overlay layout to the overlay channel")
        }

        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {
                if (action is InAppAction.Url) {
                    InAppModule.delegate?.clickedAction(action.url, message.id)
                    NativeInAppAnalytics.onClicked(message.rawJson)
                    InAppActionDispatcher.open(activity, action)
                }
                dismiss(view, message)
            }

            override fun onClose() = dismiss(view, message)
        }

        // Detach is the single point where a close is finalized (analytics onClosed + queue
        // release). Every real close routes through it: a normal dismiss ends its exit animation
        // and removes the view (see dismiss()); a host Activity torn down mid-animation detaches
        // the view even though the animator may have abandoned its end callback; an idle-showing
        // overlay's Activity dies with no dismiss at all. Detachment always notifies attached
        // views, so the slot is released regardless of the animator — without this the slot would
        // stay claimed forever and block every later in-app. A view is attached once and detached
        // once, so this fires exactly once; a failed present unregisters it (see the catch below)
        // so an unconfirmed show stays silent.
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                if (activeView === view) {
                    activeView = null
                    activeMessage = null
                }
                NativeInAppAnalytics.onClosed(message.rawJson)
                onDismissed(message.id)
            }
        }
        view.addOnAttachStateChangeListener(attachListener)

        root.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height, gravity))
        activeView = view
        activeMessage = message
        try {
            InAppModule.delegate?.willPresent(message.id)
            view.animateIn()
            InAppModule.delegate?.didPresent(message.id)
            NativeInAppAnalytics.onShown(message.rawJson)
        } catch (e: Throwable) {
            // A throwing present side effect must not leave the view attached: the channel drops
            // the slot on throw, and advance() would then stack the next overlay over an orphan
            // no dismiss path can reach. Unregister the detach listener so the removeView below
            // stays silent (no onClosed/onDismissed for a show that never confirmed — the
            // channel releases the slot without didClose), clear activeView, detach, then rethrow.
            // Throwable, not Exception: an Error (TODO()/assert in a callback or view constructor)
            // must detach its orphan too; the rethrow hands it to the channel, which releases the
            // slot before BackgroundExecutor.main swallows it.
            view.removeOnAttachStateChangeListener(attachListener)
            activeView = null
            activeMessage = null
            (view.parent as? ViewGroup)?.removeView(view)
            throw e
        }

        if (view is BannerInAppView && view.autoDismissMs > 0) {
            view.postDelayed({ dismiss(view, message) }, view.autoDismissMs)
        }
        return true
    }

    /**
     * Starts the overlay's exit animation and removes the view when it ends. The removal detaches
     * the view, and the detach listener registered in [show] finalizes the close (analytics
     * `onClosed`, queue release, delegate `didClose` for a confirmed show) — so the terminal work
     * lives in one place regardless of how the view leaves the window.
     *
     * No-ops if [view] is no longer the active overlay (a stale dismissal). Clears [activeView]
     * synchronously so [isActive] flips at once and a second dismiss is rejected. The queue-slot
     * release does not depend on the exit animation completing: if the host Activity is torn down
     * mid-animation the view still detaches and the listener runs, so the slot is never stranded.
     */
    private fun dismiss(view: InAppTemplateView, message: InAppMessage) {
        if (activeView !== view) return
        activeView = null
        activeMessage = null
        view.animateOut { (view.parent as? ViewGroup)?.removeView(view) }
    }
}
