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
    /** Вертикальный ритм: секции / мелкие блоки */
    val sectionGap: Dp,
    val blockGap: Dp,
    val sectionTitleSize: TextUnit,
    val brandSize: TextUnit,
    val searchHeight: Dp,
    val searchTextSize: TextUnit,
    val chipTextSize: TextUnit,
    val cardTypeSize: TextUnit,
    val cardTitleSize: TextUnit,
    val cardMetaSize: TextUnit,
    val ratingChipSize: TextUnit,
    /** Нижняя навигация */
    val navBarHeight: Dp,
    val navIconSize: Dp,
    val navLabelSize: TextUnit,
    /** Карточка тайтла */
    val detailTitleSize: TextUnit,
    val detailSubtitleSize: TextUnit,
    val detailBodySize: TextUnit,
    val detailBodyLine: TextUnit,
    val detailTagSize: TextUnit,
    val detailMetaSize: TextUnit,
    val detailMetaLine: TextUnit,
    val detailFavTextSize: TextUnit,
    val detailFavIconSize: Dp,
    val detailFavPadH: Dp,
    val detailFavPadV: Dp,
    val castNameSize: TextUnit,
    val castRoleSize: TextUnit,
    /** Плеер: чипы источников + панель */
    val playerChipHeight: Dp,
    val playerChipText: TextUnit,
    val playerBarBtn: Dp,
    val playerBarIcon: Dp,
    val diaryMenuTitle: TextUnit,
    val diaryMenuItem: TextUnit,
    /** Расписание / next air */
    val scheduleTitleSize: TextUnit,
    val scheduleBodySize: TextUnit,
    val scheduleEpSize: TextUnit,
) {
    val isTablet: Boolean get() = widthClass != WidthClass.Compact

    /** Межстрочный для произвольного размера шрифта. */
    fun lineOf(size: TextUnit): TextUnit =
        if (isTablet) {
            (size.value * 1.35f).sp
        } else {
            (size.value + 2f).sp
        }
}

val LocalHomeMetrics = compositionLocalOf { CompactMetrics }

/** Телефон: нормальный Material-масштаб читаемого приложения. */
val CompactMetrics = HomeMetrics(
    widthClass = WidthClass.Compact,
    contentMaxWidth = Dp.Unspecified,
    screenPadding = 16.dp,
    posterLarge = 152.dp,
    poster = 128.dp,
    trailer = 260.dp,
    cardGap = 8.dp,
    sectionGap = 8.dp,
    blockGap = 4.dp,
    sectionTitleSize = 20.sp,
    brandSize = 24.sp,
    searchHeight = 44.dp,
    searchTextSize = 15.sp,
    chipTextSize = 13.sp,
    cardTypeSize = 11.sp,
    cardTitleSize = 14.sp,
    cardMetaSize = 13.sp,
    ratingChipSize = 11.sp,
    navBarHeight = 72.dp,
    navIconSize = 22.dp,
    navLabelSize = 11.sp,
    detailTitleSize = 22.sp,
    detailSubtitleSize = 16.sp,
    detailBodySize = 15.sp,
    detailBodyLine = 18.sp,
    detailTagSize = 13.sp,
    detailMetaSize = 14.sp,
    detailMetaLine = 17.sp,
    detailFavTextSize = 14.sp,
    detailFavIconSize = 18.dp,
    detailFavPadH = 10.dp,
    detailFavPadV = 4.dp,
    castNameSize = 13.sp,
    castRoleSize = 11.sp,
    playerChipHeight = 40.dp,
    playerChipText = 14.sp,
    playerBarBtn = 48.dp,
    playerBarIcon = 24.dp,
    diaryMenuTitle = 11.sp,
    diaryMenuItem = 14.sp,
    scheduleTitleSize = 18.sp,
    scheduleBodySize = 14.sp,
    scheduleEpSize = 14.sp,
)

@Composable
fun rememberHomeMetrics(): HomeMetrics {
    // smallestScreenWidth — стабилен при повороте (иначе телефон в альбоме → Medium и UI скачет).
    val widthDp = LocalConfiguration.current.smallestScreenWidthDp
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
                sectionGap = 16.dp,
                blockGap = 10.dp,
                sectionTitleSize = 32.sp,
                brandSize = 44.sp,
                searchHeight = 60.dp,
                searchTextSize = 22.sp,
                chipTextSize = 20.sp,
                cardTypeSize = 18.sp,
                cardTitleSize = 26.sp,
                cardMetaSize = 22.sp,
                ratingChipSize = 20.sp,
                navBarHeight = 96.dp,
                navIconSize = 36.dp,
                navLabelSize = 20.sp,
                detailTitleSize = 36.sp,
                detailSubtitleSize = 28.sp,
                detailBodySize = 24.sp,
                detailBodyLine = 34.sp,
                detailTagSize = 20.sp,
                detailMetaSize = 24.sp,
                detailMetaLine = 34.sp,
                detailFavTextSize = 22.sp,
                detailFavIconSize = 28.dp,
                detailFavPadH = 18.dp,
                detailFavPadV = 10.dp,
                castNameSize = 20.sp,
                castRoleSize = 13.sp,
                playerChipHeight = 64.dp,
                playerChipText = 26.sp,
                playerBarBtn = 72.dp,
                playerBarIcon = 40.dp,
                diaryMenuTitle = 22.sp,
                diaryMenuItem = 26.sp,
                scheduleTitleSize = 24.sp,
                scheduleBodySize = 22.sp,
                scheduleEpSize = 22.sp,
            )
            widthDp >= 600 -> HomeMetrics(
                widthClass = WidthClass.Medium,
                contentMaxWidth = 1000.dp,
                screenPadding = 28.dp,
                posterLarge = 240.dp,
                poster = 188.dp,
                trailer = 380.dp,
                cardGap = 16.dp,
                sectionGap = 12.dp,
                blockGap = 8.dp,
                sectionTitleSize = 26.sp,
                brandSize = 36.sp,
                searchHeight = 56.dp,
                searchTextSize = 19.sp,
                chipTextSize = 18.sp,
                cardTypeSize = 14.sp,
                cardTitleSize = 18.sp,
                cardMetaSize = 16.sp,
                ratingChipSize = 14.sp,
                navBarHeight = 88.dp,
                navIconSize = 32.dp,
                navLabelSize = 16.sp,
                detailTitleSize = 28.sp,
                detailSubtitleSize = 20.sp,
                detailBodySize = 18.sp,
                detailBodyLine = 28.sp,
                detailTagSize = 16.sp,
                detailMetaSize = 18.sp,
                detailMetaLine = 28.sp,
                detailFavTextSize = 16.sp,
                detailFavIconSize = 22.dp,
                detailFavPadH = 12.dp,
                detailFavPadV = 6.dp,
                castNameSize = 18.sp,
                castRoleSize = 13.sp,
                playerChipHeight = 56.dp,
                playerChipText = 22.sp,
                playerBarBtn = 64.dp,
                playerBarIcon = 36.dp,
                diaryMenuTitle = 18.sp,
                diaryMenuItem = 22.sp,
                scheduleTitleSize = 22.sp,
                scheduleBodySize = 18.sp,
                scheduleEpSize = 18.sp,
            )
            else -> CompactMetrics
        }
    }
}
