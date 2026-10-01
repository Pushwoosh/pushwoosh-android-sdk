package com.pushwoosh.pushmart

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.pushwoosh.Pushwoosh
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.core.LaunchArgs
import com.pushwoosh.pushmart.databinding.ActivityMainBinding
import com.pushwoosh.pushmart.routing.DeepLinkRouter
import com.pushwoosh.pushmart.routing.PushRouting
import com.pushwoosh.pushmart.routing.Route
import com.pushwoosh.pushmart.routing.RouteExtras
import com.pushwoosh.pushmart.ui.common.ResultBus
import com.pushwoosh.pushmart.ui.promo.VoucherFragment
import com.pushwoosh.pushmart.ui.shop.ProductDetailSheet

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val handler = Handler(Looper.getMainLooper())
    private val hideResult = Runnable { binding.resultOverlay.resultRoot.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) applyLaunchArgs(intent)
        val host = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = host.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(startDestination())
        navController.graph = graph
        binding.navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.visibility =
                if (destination.id in TAB_DESTINATIONS) View.VISIBLE else View.GONE
            // applyInsets() keys on the destination, so the new screen needs a fresh pass.
            ViewCompat.requestApplyInsets(binding.root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets -> applyInsets(insets) }

        binding.resultOverlay.resultRoot.setOnClickListener { hideResultNow() }
        ResultBus.events.observe(this) { event ->
            if (event != null) {
                showResult(event)
                ResultBus.events.value = null
            }
        }

        // The intent survives recreation; route it once, not after every rotation.
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

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
        val singleTop = NavOptions.Builder().setLaunchSingleTop(true).build()
        when (route) {
            is Route.Product ->
                Catalog.byId(route.id)?.let { ProductDetailSheet.show(supportFragmentManager, it) }
            Route.Sale -> navController.navigate(R.id.saleFragment, null, singleTop)
            is Route.Voucher ->
                navController.navigate(
                    R.id.voucherFragment,
                    bundleOf(VoucherFragment.ARG_CODE to route.code),
                    singleTop)
        }
    }

    // Content pads only what nobody below takes: the tab bar and full-bleed screens pad themselves.
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
                fullBleed || screen in TAB_DESTINATIONS -> 0
                else -> bars.bottom
            }
        binding.content.setPadding(0, top, 0, bottom)
        return insets.inset(0, top, 0, bottom)
    }

    private fun startDestination(): Int =
        if (services.account.onboarded) R.id.homeFragment else R.id.welcomeFragment

    private fun applyLaunchArgs(intent: Intent?) {
        val args = LaunchArgs.from(intent?.extras?.toMap().orEmpty())
        if (!args.skipOnboarding) return
        if (services.accountManager.enterWithoutOnboarding(args.appCode)) {
            PWLog.debug(
                TAG, "onboarding skipped by launch args, app code ${services.account.appCode}")
        }
    }

    @Suppress("DEPRECATION")
    private fun Bundle.toMap(): Map<String, Any?> = keySet().associateWith { get(it) }

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
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val RESULT_DURATION_MS = 1800L
        val TAB_DESTINATIONS =
            setOf(R.id.homeFragment, R.id.shopFragment, R.id.offersFragment, R.id.profileFragment)
        val FULL_BLEED_DESTINATIONS = setOf(R.id.saleFragment, R.id.voucherFragment)
    }
}
