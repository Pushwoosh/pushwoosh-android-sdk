package com.pushwoosh.inline.internal.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.pushwoosh.inapp.InAppManager
import com.pushwoosh.inline.internal.InlineManager
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.tags.Tags
import com.pushwoosh.tags.TagsBundle

/**
 * Runs what a tapped banner element does. Every action but `close` is reported as a click first;
 * `close` only dismisses the banner. An action of a banner the slot no longer holds, an expired one
 * included, is not run. Links leave the banner in its slot. Main thread only.
 */
internal class InlineActionPerformer(
    private val context: Context,
    private val manager: InlineManager,
    private val postEvent: (event: String, attributes: TagsBundle?) -> Unit = { event, attributes ->
        InAppManager.getInstance().postEvent(event, attributes)
    },
) {

    /** [elementId] `null` = the banner itself. */
    fun perform(slotId: String, banner: InlineBanner, elementId: String?, action: InlineAction) {
        if (action == InlineAction.Close) {
            manager.dismiss(slotId, banner.bannerId)
            return
        }
        if (!manager.click(slotId, banner.bannerId, elementId)) return
        when (action) {
            is InlineAction.Url -> InlineLinkOpener.open(context, action.uri)
            is InlineAction.Deeplink -> InlineLinkOpener.open(context, action.uri)
            is InlineAction.PostEvent -> postEvent(action)
            is InlineAction.Copy -> copy(action)
            InlineAction.Close -> Unit
        }
    }

    private fun postEvent(action: InlineAction.PostEvent) {
        if (!manager.canPostEvent()) {
            PWLog.warn(TAG, "Inline banner postEvent '${action.event}' skipped: the user change is not confirmed yet")
            return
        }
        postEvent(action.event, action.attributes?.let { Tags.fromJson(it) })
    }

    private fun copy(action: InlineAction.Copy) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            PWLog.warn(TAG, "No clipboard, inline banner text is not copied")
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, action.text))
        Toast.makeText(context, action.toast ?: DEFAULT_TOAST, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "InlineActionPerformer"
        const val CLIP_LABEL = "Pushwoosh"
        const val DEFAULT_TOAST = "Copied"
    }
}
