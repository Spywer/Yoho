package com.spywer.yoho.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.TitleDetailDto
import com.spywer.yoho.ui.components.YohoBackButton
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.theme.YohoThemeExtras

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailHero(
    detail: TitleDetailDto,
    titleRu: String,
    titleOriginal: String?,
    year: Int?,
    isFavorite: Boolean,
    favoriteBusy: Boolean,
    favoriteError: String?,
    metrics: HomeMetrics,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenGenre: (String, String?) -> Unit,
) {
    val colors = YohoThemeExtras.colors
Box(Modifier.fillMaxWidth()) {
    AsyncImage(
        model = detail.backdrop ?: detail.poster,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
    Box(
        Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.58f),
                    0.4f to Color.Black.copy(alpha = 0.68f),
                    0.75f to Color.Black.copy(alpha = 0.82f),
                    1f to MaterialTheme.colorScheme.background,
                ),
            ),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = metrics.screenPadding)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AsyncImage(
                model = detail.poster,
                contentDescription = titleRu,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(if (metrics.isTablet) 300.dp else 228.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (year != null) "$titleRu ($year)" else titleRu,
                    color = Color.White,
                    fontSize = if (metrics.isTablet) 32.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (titleOriginal != null || !detail.ageRating.isNullOrBlank()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        titleOriginal?.let {
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 26.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        detail.ageRating?.takeIf { it.isNotBlank() }?.let { age ->
                            AgeRatingBadge(age = age)
                        }
                    }
                }
                val hasRatings = detail.ratings?.kinopoisk?.score != null ||
                    detail.ratings?.imdb?.score != null ||
                    detail.rtScore != null
                val showFavorite = detail.idKp != null && detail.idKp > 0
                val hasGenres = detail.genres.isNotEmpty()
                if (showFavorite || hasGenres || hasRatings) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(end = 8.dp, top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (showFavorite) {
                                OutlinedButton(
                                    onClick = onToggleFavorite,
                                    enabled = !favoriteBusy,
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 6.dp,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Filled.FavoriteBorder
                                        },
                                        contentDescription = null,
                                        tint = if (isFavorite) {
                                            Color(0xFFE53935)
                                        } else {
                                            Color.White
                                        },
                                        modifier = Modifier.size(26.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(
                                            if (isFavorite) {
                                                R.string.detail_favorite_remove
                                            } else {
                                                R.string.detail_favorite_add
                                            },
                                        ),
                                        color = Color.White,
                                        fontSize = 24.sp,
                                    )
                                }
                                favoriteError?.let { err ->
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = metrics.cardMetaSize,
                                    )
                                }
                            }
                            if (hasGenres) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    detail.genres.forEach { g ->
                                        val genreType = when (detail.mediaType) {
                                            "tv", "anime", "animation" -> detail.mediaType
                                            else -> "movie"
                                        }
                                        Text(
                                            text = g,
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(Color.White.copy(alpha = 0.18f))
                                                .clickable {
                                                    onOpenGenre(g, genreType)
                                                }
                                                .padding(
                                                    horizontal = 16.dp,
                                                    vertical = 9.dp,
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                        if (hasRatings) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                detail.ratings?.kinopoisk?.score?.let { score ->
                                    RatingBadge(
                                        label = "КП",
                                        value = score,
                                        votes = detail.ratings.kinopoisk.votes,
                                        color = colors.ratingKp,
                                    )
                                }
                                detail.ratings?.imdb?.score?.let { score ->
                                    RatingBadge(
                                        label = "IMDb",
                                        value = score,
                                        votes = detail.ratings.imdb.votes,
                                        color = colors.ratingImdb,
                                    )
                                }
                                detail.rtScore?.let { score ->
                                    RatingBadge(
                                        label = "RT",
                                        value = score.toDouble(),
                                        votes = null,
                                        color = Color(0xFFFA320A),
                                        suffix = "%",
                                    )
                                }
                            }
                        }
                    }
                }
                detail.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 35.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    YohoBackButton(
        onClick = onBack,
        withStatusBarsPadding = true,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = 12.dp, top = 8.dp),
    )
}
}

