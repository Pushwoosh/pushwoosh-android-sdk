package com.pushwoosh.pushmart.sdk

import com.pushwoosh.exception.PushwooshException
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.richmedia.RichMedia
import com.pushwoosh.richmedia.RichMediaPresentingDelegate

/** Logs every rich media callback and republishes present/error on [RichMediaSignals]. */
class LoggingRichMediaDelegate : RichMediaPresentingDelegate {
    override fun shouldPresent(richMedia: RichMedia?): Boolean {
        PWLog.debug(TAG, "shouldPresent ${richMedia.code()}")
        return true
    }

    override fun onPresent(richMedia: RichMedia?) {
        PWLog.debug(TAG, "onPresent ${richMedia.code()}")
        RichMediaSignals.publish(RichMediaSignals.Signal.Presented(richMedia.code()))
    }

    override fun onError(richMedia: RichMedia?, pushwooshException: PushwooshException?) {
        val error = pushwooshException?.message ?: "unknown error"
        PWLog.warn(TAG, "onError ${richMedia.code()}: $error")
        RichMediaSignals.publish(RichMediaSignals.Signal.Failed(richMedia.code(), error))
    }

    override fun onClose(richMedia: RichMedia?) {
        PWLog.debug(TAG, "onClose ${richMedia.code()}")
    }

    private fun RichMedia?.code(): String = this?.content ?: "?"

    companion object {
        const val TAG = "PushMartRichMedia"
    }
}
