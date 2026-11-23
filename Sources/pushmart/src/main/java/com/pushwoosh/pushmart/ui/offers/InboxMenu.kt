package com.pushwoosh.pushmart.ui.offers

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.pushwoosh.pushmart.databinding.ViewInboxMenuBinding

/** The More menu of the Inbox header: a card anchored on the button, like the iOS context menu. */
object InboxMenu {
    fun show(
        anchor: View,
        actions: OffersActions,
        onRefreshed: (syncFailed: Boolean) -> Unit,
    ): PopupWindow {
        val binding = ViewInboxMenuBinding.inflate(LayoutInflater.from(anchor.context))
        val card = binding.root
        card.clipToOutline = true
        card.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popup = PopupWindow(card, card.measuredWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.isOutsideTouchable = true
        popup.elevation = ELEVATION_DP * anchor.resources.displayMetrics.density
        fun row(view: View, action: () -> Unit) {
            view.setOnClickListener {
                popup.dismiss()
                action()
            }
        }
        row(binding.refresh) { actions.refresh(onRefreshed) }
        row(binding.markAllRead, actions::markAllRead)
        row(binding.clearRead, actions::clearRead)
        row(binding.deleteAll, actions::deleteAll)
        popup.showAsDropDown(anchor, 0, -anchor.height, Gravity.END)
        return popup
    }

    private const val ELEVATION_DP = 12f
}
