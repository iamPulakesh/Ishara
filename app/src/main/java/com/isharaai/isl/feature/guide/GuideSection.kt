package com.isharaai.isl.feature.guide

import androidx.annotation.StringRes
import com.isharaai.isl.R

/**
 * Defines all pages available in the Guide pager.
 * Each page has a short tab label and a body with the full content.
 */
enum class GuideSection(
    @StringRes val tabLabelRes: Int,
    @StringRes val bodyRes: Int
) {
    INTRODUCTION(
        tabLabelRes = R.string.guide_tab_intro,
        bodyRes = R.string.guide_intro_body
    ),
    HOW_TO_USE(
        tabLabelRes = R.string.guide_tab_how_to_use,
        bodyRes = R.string.guide_how_to_use_body
    ),
    ABOUT(
        tabLabelRes = R.string.guide_tab_about,
        bodyRes = R.string.guide_about_body
    );
}
