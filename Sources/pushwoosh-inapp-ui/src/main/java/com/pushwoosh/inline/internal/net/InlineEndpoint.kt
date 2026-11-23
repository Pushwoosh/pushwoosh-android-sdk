package com.pushwoosh.inline.internal.net

import android.os.Bundle
import com.pushwoosh.repository.RepositoryModule

internal object InlineEndpoint {
    private const val META_INLINE_URL = "com.pushwoosh.inline_url"

    /** The manifest override with a trailing slash; blank or absent means no override. */
    fun overrideUrl(metaData: Bundle?): String? {
        val value = metaData?.getString(META_INLINE_URL)?.trim()
        if (value.isNullOrEmpty()) return null
        return if (value.endsWith("/")) value else "$value/"
    }

    /**
     * The root a request is sent against. Without an override the request also declares an api path,
     * and core swaps the `json/1.3/` tail of this root for it.
     */
    fun sendRoot(override: String?): String? = override ?: RepositoryModule.getRegistrationPreferences()?.baseUrl()?.get()
}
