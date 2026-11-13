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
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.TitleDetailDto
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.WidthClass
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
    onToggleFavorite: () -> Unit,
    onOpenGenre: (String, String?) -> Unit,
) {
    val colors = YohoThemeExtras.colors
    val hasRatings = detail.ratings?.kinopoisk?.score != null ||
        detail.ratings?.imdb?.score != null ||
        detail.rtScore != null
    val showFavorite = detail.idKp != null && detail.idKp > 0
    val hasGenres = detail.genres.isNotEmpty()
    val description = detail.description?.takeIf { it.isNotBlank() }
    // metrics по smallestWidth — телефон в альбоме остаётся Compact;
    // шапка всё равно как на планшете (горизонтальный блок + backdrop).
    val landscape = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_LANDSCAPE
    val wideHero = metrics.isTablet || landscape

    if (wideHero) {
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
                DetailHeroTablet(
                    detail = detail,
                    titleRu = titleRu,
                    titleOriginal = titleOriginal,
                    year = year,
                    isFavorite = isFavorite,
                    favoriteBusy = favoriteBusy,
                    favoriteError = favoriteError,
                    metrics = metrics,
                    showFavorite = showFavorite,
                    hasGenres = hasGenres,
                    hasRatings = hasRatings,
                    description = description,
                    ratingKp = colors.ratingKp,
                    ratingImdb = colors.ratingImdb,
                    onToggleFavorite = onToggleFavorite,
                    onOpenGenre = onOpenGenre,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    } else {
        DetailHeroPhone(
            detail = detail,
            titleRu = titleRu,
            titleOriginal = titleOriginal,
            year = year,
            isFavorite = isFavorite,
            favoriteBusy = favoriteBusy,
            favoriteError = favoriteError,
            metrics = metrics,
            showFavorite = showFavorite,
            hasGenres = hasGenres,
            hasRatings = hasRatings,
            description = description,
            ratingKp = colors.ratingKp,
            ratingImdb = colors.ratingImdb,
            onToggleFavorite = onToggleFavorite,
            onOpenGenre = onOpenGenre,
        )
    }
}

@Composable
private fun DetailHeroPhone(
    detail: TitleDetailDto,
    titleRu: String,
    titleOriginal: String?,
    year: Int?,
    isFavorite: Boolean,
    favoriteBusy: Boolean,
    favoriteError: String?,
    metrics: HomeMetrics,
    showFavorite: Boolean,
    hasGenres: Boolean,
    hasRatings: Boolean,
    description: String?,
    ratingKp: Color,
    ratingImdb: Color,
    onToggleFavorite: () -> Unit,
    onOpenGenre: (String, String?) -> Unit,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            AsyncImage(
                model = detail.poster,
                contentDescription = titleRu,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            if (hasRatings) {
                HeroRatingsRow(
                    detail = detail,
                    ratingKp = ratingKp,
                    ratingImdb = ratingImdb,
                    wrap = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(end = 14.dp, top = 14.dp),
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = metrics.screenPadding)
                .padding(top = metrics.sectionGap, bottom = metrics.blockGap),
            verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(metrics.blockGap)) {
                Text(
                    text = if (year != null) "$titleRu ($year)" else titleRu,
                    color = onBg,
                    fontSize = metrics.detailTitleSize,
                    lineHeight = metrics.lineOf(metrics.detailTitleSize),
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                HeroOriginalAgeRow(
                    titleOriginal = titleOriginal,
                    ageRating = detail.ageRating,
                    metrics = metrics,
                    compact = true,
                    onSurface = true,
                )
            }
            if (showFavorite) {
                HeroFavoriteButton(
                    isFavorite = isFavorite,
                    favoriteBusy = favoriteBusy,
                    favoriteError = favoriteError,
                    metrics = metrics,
                    onToggleFavorite = onToggleFavorite,
                    onSurface = true,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (hasGenres) {
                HeroGenresRow(
                    genres = detail.genres,
                    mediaType = detail.mediaType,
                    metrics = metrics,
                    onOpenGenre = onOpenGenre,
                    onSurface = true,
                )
            }
            description?.let { desc ->
                Text(
                    text = desc,
                    color = onBg.copy(alpha = 0.92f),
                    fontSize = metrics.detailBodySize,
                    lineHeight = metrics.detailBodyLine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeroTablet(
    detail: TitleDetailDto,
    titleRu: String,
    titleOriginal: String?,
    year: Int?,
    isFavorite: Boolean,
    favoriteBusy: Boolean,
    favoriteError: String?,
    metrics: HomeMetrics,
    showFavorite: Boolean,
    hasGenres: Boolean,
    hasRatings: Boolean,
    description: String?,
    ratingKp: Color,
    ratingImdb: Color,
    onToggleFavorite: () -> Unit,
    onOpenGenre: (String, String?) -> Unit,
) {
    val compact = metrics.widthClass == WidthClass.Compact
    val posterWidth = when (metrics.widthClass) {
        WidthClass.Expanded -> 360.dp
        WidthClass.Medium -> 300.dp
        // Телефон-альбом: крупнее posterLarge, но меньше Medium (300).
        WidthClass.Compact -> 200.dp
    }
    val rowGap = if (compact) metrics.cardGap else 16.dp
    val metaTop = if (compact) metrics.sectionGap else 12.dp
    val metaStackGap = when (metrics.widthClass) {
        WidthClass.Expanded -> 24.dp
        WidthClass.Medium -> 12.dp
        WidthClass.Compact -> metrics.sectionGap
    }
    val descTop = if (compact) metrics.sectionGap else 28.dp

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = metrics.screenPadding)
            .padding(top = if (compact) metrics.blockGap else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(rowGap),
    ) {
        AsyncImage(
            model = detail.poster,
            contentDescription = titleRu,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(posterWidth)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(if (compact) 8.dp else 12.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = if (year != null) "$titleRu ($year)" else titleRu,
                color = Color.White,
                fontSize = metrics.detailTitleSize,
                lineHeight = metrics.lineOf(metrics.detailTitleSize),
                fontWeight = FontWeight.Bold,
                maxLines = if (compact) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            HeroOriginalAgeRow(
                titleOriginal = titleOriginal,
                ageRating = detail.ageRating,
                metrics = metrics,
                compact = compact,
            )
            if (showFavorite || hasGenres || hasRatings) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = metaTop),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(
                                end = if (compact) metrics.blockGap else 8.dp,
                                top = if (compact) metrics.blockGap else 6.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(metaStackGap),
                    ) {
                        if (showFavorite) {
                            HeroFavoriteButton(
                                isFavorite = isFavorite,
                                favoriteBusy = favoriteBusy,
                                favoriteError = favoriteError,
                                metrics = metrics,
                                onToggleFavorite = onToggleFavorite,
                            )
                        }
                        if (hasGenres) {
                            HeroGenresRow(
                                genres = detail.genres,
                                mediaType = detail.mediaType,
                                metrics = metrics,
                                onOpenGenre = onOpenGenre,
                            )
                        }
                    }
                    if (hasRatings) {
                        HeroRatingsRow(
                            detail = detail,
                            ratingKp = ratingKp,
                            ratingImdb = ratingImdb,
                            wrap = false,
                        )
                    }
                }
            }
            description?.let { desc ->
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = metrics.detailBodySize,
                    lineHeight = metrics.detailBodyLine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = descTop),
                    maxLines = if (compact) 6 else Int.MAX_VALUE,
                    overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun HeroOriginalAgeRow(
    titleOriginal: String?,
    ageRating: String?,
    metrics: HomeMetrics,
    compact: Boolean,
    onSurface: Boolean = false,
) {
    if (titleOriginal == null && ageRating.isNullOrBlank()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                top = if (compact) 2.dp else 8.dp,
                bottom = if (compact) 0.dp else 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        titleOriginal?.let {
            Text(
                text = it,
                color = if (onSurface) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color.White.copy(alpha = 0.75f)
                },
                fontSize = metrics.detailSubtitleSize,
                lineHeight = metrics.lineOf(metrics.detailSubtitleSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        ageRating?.takeIf { it.isNotBlank() }?.let { age ->
            AgeRatingBadge(age = age)
        }
    }
}

@Composable
private fun HeroFavoriteButton(
    isFavorite: Boolean,
    favoriteBusy: Boolean,
    favoriteError: String?,
    metrics: HomeMetrics,
    onToggleFavorite: () -> Unit,
    onSurface: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val content = if (onSurface) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.White
    }
    Column(modifier) {
    OutlinedButton(
        onClick = onToggleFavorite,
        enabled = !favoriteBusy,
        contentPadding = PaddingValues(
            horizontal = metrics.detailFavPadH,
            vertical = metrics.detailFavPadV,
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
                content
            },
            modifier = Modifier.size(metrics.detailFavIconSize),
        )
        Spacer(Modifier.width(if (metrics.widthClass == WidthClass.Expanded) 10.dp else 6.dp))
        Text(
            text = stringResource(
                if (isFavorite) {
                    R.string.detail_favorite_remove
                } else {
                    R.string.detail_favorite_add
                },
            ),
            color = content,
            fontSize = metrics.detailFavTextSize,
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroGenresRow(
    genres: List<String>,
    mediaType: String?,
    metrics: HomeMetrics,
    onOpenGenre: (String, String?) -> Unit,
    onSurface: Boolean = false,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.blockGap),
        verticalArrangement = Arrangement.spacedBy(metrics.blockGap),
    ) {
        genres.forEach { g ->
            val genreType = when (mediaType) {
                "tv", "anime", "animation" -> mediaType
                else -> "movie"
            }
            Text(
                text = g,
                color = if (onSurface) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    Color.White
                },
                fontSize = metrics.detailTagSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (onSurface) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.White.copy(alpha = 0.18f)
                        },
                    )
                    .clickable { onOpenGenre(g, genreType) }
                    .padding(
                        horizontal = if (metrics.isTablet) 14.dp else 10.dp,
                        vertical = if (metrics.isTablet) 8.dp else 5.dp,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroRatingsRow(
    detail: TitleDetailDto,
    ratingKp: Color,
    ratingImdb: Color,
    wrap: Boolean,
    modifier: Modifier = Modifier,
) {
    if (wrap) {
        val metrics = LocalHomeMetrics.current
        val gap = if (metrics.isTablet) metrics.blockGap else 8.dp
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(gap, alignment = Alignment.End),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            HeroRatingBadges(detail, ratingKp, ratingImdb)
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroRatingBadges(detail, ratingKp, ratingImdb)
        }
    }
}

@Composable
private fun HeroRatingBadges(
    detail: TitleDetailDto,
    ratingKp: Color,
    ratingImdb: Color,
) {
    detail.ratings?.kinopoisk?.score?.let { score ->
        RatingBadge(
            label = "КП",
            value = score,
            votes = detail.ratings.kinopoisk.votes,
            color = ratingKp,
        )
    }
    detail.ratings?.imdb?.score?.let { score ->
        RatingBadge(
            label = "IMDb",
            value = score,
            votes = detail.ratings.imdb.votes,
            color = ratingImdb,
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
