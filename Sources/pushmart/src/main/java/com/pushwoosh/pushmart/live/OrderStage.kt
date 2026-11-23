package com.pushwoosh.pushmart.live

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.pushwoosh.pushmart.R

/** The four delivery phases of an order, as the iOS widget's `DeliveryStage`. */
enum class OrderStage(@DrawableRes val icon: Int, @StringRes val label: Int) {
    CREATED(R.drawable.ic_tab_shop_filled, R.string.live_stage_created),
    PACKED(R.drawable.ic_tab_orders_filled, R.string.live_stage_packed),
    ON_THE_WAY(R.drawable.ic_local_shipping, R.string.live_stage_on_the_way),
    DELIVERED(R.drawable.ic_tab_home_filled, R.string.live_stage_delivered);

    /** This phase's segment of the order bar: coral at Created to tangerine at Delivered. */
    @ColorInt
    fun color(context: Context): Int =
        ColorUtils.blendARGB(
            ContextCompat.getColor(context, R.color.pm_coral),
            ContextCompat.getColor(context, R.color.pm_tangerine),
            ordinal / (values().size - 1f))

    companion object {
        private val keywords =
            listOf(
                DELIVERED to listOf("delivered", "arriv", "door", "here", "complete"),
                ON_THE_WAY to
                    listOf(
                        "way",
                        "courier",
                        "out for",
                        "ship",
                        "transit",
                        "route",
                        "driv",
                        "dispatch"),
                PACKED to listOf("pack", "prepar", "ready", "warehouse", "picking"),
                CREATED to listOf("placed", "confirm", "received", "order"),
            )

        /**
         * The phase a status line names, as iOS `DeliveryStage.from`; unknown or none is Packed.
         */
        fun from(status: String?): OrderStage {
            val text = status?.lowercase() ?: return PACKED
            return keywords.firstOrNull { (_, keys) -> keys.any { it in text } }?.first ?: PACKED
        }
    }
}

/**
 * PushMart's order bar: four coral-to-tangerine phases, a point at each boundary, the phase icon.
 */
object OrderProgressStyle {
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun build(context: Context, stage: OrderStage): Notification.ProgressStyle {
        val ink = ContextCompat.getColor(context, R.color.pm_text_primary)
        val style = Notification.ProgressStyle()
        for (phase in OrderStage.values()) {
            style.addProgressSegment(
                Notification.ProgressStyle.Segment(1).setColor(phase.color(context)))
        }
        for (point in 1 until OrderStage.values().size) {
            style.addProgressPoint(Notification.ProgressStyle.Point(point).setColor(ink))
        }
        style.setProgress(stage.ordinal + 1)
        style.setProgressTrackerIcon(
            Icon.createWithResource(context, stage.icon).setTint(stage.color(context)))
        return style
    }
}
