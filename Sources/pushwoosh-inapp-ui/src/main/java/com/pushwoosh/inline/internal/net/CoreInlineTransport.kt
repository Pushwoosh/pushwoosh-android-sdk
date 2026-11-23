package com.pushwoosh.inline.internal.net

import android.os.Bundle
import com.pushwoosh.inline.PushwooshInlineException
import com.pushwoosh.internal.network.NetworkException
import com.pushwoosh.internal.network.NetworkModule
import com.pushwoosh.internal.network.RequestBlockedException
import org.json.JSONObject

internal sealed class InlineFetchResult {
    class Success(val slots: JSONObject, val userId: String?, val appCode: String?) : InlineFetchResult()

    class Failure(val error: PushwooshInlineException) : InlineFetchResult()
}

internal interface InlineTransport {
    /** [callback] runs on the main thread. */
    fun fetch(slots: List<String>, callback: (InlineFetchResult) -> Unit)
}

internal fun mapFailure(e: NetworkException): PushwooshInlineException = when (e) {
    is RequestBlockedException ->
        PushwooshInlineException(PushwooshInlineException.ENDPOINT_UNAVAILABLE, e.message.orEmpty())
    else -> PushwooshInlineException(PushwooshInlineException.OTHER, e.message.orEmpty())
}

internal class CoreInlineTransport(private val metaData: () -> Bundle?) : InlineTransport {
    override fun fetch(slots: List<String>, callback: (InlineFetchResult) -> Unit) {
        val override = InlineEndpoint.overrideUrl(metaData())
        val request = GetInlineBannersRequest(slots, override)
        NetworkModule.getRequestManager().sendRequest(request, InlineEndpoint.sendRoot(override)) { result ->
            val data = result.data
            val error = result.exception
            callback(
                when {
                    error != null -> InlineFetchResult.Failure(mapFailure(error))
                    data == null -> InlineFetchResult.Failure(
                        PushwooshInlineException(PushwooshInlineException.INVALID_RESPONSE, "No slots in the answer"),
                    )
                    else -> InlineFetchResult.Success(data, request.sentUserId, request.sentAppCode)
                },
            )
        }
    }
}
