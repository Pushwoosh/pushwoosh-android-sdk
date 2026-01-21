package com.pushwoosh.demoapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.AppBarConfiguration.Builder
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.pushwoosh.calls.PushwooshCallSettings
import com.pushwoosh.demoapp.databinding.ActivityMainBinding
import com.pushwoosh.demoapp.utils.InboxStyleHelper
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth
import com.pushwoosh.richmedia.RichMediaManager

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setSupportActionBar(binding!!.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding!!.container) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding!!
                .navView
                .setPadding(
                    binding!!.navView.paddingLeft,
                    binding!!.navView.paddingTop,
                    binding!!.navView.paddingRight,
                    systemBars.bottom)
            insets
        }

        val appBarConfiguration: AppBarConfiguration =
            Builder(R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_inbox)
                .build()

        val navController = findNavController(this, R.id.nav_host_fragment_activity_main)

        androidx.navigation.ui.NavigationUI.setupActionBarWithNavController(
            this, navController, appBarConfiguration)

        setupWithNavController(binding!!.navView, navController)

        setupPushwooshSdk()
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
                .setStatusBarCovered(false)
                .setAnimationDuration(300))

        // Configure Inbox style
        InboxStyleHelper.setupCustomInboxStyle(this)

        // Request call permissions
        PushwooshCallSettings.requestCallPermissions(
            object : com.pushwoosh.calls.CallPermissionsCallback {
                override fun onPermissionResult(
                    granted: Boolean,
                    grantedPermissions: List<String>,
                    deniedPermissions: List<String>
                ) {
                    if (granted) {
                        android.util.Log.d(
                            "MainActivity", "Call permissions granted: $grantedPermissions")
                    } else {
                        android.util.Log.w(
                            "MainActivity", "Call permissions denied: $deniedPermissions")
                    }
                }
            })
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(this, R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
