package com.pushwoosh.demoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.pushwoosh.demoapp.ui.deeplink.DeepLinkFragment
import com.pushwoosh.demoapp.utils.InboxStyleHelper
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.richmedia.RichMediaManager
import com.pushwoosh.sampleapp.R
import com.pushwoosh.sampleapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding!!.container) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Top inset for fragment area; BottomNavigationView handles its own bottom inset
            // internally via Material's NavigationBarView listener.
            binding!!
                .contentContainer
                .setPadding(
                    binding!!.contentContainer.paddingLeft,
                    systemBars.top,
                    binding!!.contentContainer.paddingRight,
                    binding!!.contentContainer.paddingBottom)

            insets
        }

        val navController = findNavController(this, R.id.nav_host_fragment_activity_main)
        setupWithNavController(binding!!.navView, navController)

        // Bottom nav hides itself here because this destination has no tab to highlight.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding?.navView?.visibility =
                if (destination.id == R.id.navigation_deep_link) View.GONE else View.VISIBLE
        }

        setupPushwooshSdk()

        // Guard against replaying the deep link after rotation, since the intent survives
        // recreation.
        if (savedInstanceState == null) {
            handleDeepLink(intent, "onCreate")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent, "onNewIntent")
    }

    private fun handleDeepLink(intent: Intent?, source: String) {
        if (intent?.action != Intent.ACTION_VIEW) {
            return
        }
        val uri = intent.data ?: return
        // Explicit intents bypass the manifest filter — accept only the scheme declared there.
        if (!DEEP_LINK_SCHEME.equals(uri.scheme, ignoreCase = true)) {
            return
        }

        PWLog.debug(TAG, "deep link from $source: $uri")

        val args = Bundle().apply { putString(DeepLinkFragment.ARG_URI, uri.toString()) }
        val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
        findNavController(this, R.id.nav_host_fragment_activity_main)
            .navigate(R.id.navigation_deep_link, args, navOptions)
    }

    private fun setupPushwooshSdk() {
        // Configure Rich Media appearance
        RichMediaManager.setDefaultRichMediaConfig(
            ModalRichmediaConfig()
                .setViewPosition(ModalRichMediaViewPosition.FULLSCREEN)
                .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
                .setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_DOWN)
                .setSwipeGestures(setOf(ModalRichMediaSwipeGesture.NONE))
                .setWindowWidth(ModalRichMediaWindowWidth.FULL_SCREEN)
                .setStatusBarCovered(true)
                .setAnimationDuration(300))

        // Configure Inbox style
        InboxStyleHelper.setupCustomInboxStyle(this)

        // VoIP permission request is deferred to Settings — surfacing a system dialog on first
        // launch interrupts the demo flow before the user has any context for it.
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val DEEP_LINK_SCHEME = "pwdemo"
    }
}
