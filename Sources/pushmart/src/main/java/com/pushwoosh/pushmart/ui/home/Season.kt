package com.pushwoosh.pushmart.ui.home

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pushwoosh.pushmart.R
import java.util.Calendar

/**
 * The season's edit on Home and the Sale screen it opens, picked by the device month
 * (northern-hemisphere seasons), as iOS `Season`.
 *
 * @property title the Sale title, one line per `\n`
 * @property gradient three `@ColorRes` ids, top-leading to bottom-trailing
 */
enum class Season(
    @StringRes val edit: Int,
    @StringRes val heroTitle: Int,
    val heroProductId: String,
    @ColorRes val card: Int,
    @StringRes val title: Int,
    @StringRes val blurb: Int,
    @StringRes val ticker: Int,
    @DrawableRes val symbol: Int,
    @ColorRes val symbolColor: Int,
    val gradient: IntArray,
    @ColorRes val glowA: Int,
    @ColorRes val glowB: Int,
) {
    SPRING(
        R.string.season_edit_spring,
        R.string.season_hero_spring,
        "outwear_02",
        R.color.pm_season_spring_card,
        R.string.season_title_spring,
        R.string.season_blurb_spring,
        R.string.season_ticker_spring,
        R.drawable.ic_season_spring,
        R.color.pm_spring_symbol,
        intArrayOf(R.color.pm_spring_0, R.color.pm_spring_1, R.color.pm_spring_2),
        R.color.pm_spring_glow_a,
        R.color.pm_spring_glow_b),
    SUMMER(
        R.string.season_edit_summer,
        R.string.season_hero_summer,
        "dress_04",
        R.color.pm_season_summer_card,
        R.string.season_title_summer,
        R.string.season_blurb_summer,
        R.string.season_ticker_summer,
        R.drawable.ic_season_summer,
        R.color.pm_summer_symbol,
        intArrayOf(R.color.pm_summer_0, R.color.pm_summer_1, R.color.pm_summer_2),
        R.color.pm_summer_glow_a,
        R.color.pm_summer_glow_b),
    AUTUMN(
        R.string.season_edit_autumn,
        R.string.season_hero_autumn,
        "outwear_04",
        R.color.pm_season_autumn_card,
        R.string.season_title_autumn,
        R.string.season_blurb_autumn,
        R.string.season_ticker_autumn,
        R.drawable.ic_season_autumn,
        R.color.pm_autumn_symbol,
        intArrayOf(R.color.pm_autumn_0, R.color.pm_autumn_1, R.color.pm_autumn_2),
        R.color.pm_autumn_glow_a,
        R.color.pm_autumn_glow_b),
    WINTER(
        R.string.season_edit_winter,
        R.string.season_hero_winter,
        "outwear_05",
        R.color.pm_season_winter_card,
        R.string.season_title_winter,
        R.string.season_blurb_winter,
        R.string.season_ticker_winter,
        R.drawable.ic_season_winter,
        R.color.pm_winter_symbol,
        intArrayOf(R.color.pm_winter_0, R.color.pm_winter_1, R.color.pm_winter_2),
        R.color.pm_winter_glow_a,
        R.color.pm_winter_glow_b);

    companion object {
        /** @param month 1 for January through 12 for December */
        fun of(month: Int): Season =
            when (month) {
                in 3..5 -> SPRING
                in 6..8 -> SUMMER
                in 9..11 -> AUTUMN
                else -> WINTER
            }

        val current: Season
            get() = of(Calendar.getInstance().get(Calendar.MONTH) + 1)
    }
}
