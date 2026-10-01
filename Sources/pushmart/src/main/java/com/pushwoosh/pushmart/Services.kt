package com.pushwoosh.pushmart

import android.content.Context
import androidx.fragment.app.Fragment
import com.pushwoosh.pushmart.account.AccountManager
import com.pushwoosh.pushmart.account.AccountStore
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.core.SharedPrefsKeyValueStore
import com.pushwoosh.pushmart.sdk.InboxGateway
import com.pushwoosh.pushmart.sdk.LoggingInAppDelegate
import com.pushwoosh.pushmart.sdk.LoggingRichMediaDelegate
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.RealInboxGateway
import com.pushwoosh.pushmart.sdk.RealPushwooshGateway
import com.pushwoosh.pushmart.shop.CartStore
import com.pushwoosh.pushmart.shop.PreferencesStore
import com.pushwoosh.pushmart.shop.PriceAlertStore
import com.pushwoosh.pushmart.shop.PurchaseReporter
import com.pushwoosh.pushmart.shop.ShopEvents
import com.pushwoosh.pushmart.shop.WishlistStore

class Services(context: Context) {
    val prefs: KeyValueStore =
        SharedPrefsKeyValueStore(context.getSharedPreferences("pushmart", Context.MODE_PRIVATE))
    val gateway: PushwooshGateway = RealPushwooshGateway()
    val inbox: InboxGateway = RealInboxGateway()
    // PushwooshInAppUi.delegate is weak; these fields are the strong reference that keeps callbacks
    // alive.
    val inAppDelegate = LoggingInAppDelegate()
    val richMediaDelegate = LoggingRichMediaDelegate()
    val account = AccountStore(prefs)
    val accountManager = AccountManager(account, gateway)
    val cart = CartStore(prefs, gateway)
    val wishlist = WishlistStore(prefs, gateway)
    val priceAlerts = PriceAlertStore(prefs, gateway)
    val preferences = PreferencesStore(prefs, gateway)
    val shopEvents = ShopEvents(gateway)
    val purchaseReporter = PurchaseReporter(gateway)
}

val Context.services: Services
    get() = (applicationContext as PushMartApp).services

val Fragment.services: Services
    get() = requireContext().services
