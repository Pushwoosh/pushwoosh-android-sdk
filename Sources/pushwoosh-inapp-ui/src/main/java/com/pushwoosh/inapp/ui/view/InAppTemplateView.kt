package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pushwoosh.inapp.ui.model.InAppAction

/**
 * Base for every native in-app template view. Owns the action/close callback wiring and
 * the enter/exit animation contract; subclasses build their own content and call
 * [dispatchAction] / [requestClose].
 */
abstract class InAppTemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    interface Listener {
        fun onAction(action: InAppAction)
        fun onClose()
    }

    var listener: Listener? = null

    /** System bar + display-cutout insets, kept current so templates keep their controls
     *  and content clear of the status bar, navigation bar and notches on every device. */
    protected var systemInsets: Insets = Insets.NONE
        private set

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            onInsetsApplied(systemInsets)
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    /** Called whenever the system insets change; templates pad their controls/content here. */
    protected open fun onInsetsApplied(insets: Insets) {}

    /** Plays the entrance animation (the view is already attached). */
    abstract fun animateIn()

    /** Plays the exit animation, then invokes [onEnd] on the main thread. */
    abstract fun animateOut(onEnd: () -> Unit)

    protected fun dispatchAction(action: InAppAction) {
        when (action) {
            is InAppAction.Close -> listener?.onClose()
            is InAppAction.Url -> listener?.onAction(action)
        }
    }

    protected fun requestClose() {
        listener?.onClose()
    }
}
