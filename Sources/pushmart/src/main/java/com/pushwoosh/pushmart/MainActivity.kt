package com.pushwoosh.pushmart

import android.content.Intent
import android.graphics.Outline
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.pushwoosh.Pushwoosh
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.core.DebugLaunch
import com.pushwoosh.pushmart.databinding.ActivityMainBinding
import com.pushwoosh.pushmart.routing.DeepLinkRouter
import com.pushwoosh.pushmart.routing.PushRouting
import com.pushwoosh.pushmart.routing.Route
import com.pushwoosh.pushmart.routing.RouteExtras
import com.pushwoosh.pushmart.sdk.Registration
import com.pushwoosh.pushmart.ui.common.ResultBus
import com.pushwoosh.pushmart.ui.common.setPushMartGraph
import com.pushwoosh.pushmart.ui.promo.VoucherFragment
import com.pushwoosh.pushmart.ui.shop.CartSheet
import com.pushwoosh.pushmart.ui.shop.ProductDetailSheet

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val handler = Handler(Looper.getMainLooper())
    private val hideResult = Runnable { binding.resultOverlay.resultRoot.visibility = View.GONE }
    private var inboxCounts: Registration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        services.pushPermission.attach(this)

        if (savedInstanceState == null) DebugLaunch.apply(services, intent?.extras)
        val host = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = host.navController
        navController.setPushMartGraph(services.account.onboarded)
        binding.navView.setupWithNavController(navController)
        binding.navView.outlineProvider = OpaqueBackgroundOutline
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.visibility =
                if (destination.id in TAB_DESTINATIONS) View.VISIBLE else View.GONE
            // applyInsets() keys on the destination, so the new screen needs a fresh pass.
            ViewCompat.requestApplyInsets(binding.root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets -> applyInsets(insets) }
        inboxCounts = services.inbox.observeCounts { _, unread -> renderInboxBadge(unread) }

        binding.resultOverlay.resultRoot.setOnClickListener { hideResultNow() }
        ResultBus.events.observe(this) { event ->
            if (event != null) {
                showResult(event)
                ResultBus.events.value = null
            }
        }

        // The intent survives recreation and Recents relaunches; route it once.
        if (savedInstanceState == null && !intent.launchedFromHistory()) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun Intent?.launchedFromHistory(): Boolean =
        this != null && (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0

    private fun handleIntent(intent: Intent?) {
        val route = routeFrom(intent) ?: return
        PWLog.debug(TAG, "routing: $route")
        open(route)
    }

    private fun routeFrom(intent: Intent?): Route? {
        val launch = Pushwoosh.getInstance().launchNotification
        if (launch != null) Pushwoosh.getInstance().clearLaunchNotification()
        RouteExtras.get(intent)?.let {
            RouteExtras.clear(intent)
            return it
        }
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                DeepLinkRouter.fromUri(uri.toString())?.let {
                    return it
                }
            }
        }
        return PushRouting.routeFor(launch)
    }

    fun open(route: Route) {
        if (!services.account.onboarded) {
            PWLog.debug(TAG, "route dropped before onboarding: $route")
            return
        }
        val product =
            when (route) {
                is Route.Product -> Catalog.byId(route.id) ?: return
                is Route.AddToBag -> Catalog.byId(route.id) ?: return
                else -> null
            }
        closeOverlays()
        val singleTop = NavOptions.Builder().setLaunchSingleTop(true).build()
        when (route) {
            is Route.Product -> product?.let { ProductDetailSheet.show(supportFragmentManager, it) }
            Route.Sale -> navController.navigate(R.id.saleFragment, null, singleTop)
            is Route.Voucher ->
                navController.navigate(
                    R.id.voucherFragment,
                    bundleOf(VoucherFragment.ARG_CODE to route.code),
                    singleTop)
            Route.Cart -> CartSheet.show(supportFragmentManager)
            is Route.AddToBag -> {
                services.cart.add(route.id)
                CartSheet.show(supportFragmentManager)
            }
        }
    }

    /**
     * Closes every sheet and the Sale or Voucher screen, as iOS `present` dismisses what is up, so
     * the route's screen opens once, even for the product whose sheet is open. Sheets shown by the
     * tabs live in the nav host's child fragment manager, sheets shown by routes in this one.
     */
    private fun closeOverlays() {
        val host = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        for (fm in listOf(supportFragmentManager, host.childFragmentManager)) {
            if (fm.isStateSaved) continue
            fm.fragments.filterIsInstance<DialogFragment>().forEach { it.dismissNow() }
        }
        if (navController.currentDestination?.id in FULL_BLEED_DESTINATIONS) {
            navController.popBackStack()
        }
    }

    // Content pads only what nobody below takes: full-bleed screens pad themselves.
    // The keyboard branch stands in for adjustResize, which stops resizing under edge-to-edge.
    private fun applyInsets(insets: WindowInsetsCompat): WindowInsetsCompat {
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val screen = navController.currentDestination?.id
        val fullBleed = screen in FULL_BLEED_DESTINATIONS
        val top = if (fullBleed) 0 else bars.top
        val bottom =
            when {
                ime.bottom > 0 -> ime.bottom
                fullBleed -> 0
                else -> bars.bottom
            }
        binding.content.setPadding(0, top, 0, bottom)
        return insets.inset(0, top, 0, bottom)
    }

    private fun renderInboxBadge(unread: Int) {
        val badge = binding.navView.getOrCreateBadge(R.id.offersFragment)
        badge.backgroundColor = getColor(R.color.pm_coral)
        badge.badgeTextColor = getColor(R.color.pm_white)
        badge.setTextAppearance(R.style.TextAppearance_PushMart_Badge)
        badge.maxCharacterCount = 2
        badge.number = unread
        badge.isVisible = unread > 0
    }

    private fun showResult(event: ResultBus.Event) {
        val overlay = binding.resultOverlay
        overlay.resultIcon.setImageResource(
            if (event.success) R.drawable.ic_check_circle else R.drawable.ic_cancel)
        overlay.resultIcon.setBackgroundResource(
            if (event.success) R.drawable.bg_circle_success_16 else R.drawable.bg_circle_danger_16)
        overlay.resultIcon.imageTintList =
            getColorStateList(if (event.success) R.color.pm_success else R.color.pm_danger)
        overlay.resultTitle.text = event.title
        overlay.resultMessage.text = event.message
        overlay.resultMessage.visibility = if (event.message.isEmpty()) View.GONE else View.VISIBLE
        overlay.resultRoot.visibility = View.VISIBLE
        handler.removeCallbacks(hideResult)
        handler.postDelayed(hideResult, RESULT_DURATION_MS)
    }

    private fun hideResultNow() {
        handler.removeCallbacks(hideResult)
        hideResult.run()
    }

    override fun onDestroy() {
        handler.removeCallbacks(hideResult)
        inboxCounts?.unregister()
        inboxCounts = null
        services.pushPermission.detach(this)
        super.onDestroy()
    }

    // bg_tab_bar stroke and fill alphas differ: its own outline gets alpha 0 and casts no shadow.
    private object OpaqueBackgroundOutline : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            view.background?.getOutline(outline)
            outline.alpha = 1f
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val RESULT_DURATION_MS = 1800L
        val TAB_DESTINATIONS =
            setOf(
                R.id.homeFragment,
                R.id.shopFragment,
                R.id.ordersFragment,
                R.id.offersFragment,
                R.id.profileFragment)
        val FULL_BLEED_DESTINATIONS = setOf(R.id.saleFragment, R.id.voucherFragment)
    }
}
