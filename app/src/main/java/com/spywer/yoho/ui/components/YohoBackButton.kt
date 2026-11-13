package com.spywer.yoho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Круглая «Назад» как на странице фильма. */
@Composable
fun YohoBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    withStatusBarsPadding: Boolean = false,
    size: Dp = 44.dp,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .then(if (withStatusBarsPadding) Modifier.statusBarsPadding() else Modifier)
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(Color.Black.copy(alpha = 0.55f)),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
