package com.spywer.yoho.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spywer.yoho.R
import com.spywer.yoho.ui.layout.rememberHomeMetrics

@Composable
fun CollectionHubScreen() {
    val context = LocalContext.current
    val metrics = rememberHomeMetrics()
    val titleSize = (metrics.sectionTitleSize.value + 4f).sp
    val rowText = (metrics.searchTextSize.value + 2f).sp
    val iconSize = if (metrics.isTablet) 32.dp else 24.dp
    val pad = metrics.screenPadding

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = pad),
    ) {
        Text(
            text = stringResource(R.string.nav_favorites),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = metrics.blockGap * 2),
        )
        HubRow(
            icon = Icons.Outlined.FavoriteBorder,
            label = stringResource(R.string.favorites_section),
            textSize = rowText,
            iconSize = iconSize,
            onClick = { context.startActivity(FavoritesListActivity.intent(context)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        HubRow(
            icon = Icons.Outlined.AutoStories,
            label = stringResource(R.string.diary_section),
            textSize = rowText,
            iconSize = iconSize,
            onClick = { context.startActivity(DiaryActivity.intent(context)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        HubRow(
            icon = Icons.Outlined.History,
            label = stringResource(R.string.history_section),
            textSize = rowText,
            iconSize = iconSize,
            onClick = { context.startActivity(HistoryActivity.intent(context)) },
        )
    }
}

@Composable
private fun HubRow(
    icon: ImageVector,
    label: String,
    textSize: TextUnit,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
