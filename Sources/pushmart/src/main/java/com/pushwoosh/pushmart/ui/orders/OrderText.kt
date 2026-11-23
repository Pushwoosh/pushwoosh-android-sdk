package com.pushwoosh.pushmart.ui.orders

import android.content.Context
import android.text.format.DateFormat
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.shop.CartStore
import com.pushwoosh.pushmart.shop.Order
import com.pushwoosh.pushmart.ui.common.ResultBus
import java.text.DateFormat as JavaDateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Order copy shared by the Orders tab and the order details, worded as on iOS. */
object OrderText {
    fun items(context: Context, n: Int): String = context.getString(R.string.order_items, n)

    fun orders(context: Context, n: Int): String = context.getString(R.string.orders_count, n)

    /** The first product's name, "<name> + N more" for several, "" when none is in the catalog. */
    fun summary(context: Context, order: Order): String {
        val products = order.products
        val first = products.firstOrNull() ?: return ""
        return if (products.size == 1) first.name
        else context.getString(R.string.order_summary_more, first.name, products.size - 1)
    }

    /** Month and day in the device locale, e.g. "Nov 2". */
    fun shortDate(millis: Long): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "MMMd")
        return SimpleDateFormat(pattern, locale).format(Date(millis))
    }

    fun longDateTime(millis: Long): String =
        JavaDateFormat.getDateTimeInstance(JavaDateFormat.LONG, JavaDateFormat.SHORT)
            .format(Date(millis))

    fun mediumDateTime(millis: Long): String =
        JavaDateFormat.getDateTimeInstance(JavaDateFormat.MEDIUM, JavaDateFormat.SHORT)
            .format(Date(millis))

    /**
     * Puts the order's products that are still in the catalog back in [cart] and reports it;
     * [CartStore.add] posts ProductAdd for each product not in the bag yet.
     */
    fun buyAgain(context: Context, order: Order, cart: CartStore) {
        val products = order.products
        if (products.isEmpty()) {
            ResultBus.fail(
                context.getString(R.string.order_unavailable_title),
                context.getString(R.string.order_unavailable_message),
            )
            return
        }
        products.forEach { cart.add(it.id) }
        ResultBus.success(
            context.getString(R.string.order_added_title), items(context, products.size))
    }
}
