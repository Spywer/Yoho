package com.spywer.yoho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class YohoColors(
    val posterScrim: Color = YohoPalette.PosterScrim,
    val ratingKp: Color = YohoPalette.Tertiary,
    val ratingImdb: Color = YohoPalette.Secondary,
    val chipSelected: Color = YohoPalette.Primary,
    val chipUnselected: Color = YohoPalette.SurfaceElevated,
)

val LocalYohoColors = staticCompositionLocalOf { YohoColors() }

private val YohoDarkScheme = darkColorScheme(
    primary = YohoPalette.Primary,
    onPrimary = YohoPalette.OnPrimary,
    secondary = YohoPalette.Secondary,
    tertiary = YohoPalette.Tertiary,
    background = YohoPalette.Background,
    onBackground = YohoPalette.OnBackground,
    surface = YohoPalette.Surface,
    onSurface = YohoPalette.OnBackground,
    onSurfaceVariant = YohoPalette.OnSurfaceMuted,
    surfaceVariant = YohoPalette.SurfaceElevated,
    outline = YohoPalette.Outline,
    error = YohoPalette.Error,
)

private val YohoLightScheme = lightColorScheme(
    primary = YohoPalette.Primary,
    onPrimary = YohoPalette.OnPrimary,
    secondary = Color(0xFFB45309),
    tertiary = YohoPalette.Tertiary,
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF12171C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12171C),
    onSurfaceVariant = Color(0xFF5B6670),
    surfaceVariant = Color(0xFFE8ECF0),
    outline = Color(0xFFCDD5DD),
    error = YohoPalette.Error,
)

@Composable
fun YohoTheme(
    darkTheme: Boolean = true, // reyoho по умолчанию тёмная
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) YohoDarkScheme else YohoLightScheme
    CompositionLocalProvider(LocalYohoColors provides YohoColors()) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content,
        )
    }
}

object YohoThemeExtras {
    val colors: YohoColors
        @Composable
        get() = LocalYohoColors.current
}
