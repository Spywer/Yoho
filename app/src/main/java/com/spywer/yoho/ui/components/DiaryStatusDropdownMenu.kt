package com.spywer.yoho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spywer.yoho.R
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.LocalHomeMetrics

private val diaryStatusLabelRes = listOf(
    R.string.detail_diary_watched,
    R.string.detail_diary_watching,
    R.string.detail_diary_want,
    R.string.detail_diary_rewatching,
    R.string.detail_diary_on_hold,
    R.string.detail_diary_dropped,
)

/**
 * Меню статусов дневника (коллекция + карточка тайтла).
 * DropdownMenu, не Dialog: AlertDialog ломает surface WebView-плеера.
 */
@Composable
fun DiaryStatusDropdownMenu(
    expanded: Boolean,
    currentStatus: String?,
    busy: Boolean = false,
    metrics: HomeMetrics = LocalHomeMetrics.current,
    onDismiss: () -> Unit,
    onSelectStatus: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val inDiary = !currentStatus.isNullOrBlank()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = if (metrics.isTablet) 340.dp else 180.dp),
    ) {
        Text(
            text = stringResource(
                if (inDiary) {
                    R.string.detail_diary_menu_title_active
                } else {
                    R.string.detail_diary_menu_title
                },
            ).uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = metrics.diaryMenuTitle,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(
                horizontal = if (metrics.isTablet) 20.dp else 10.dp,
                vertical = if (metrics.isTablet) 14.dp else 4.dp,
            ),
        )
        diaryStatusLabelRes.forEach { labelRes ->
            val label = stringResource(labelRes)
            val selected = currentStatus == label
            DropdownMenuItem(
                text = {
                    Text(
                        text = label,
                        fontSize = metrics.diaryMenuItem,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                onClick = {
                    if (!busy && !selected) onSelectStatus(label)
                    else onDismiss()
                },
                enabled = !busy,
                modifier = Modifier
                    .then(
                        if (metrics.isTablet) {
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        } else {
                            Modifier
                                .padding(horizontal = 4.dp)
                                .height(36.dp)
                        },
                    )
                    .clip(RoundedCornerShape(if (metrics.isTablet) 10.dp else 6.dp))
                    .then(
                        if (selected) {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier
                        },
                    ),
                contentPadding = PaddingValues(
                    horizontal = if (metrics.isTablet) 16.dp else 10.dp,
                    vertical = if (metrics.isTablet) 12.dp else 0.dp,
                ),
            )
        }
        if (inDiary) {
            HorizontalDivider(
                Modifier.padding(vertical = if (metrics.isTablet) 8.dp else 2.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.detail_diary_remove),
                        fontSize = metrics.diaryMenuItem,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF87171),
                    )
                },
                onClick = {
                    if (!busy) onRemove() else onDismiss()
                },
                enabled = !busy,
                modifier = if (metrics.isTablet) {
                    Modifier
                } else {
                    Modifier.height(36.dp)
                },
                contentPadding = PaddingValues(
                    horizontal = if (metrics.isTablet) 20.dp else 10.dp,
                    vertical = if (metrics.isTablet) 14.dp else 0.dp,
                ),
            )
        }
    }
}
