package com.pushwoosh.pushmart

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.fragment.app.Fragment
import com.pushwoosh.pushmart.account.AccountManager
import com.pushwoosh.pushmart.account.AccountStore
import com.pushwoosh.pushmart.account.AvatarStore
import com.pushwoosh.pushmart.core.ForegroundAlerts
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.core.LauncherIcon
import com.pushwoosh.pushmart.core.LocalReminders
import com.pushwoosh.pushmart.core.SharedPrefsKeyValueStore
import com.pushwoosh.pushmart.core.ThemeStore
import com.pushwoosh.pushmart.live.DropReminders
import com.pushwoosh.pushmart.live.LiveNotifier
import com.pushwoosh.pushmart.live.LiveOrders
import com.pushwoosh.pushmart.live.WishlistPins
import com.pushwoosh.pushmart.sdk.InboxGateway
import com.pushwoosh.pushmart.sdk.LoggingInAppDelegate
import com.pushwoosh.pushmart.sdk.LoggingRichMediaDelegate
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.RealInboxGateway
import com.pushwoosh.pushmart.sdk.RealPushwooshGateway
import com.pushwoosh.pushmart.shop.CartStore
import com.pushwoosh.pushmart.shop.OrderStore
import com.pushwoosh.pushmart.shop.PreferencesStore
import com.pushwoosh.pushmart.shop.PriceAlertStore
import com.pushwoosh.pushmart.shop.PurchaseReporter
import com.pushwoosh.pushmart.shop.RecentlyViewedStore
import com.pushwoosh.pushmart.shop.ShopEvents
import com.pushwoosh.pushmart.shop.WishlistStore
import com.pushwoosh.pushmart.ui.common.PushPrimer
import java.io.File

class Services(context: Context) {
    val prefs: KeyValueStore =
        SharedPrefsKeyValueStore(context.getSharedPreferences("pushmart", Context.MODE_PRIVATE))
    val gateway: PushwooshGateway = RealPushwooshGateway()
    val inbox: InboxGateway = RealInboxGateway()
    // PushwooshInAppUi.delegate is weak; these fields are the strong reference that keeps callbacks
    // alive.
    val inAppDelegate = LoggingInAppDelegate()
    val richMediaDelegate = LoggingRichMediaDelegate()
    val theme = ThemeStore(prefs)
    val foregroundAlerts = ForegroundAlerts(prefs, gateway)
    val account = AccountStore(prefs)
    val avatar = AvatarStore(File(context.filesDir, "pushmart_avatar.jpg"))
    val pushPermission = PushPrimer(context, prefs)
    val accountManager =
        AccountManager(account, gateway, inbox, pushPermission) { context.hasInternet() }
    val reminders = LocalReminders(gateway, pushPermission, accountManager)
    val liveNotifier = LiveNotifier(context, pushPermission, accountManager)
    val orders = OrderStore(prefs)
    val liveOrders = LiveOrders(context, prefs, liveNotifier, orders)
    val drops = DropReminders(context, prefs, liveNotifier)
    val wishlistPins = WishlistPins(context, liveNotifier)
    val cart = CartStore(prefs, gateway)
    val wishlist = WishlistStore(prefs, gateway)
    val priceAlerts = PriceAlertStore(prefs, gateway)
    val preferences = PreferencesStore(prefs, gateway)
    val shopEvents = ShopEvents(gateway)
    val purchaseReporter = PurchaseReporter(prefs, gateway, orders)
    val recentlyViewed = RecentlyViewedStore(prefs)
    val launcherIcon = LauncherIcon(context)
}

private fun Context.hasInternet(): Boolean {
    val connectivity = getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities =
        connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

val Context.services: Services
    get() = (applicationContext as PushMartApp).services

val Fragment.services: Services
    get() = requireContext().services
