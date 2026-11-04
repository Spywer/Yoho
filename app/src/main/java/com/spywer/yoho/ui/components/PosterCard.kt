package com.spywer.yoho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.displayTitle
import com.spywer.yoho.data.remote.displayYear
import com.spywer.yoho.data.remote.typeLabel
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.theme.YohoThemeExtras

@Composable
fun PosterCard(
    item: TitleDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = YohoThemeExtras.colors
    val metrics = LocalHomeMetrics.current
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
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
                .padding(6.dp),
        ) {
            item.ratings?.kinopoisk?.score?.let { score ->
                RatingChip(
                    label = "КП",
                    value = score,
                    color = colors.ratingKp,
                    fontSize = metrics.ratingChipSize,
                )
                Spacer(Modifier.width(4.dp))
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
                text = item.typeLabel(),
                color = Color.White,
                fontSize = metrics.cardTypeSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                text = item.displayTitle().replace('\n', ' ').trim(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = metrics.cardTitleSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            item.displayYear()?.let { year ->
                Text(
                    text = year.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = metrics.cardMetaSize,
                )
            }
            item.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Text(
                    text = reason,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = metrics.cardMetaSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
