package com.pushwoosh.pushmart.ui.promo

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

/**
 * A promo screen whose gradient runs under the system bars. The activity leaves the bars to it
 * (see `MainActivity.FULL_BLEED_DESTINATIONS`), so it pads its own root by them. While shown, the
 * status icons are light (the theme's dark icons vanish on the plum top) and the 3-button contrast
 * scrim is off (it covers the bottom of the gradient); the window state is restored on leave.
 */
abstract class FullBleedFragment(@LayoutRes layout: Int) : Fragment(layout) {
    private var lightStatusBars = true
    private var navigationBarContrast = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        val window = requireActivity().window
        val controller = WindowCompat.getInsetsController(window, requireView())
        lightStatusBars = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            navigationBarContrast = window.isNavigationBarContrastEnforced
            window.isNavigationBarContrastEnforced = false
        }
    }

    override fun onStop() {
        val window = requireActivity().window
        WindowCompat.getInsetsController(window, requireView()).isAppearanceLightStatusBars =
            lightStatusBars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = navigationBarContrast
        }
        super.onStop()
    }
}
