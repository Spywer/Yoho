package com.spywer.yoho.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spywer.yoho.R
import com.spywer.yoho.ui.layout.LocalHomeMetrics

/** Заглушка при ошибке сети / загрузки контента. */
@Composable
fun ContentErrorPlaceholder(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalHomeMetrics.current
    val titleSize = if (metrics.isTablet) {
        metrics.sectionTitleSize
    } else {
        metrics.searchTextSize
    }
    val bodySize = if (metrics.isTablet) {
        metrics.searchTextSize
    } else {
        metrics.cardMetaSize
    }
    val btnHeight = if (metrics.isTablet) 56.dp else metrics.searchHeight

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (metrics.isTablet) 40.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(if (metrics.isTablet) 72.dp else 56.dp),
        )
        Spacer(Modifier.height(if (metrics.isTablet) 20.dp else 14.dp))
        Text(
            text = stringResource(R.string.error_load_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = titleSize,
            lineHeight = metrics.lineOf(titleSize),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (metrics.isTablet) 12.dp else 8.dp))
        Text(
            text = stringResource(R.string.error_load_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = bodySize,
            lineHeight = metrics.lineOf(bodySize),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (metrics.isTablet) 28.dp else 18.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier
                .widthIn(
                    min = if (metrics.isTablet) 240.dp else 160.dp,
                    max = if (metrics.isTablet) 360.dp else 280.dp,
                )
                .fillMaxWidth()
                .height(btnHeight),
        ) {
            Text(
                text = stringResource(R.string.retry),
                fontSize = if (metrics.isTablet) metrics.searchTextSize else metrics.chipTextSize,
            )
        }
    }
}
