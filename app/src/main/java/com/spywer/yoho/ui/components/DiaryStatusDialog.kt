package com.spywer.yoho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * Общий диалог статусов дневника (коллекция + карточка тайтла).
 * Шрифты: [HomeMetrics.diaryMenuTitle] / [HomeMetrics.diaryMenuItem].
 */
@Composable
fun DiaryStatusDialog(
    currentStatus: String?,
    busy: Boolean = false,
    metrics: HomeMetrics = LocalHomeMetrics.current,
    onDismiss: () -> Unit,
    onSelectStatus: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val inDiary = !currentStatus.isNullOrBlank()
    val itemRadius = if (metrics.isTablet) 10.dp else 6.dp
    val itemPadH = if (metrics.isTablet) 16.dp else 10.dp
    val itemPadV = if (metrics.isTablet) 12.dp else 8.dp
    val itemGap = if (metrics.isTablet) 4.dp else 2.dp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(itemGap)) {
                diaryStatusLabelRes.forEach { labelRes ->
                    val label = stringResource(labelRes)
                    val selected = currentStatus == label
                    Text(
                        text = label,
                        fontSize = metrics.diaryMenuItem,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(itemRadius))
                            .then(
                                if (selected) {
                                    Modifier.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable(enabled = !busy) {
                                if (!selected) onSelectStatus(label) else onDismiss()
                            }
                            .padding(horizontal = itemPadH, vertical = itemPadV),
                    )
                }
                if (inDiary) {
                    HorizontalDivider(
                        Modifier.padding(vertical = if (metrics.isTablet) 8.dp else 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    )
                    Text(
                        text = stringResource(R.string.detail_diary_remove),
                        fontSize = metrics.diaryMenuItem,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF87171),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(itemRadius))
                            .clickable(enabled = !busy, onClick = onRemove)
                            .padding(horizontal = itemPadH, vertical = itemPadV),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.collection_cancel_select),
                    fontSize = metrics.diaryMenuItem,
                )
            }
        },
    )
}
