package com.pushwoosh.pushmart.live

import android.content.Context
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Product

/**
 * Keep your picks close, as iOS `StoryReelLiveActivityController`: up to [LIMIT] wishlist products
 * as separate ongoing notifications `picks-<id>`, each opening its product. They are plain ongoing
 * notifications, never promoted: they are not an ongoing activity.
 */
class WishlistPins(context: Context, private val notifier: LiveNotifier) {
    private val context = context.applicationContext

    val isAvailable: Boolean
        get() = notifier.isAvailable

    val isRunning: Boolean
        get() = notifier.activeTags(PREFIX).isNotEmpty()

    /** Pins the first [LIMIT] of [products], through the Push Primer when needed. */
    fun pin(products: List<Product>, onDone: () -> Unit) {
        notifier.whenAllowed { granted ->
            if (granted) products.take(LIMIT).forEach { show(it) }
            onDone()
        }
    }

    fun unpinAll() {
        notifier.activeTags(PREFIX).forEach { notifier.cancel(it) }
    }

    private fun show(product: Product) {
        val open = notifier.openProduct(product.id)
        notifier.show(
            PREFIX + product.id,
            LiveContent(
                product.name,
                context.getString(R.string.picks_summary, product.priceText),
                open,
                largeIcon = notifier.photo(product.imageName),
                action = context.getString(R.string.picks_view_item) to open,
            ),
        )
    }

    companion object {
        const val PREFIX = "picks-"
        const val LIMIT = 3
    }
}
