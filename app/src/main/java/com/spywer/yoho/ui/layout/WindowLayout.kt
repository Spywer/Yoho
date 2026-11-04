package com.spywer.yoho.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WidthClass {
    Compact,
    Medium,
    Expanded,
}

@Immutable
data class HomeMetrics(
    val widthClass: WidthClass,
    val contentMaxWidth: Dp,
    val screenPadding: Dp,
    val posterLarge: Dp,
    val poster: Dp,
    val trailer: Dp,
    val cardGap: Dp,
    val sectionTitleSize: TextUnit,
    val brandSize: TextUnit,
    val searchHeight: Dp,
    val searchTextSize: TextUnit,
    val chipTextSize: TextUnit,
    val cardTypeSize: TextUnit,
    val cardTitleSize: TextUnit,
    val cardMetaSize: TextUnit,
    val ratingChipSize: TextUnit,
) {
    val isTablet: Boolean get() = widthClass != WidthClass.Compact
}

val LocalHomeMetrics = compositionLocalOf { CompactMetrics }

val CompactMetrics = HomeMetrics(
    widthClass = WidthClass.Compact,
    contentMaxWidth = Dp.Unspecified,
    screenPadding = 16.dp,
    posterLarge = 176.dp,
    poster = 148.dp,
    trailer = 280.dp,
    cardGap = 12.dp,
    sectionTitleSize = 22.sp,
    brandSize = 28.sp,
    searchHeight = 48.dp,
    searchTextSize = 17.sp,
    chipTextSize = 16.sp,
    cardTypeSize = 12.sp,
    cardTitleSize = 16.sp,
    cardMetaSize = 15.sp,
    ratingChipSize = 13.sp,
)

@Composable
fun rememberHomeMetrics(): HomeMetrics {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp >= 840 -> HomeMetrics(
                widthClass = WidthClass.Expanded,
                contentMaxWidth = 1280.dp,
                screenPadding = 36.dp,
                posterLarge = 300.dp,
                poster = 228.dp,
                trailer = 460.dp,
                cardGap = 18.dp,
                sectionTitleSize = 32.sp,
                brandSize = 44.sp,
                searchHeight = 60.dp,
                searchTextSize = 22.sp,
                chipTextSize = 20.sp,
                cardTypeSize = 18.sp,
                cardTitleSize = 26.sp,
                cardMetaSize = 22.sp,
                ratingChipSize = 20.sp,
            )
            widthDp >= 600 -> HomeMetrics(
                widthClass = WidthClass.Medium,
                contentMaxWidth = 1000.dp,
                screenPadding = 28.dp,
                posterLarge = 240.dp,
                poster = 188.dp,
                trailer = 380.dp,
                cardGap = 16.dp,
                sectionTitleSize = 26.sp,
                brandSize = 36.sp,
                searchHeight = 56.dp,
                searchTextSize = 19.sp,
                chipTextSize = 18.sp,
                cardTypeSize = 13.sp,
                cardTitleSize = 18.sp,
                cardMetaSize = 16.sp,
                ratingChipSize = 14.sp,
            )
            else -> CompactMetrics
        }
    }
}
