package com.pushwoosh.pushmart.ui.common

import android.content.Context
import com.pushwoosh.pushmart.R

/** The shopper-facing title of a catalog [category]; unknown categories are shown as they are. */
fun Context.categoryTitle(category: String): String =
    when (category) {
        "All" -> getString(R.string.category_all)
        "Outerwear" -> getString(R.string.category_outerwear)
        "Tops" -> getString(R.string.category_tops)
        "Jeans" -> getString(R.string.category_jeans)
        "Dresses" -> getString(R.string.category_dresses)
        "Skirts" -> getString(R.string.category_skirts)
        else -> category
    }
