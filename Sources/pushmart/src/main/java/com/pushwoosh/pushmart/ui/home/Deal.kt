package com.pushwoosh.pushmart.ui.home

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.catalog.Product

/**
 * A Home collection, as iOS `HomeTabView.Deal`: on a project with its own code it posts [event], so
 * a campaign on that event can show its in-app; a guest browses [products] instead.
 */
enum class Deal(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    @ColorRes val tint: Int,
    val event: String,
    val productIds: List<String>,
) {
    NEW_IN(
        R.string.home_deal_new_in,
        R.drawable.ic_sparkles,
        R.color.pm_coral,
        "showRichMedia",
        listOf("dress_05", "outwear_05", "skirt_03", "tops_05", "jeans_03", "dress_02")),
    GIFT_IDEAS(
        R.string.home_deal_gift_ideas,
        R.drawable.ic_gift,
        R.color.pm_tangerine,
        "showRichMediaClient",
        listOf("tops_01", "skirt_05", "dress_04", "tops_02", "skirt_04", "tops_05")),
    ESSENTIALS(
        R.string.home_deal_essentials,
        R.drawable.ic_tab_shop_filled,
        R.color.pm_violet,
        "showRichMediaServer",
        listOf("tops_03", "jeans_04", "outwear_04", "dress_01", "skirt_02", "jeans_01"));

    val products: List<Product>
        get() = productIds.mapNotNull { Catalog.byId(it) }
}
