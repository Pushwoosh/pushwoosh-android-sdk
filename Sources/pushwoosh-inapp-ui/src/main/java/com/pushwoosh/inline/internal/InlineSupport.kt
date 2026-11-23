package com.pushwoosh.inline.internal

import android.os.Handler
import android.os.Looper
import com.pushwoosh.repository.RepositoryModule

internal interface InlineClock {
    fun nowMillis(): Long
}

internal object SystemInlineClock : InlineClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

internal fun interface Cancelable {
    fun cancel()
}

/** Runs delayed work on the main thread. */
internal interface InlineScheduler {
    fun postDelayed(delayMs: Long, task: () -> Unit): Cancelable
}

internal object MainInlineScheduler : InlineScheduler {
    private val handler = Handler(Looper.getMainLooper())

    override fun postDelayed(delayMs: Long, task: () -> Unit): Cancelable {
        val runnable = Runnable { task() }
        handler.postDelayed(runnable, delayMs.coerceAtLeast(0))
        return Cancelable { handler.removeCallbacks(runnable) }
    }
}

/** The user and application the SDK works for right now. */
internal interface InlineIdentity {
    /** `null` or empty = no user id set. */
    fun userId(): String?
    fun hwid(): String

    /** `null` or empty = the app code is not known yet. */
    fun appCode(): String?
}

/** `userId-or-hwid|appCode`; `null` while the app code, or both the user id and the hwid, are not known. */
internal fun inlineOwner(userId: String?, hwid: String?, appCode: String?): String? {
    val app = appCode?.takeIf { it.isNotEmpty() } ?: return null
    val user = userId?.takeIf { it.isNotEmpty() } ?: hwid?.takeIf { it.isNotEmpty() } ?: return null
    return "$user|$app"
}

/** The owner the SDK works for right now; see [inlineOwner]. */
internal fun InlineIdentity.owner(): String? = inlineOwner(userId(), hwid(), appCode())

internal object CoreInlineIdentity : InlineIdentity {
    override fun userId(): String? = RepositoryModule.getRegistrationPreferences()?.userId()?.get()

    override fun hwid(): String = RepositoryModule.getRegistrationPreferences()?.hwid()?.get().orEmpty()

    override fun appCode(): String? = RepositoryModule.getRegistrationPreferences()?.applicationId()?.get()
}
