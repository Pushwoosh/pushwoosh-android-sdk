package com.pushwoosh.inline

/**
 * Why an inline slots request failed; [code] is one of the constants of the companion object.
 */
class PushwooshInlineException(val code: Int, message: String) : Exception(message) {
    companion object {
        /** A transport failure; the message carries the transport error. */
        const val OTHER = 0

        /** No slot ids left after dropping empty ones and duplicates, or more than 10 slots in one request. */
        const val INVALID_ARGUMENT = 1

        /** No inline banners endpoint can be resolved. */
        const val ENDPOINT_UNAVAILABLE = 2

        /** Manual requests are rate limited; the cached banners stay. */
        const val RATE_LIMITED = 3

        /** The server answered without a `slots` object. */
        const val INVALID_RESPONSE = 4

        /** A user change is not confirmed yet, or the user or app changed while the request was in flight. */
        const val OWNER_CHANGED = 5
    }
}
