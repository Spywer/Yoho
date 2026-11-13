package com.spywer.yoho.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.displayTitle
import com.spywer.yoho.data.remote.displayYear
import com.spywer.yoho.data.remote.typeLabel
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.theme.YohoThemeExtras

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    item: TitleDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    /** like / dislike — бейдж на постере */
    reaction: String? = null,
    /** Подпись сверху слева (напр. дата просмотра). */
    topStartLabel: String? = null,
    /** Тап по reason (статус дневника). */
    onReasonClick: (() -> Unit)? = null,
    /** Dropdown у статуса — якорь на reason, не на всю карточку. */
    reasonMenu: (@Composable () -> Unit)? = null,
) {
    val colors = YohoThemeExtras.colors
    val metrics = LocalHomeMetrics.current
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = item.poster,
            contentDescription = item.displayTitle(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to colors.posterScrim,
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (metrics.isTablet) 10.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (reaction?.lowercase()) {
                "like" -> ReactionBadge(
                    icon = Icons.Filled.ThumbUp,
                    tint = Color(0xFF22C55E),
                    size = if (metrics.isTablet) 44.dp else 34.dp,
                )
                "dislike" -> ReactionBadge(
                    icon = Icons.Filled.ThumbDown,
                    tint = MaterialTheme.colorScheme.error,
                    size = if (metrics.isTablet) 44.dp else 34.dp,
                )
            }
            item.ratings?.kinopoisk?.score?.let { score ->
                RatingChip(
                    label = "КП",
                    value = score,
                    color = colors.ratingKp,
                    fontSize = metrics.ratingChipSize,
                )
            }
            item.ratings?.imdb?.score?.let { score ->
                RatingChip(
                    label = "IM",
                    value = score,
                    color = colors.ratingImdb,
                    fontSize = metrics.ratingChipSize,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Text(
                text = item.displayTitle().replace('\n', ' ').trim(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = metrics.cardTitleSize,
                lineHeight = metrics.lineOf(metrics.cardTitleSize),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val year = item.displayYear()
            val type = item.typeLabel()
            if (type.isNotBlank() || year != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = metrics.blockGap),
                ) {
                    if (type.isNotBlank()) {
                        Text(
                            text = type,
                            color = Color.White,
                            fontSize = metrics.cardTypeSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    year?.let {
                        if (type.isNotBlank()) Spacer(Modifier.width(6.dp))
                        Text(
                            text = it.toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = metrics.cardMetaSize,
                            lineHeight = metrics.lineOf(metrics.cardMetaSize),
                        )
                    }
                }
            }
            item.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Box(Modifier.padding(top = 2.dp)) {
                    Text(
                        text = reason,
                        color = if (onReasonClick != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = metrics.cardMetaSize,
                        lineHeight = metrics.lineOf(metrics.cardMetaSize),
                        fontWeight = if (onReasonClick != null) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (onReasonClick != null) {
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onReasonClick)
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                        } else {
                            Modifier
                        },
                    )
                    reasonMenu?.invoke()
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(if (metrics.isTablet) 10.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selectionMode || selected) {
                Box(
                    modifier = Modifier
                        .size(if (metrics.isTablet) 28.dp else 24.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Black.copy(alpha = 0.45f)
                            },
                        )
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.85f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(if (metrics.isTablet) 18.dp else 16.dp),
                        )
                    }
                }
            }
            topStartLabel?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = if (metrics.isTablet) metrics.cardMetaSize else metrics.cardTypeSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(
                            horizontal = if (metrics.isTablet) 10.dp else 7.dp,
                            vertical = if (metrics.isTablet) 5.dp else 3.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun RatingChip(
    label: String,
    value: Double,
    color: Color,
    fontSize: TextUnit,
) {
    Text(
        text = "$label ${"%.1f".format(value)}",
        color = Color.Black,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
private fun ReactionBadge(
    icon: ImageVector,
    tint: Color,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.72f))
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
