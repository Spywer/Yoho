package com.spywer.yoho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.displayTitle
import com.spywer.yoho.data.remote.displayYear
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.theme.YohoThemeExtras

@Composable
fun TrailerCard(
    item: TitleDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = YohoThemeExtras.colors
    val metrics = LocalHomeMetrics.current
    val playSize = if (metrics.isTablet) 64.dp else 48.dp
    val iconSize = if (metrics.isTablet) 36.dp else 28.dp
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = item.backdrop ?: item.trailer?.thumbnail ?: item.poster,
                contentDescription = item.displayTitle(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colors.posterScrim),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(playSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Text(
            text = item.displayTitle().replace('\n', ' ').trim(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = metrics.cardTitleSize,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        item.displayYear()?.let {
            Text(
                text = it.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = metrics.cardMetaSize,
            )
        }
    }
}
