package com.pushwoosh.inline

/**
 * Result of a manual inline slots request, delivered on the main thread.
 */
fun interface PushwooshInlineRequestCallback {
    /** Called once; [error] is `null` on success. */
    fun onResult(error: PushwooshInlineException?)
}
