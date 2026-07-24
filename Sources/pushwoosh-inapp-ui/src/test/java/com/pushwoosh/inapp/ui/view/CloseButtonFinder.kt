package com.pushwoosh.inapp.ui.view

import android.view.View
import android.view.ViewGroup

/** Depth-first lookup of the shared ✕ (identified by its "Close" contentDescription). */
internal fun findCloseButton(root: View): View? {
    if (root.contentDescription == "Close") return root
    if (root is ViewGroup) {
        for (i in 0 until root.childCount) {
            findCloseButton(root.getChildAt(i))?.let { return it }
        }
    }
    return null
}
