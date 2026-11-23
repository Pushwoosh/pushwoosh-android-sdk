package com.pushwoosh.inline

import android.os.Handler
import android.os.Looper
import com.pushwoosh.inline.internal.InlineModule
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/**
 * Inline slots: places in the app's own screens that show banners chosen by the server.
 *
 * Put a `PushwooshInlineView` with a slot id on screen and the SDK requests, draws and reports its
 * banner by itself. Apps that draw banners themselves read them with [getBanner], report them with
 * [trackShown], [trackClick] and [dismiss], and follow changes through [listener].
 *
 * Every method may be called from any thread. The work and all callbacks run on the main thread;
 * methods without a result return at once.
 */
object PushwooshInline {
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Receives slot updates, shows, clicks and dismissals on the main thread. The SDK holds the
     * listener strongly: set it to `null` when it should no longer be called.
     */
    @JvmStatic
    @Volatile
    var listener: PushwooshInlineListener? = null

    /** The theme inline banners are drawn in; [PushwooshInlineColorScheme.APP] by default. */
    @JvmStatic
    @Volatile
    var colorScheme: PushwooshInlineColorScheme = PushwooshInlineColorScheme.APP
        set(value) {
            if (field == value) return
            field = value
            onMain { InlineModule.colorSchemeListeners.toList().forEach { it() } }
        }

    /**
     * Requests the banners of up to 10 slots now. Empty ids and duplicates are dropped. Manual
     * requests are limited to 5 per session, then one per 3 minutes; the slots keep their banners
     * when a request fails. [callback] gets `null` on success, else a [PushwooshInlineException]. A `null`
     * list or a `null` id fails the call with [PushwooshInlineException.INVALID_ARGUMENT].
     */
    @JvmStatic
    fun requestSlots(slotIds: List<String?>?, callback: PushwooshInlineRequestCallback?) {
        val ids = slotIds?.toList()
        onMain {
            val manager = InlineModule.managerOrNull()
            if (manager == null) {
                callback?.onResult(PushwooshInlineException(PushwooshInlineException.OTHER, "Pushwoosh is not initialized"))
            } else {
                manager.requestSlots(ids, callback)
            }
        }
    }

    /**
     * The banner [slotId] holds now, or `null` when it is empty. Reading a banner does not count
     * as showing it: call [trackShown] once it is on screen. Called off the main thread, it waits
     * for the main thread.
     */
    @JvmStatic
    fun getBanner(slotId: String): PushwooshInlineBanner? {
        if (Looper.myLooper() == Looper.getMainLooper()) return InlineModule.managerOrNull()?.banner(slotId)
        val task = FutureTask { InlineModule.managerOrNull()?.banner(slotId) }
        mainHandler.post(task)
        return try {
            task.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    /** Reports that the app showed [bannerId] of [slotId] itself; counted once per banner version. */
    @JvmStatic
    fun trackShown(slotId: String, bannerId: String) {
        onMain { InlineModule.managerOrNull()?.trackShown(slotId, bannerId) }
    }

    /** Reports a tap on [bannerId] of [slotId]; [elementId] is the tapped element, `null` for the banner itself. */
    @JvmStatic
    fun trackClick(slotId: String, bannerId: String, elementId: String?) {
        onMain { InlineModule.managerOrNull()?.click(slotId, bannerId, elementId) }
    }

    /** Closes [bannerId] in [slotId]: the slot becomes empty at once and later gets its next banner. */
    @JvmStatic
    fun dismiss(slotId: String, bannerId: String) {
        onMain { InlineModule.managerOrNull()?.dismiss(slotId, bannerId) }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
}
