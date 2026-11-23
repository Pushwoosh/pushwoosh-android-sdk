package com.pushwoosh.pushmart.ui.common

import android.app.Dialog
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pushwoosh.pushmart.R
import kotlin.math.roundToInt

object PageSheet {
    private const val TOP_GAP_DP = 10

    private val onWindowLayout =
        View.OnLayoutChangeListener { parent, _, _, _, _, _, _, _, _ -> fit(parent) }

    private val roundedTop =
        object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val r = view.resources.getDimensionPixelSize(R.dimen.pm_radius_sheet)
                outline.setRoundRect(0, 0, view.width, view.height + r, r.toFloat())
            }
        }

    /**
     * Expands the sheet of [dialog] and keeps its top [TOP_GAP_DP] below the status bar, like the
     * iOS large sheet, so its rounded top stays visible. The height is applied on every layout of
     * the dialog window: its size and insets are real only after the dialog's first layout, which
     * always follows onStart. The sheet clips its content to a rounded top of `pm_radius_sheet`, so
     * content scrolled under the top edge is cut by the corners on every API level.
     *
     * @return the sheet view, or null when [dialog] is not a [BottomSheetDialog] with content
     */
    fun expand(dialog: Dialog?): View? {
        val sheetDialog = dialog as? BottomSheetDialog ?: return null
        val sheet =
            sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return null
        val parent = sheet.parent as? View ?: return null
        parent.removeOnLayoutChangeListener(onWindowLayout)
        parent.addOnLayoutChangeListener(onWindowLayout)
        sheet.outlineProvider = roundedTop
        sheet.clipToOutline = true
        sheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        sheetDialog.behavior.skipCollapsed = true
        return sheet
    }

    /**
     * Shows [sheet] under [tag] unless a sheet with that tag is already shown or the state is
     * saved. It commits synchronously, so a second tap right after the first finds the sheet.
     *
     * @return true when [sheet] was shown
     */
    fun showOnce(fm: FragmentManager, sheet: DialogFragment, tag: String): Boolean {
        if (fm.isStateSaved || fm.findFragmentByTag(tag) != null) return false
        sheet.showNow(fm, tag)
        return true
    }

    private fun fit(parent: View) {
        val bars =
            ViewCompat.getRootWindowInsets(parent)?.getInsets(WindowInsetsCompat.Type.statusBars())
                ?: return
        val sheet =
            parent.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        val gap = (TOP_GAP_DP * parent.resources.displayMetrics.density).roundToInt()
        val height = parent.height - bars.top - gap
        if (height > 0 && sheet.layoutParams.height != height) {
            sheet.updateLayoutParams { this.height = height }
        }
    }
}
