package com.spywer.yoho.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.PlayerChoiceItemDto
import com.spywer.yoho.data.remote.TitleDetailDto
import com.spywer.yoho.data.remote.TvLastAirDto
import com.spywer.yoho.data.remote.TvScheduleDto
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics

@Composable
fun TitleDetailScreen(
    viewModel: TitleDetailViewModel,
    onBack: () -> Unit,
    onOpenTitle: (idKp: Int?, idTmdb: Int?, mediaType: String?) -> Unit,
    onOpenGenre: (label: String, mediaType: String?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    var selectedPersonId by remember { mutableStateOf<Int?>(null) }

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.loading && state.detail == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                state.error != null && state.detail == null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(metrics.screenPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                else -> {
                    state.detail?.let { detail ->
                        DetailContent(
                            detail = detail,
                            isFavorite = state.isFavorite,
                            favoriteBusy = state.favoriteBusy,
                            favoriteError = state.favoriteError,
                            schedule = state.schedule,
                            scheduleLoading = state.scheduleLoading,
                            selectedSeason = state.selectedSeason,
                            lastAir = state.lastAir,
                            playerChoices = state.playerChoices,
                            selectedPlayerSource = state.selectedPlayerSource,
                            playerIframeUrl = state.playerIframeUrl,
                            playerEmbedHtml = state.playerEmbedHtml,
                            playerLoading = state.playerLoading,
                            playerNeedsLogin = state.playerNeedsLogin,
                            diaryStatusLabel = state.diaryStatusLabel,
                            diaryBusy = state.diaryBusy,
                            diaryError = state.diaryError,
                            diaryNotify = state.diaryNotify,
                            onBack = onBack,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onSetDiaryStatus = viewModel::setDiaryStatus,
                            onRemoveFromDiary = viewModel::removeFromDiary,
                            onConsumeDiaryNotify = viewModel::consumeDiaryNotify,
                            onSelectSeason = viewModel::selectSeason,
                            onSelectPlayerSource = viewModel::selectPlayerSource,
                            onOpenTitle = onOpenTitle,
                            onOpenPerson = { selectedPersonId = it },
                            onOpenGenre = onOpenGenre,
                        )
                    }
                }
            }

            selectedPersonId?.let { personId ->
                PersonSheet(
                    personId = personId,
                    onDismiss = { selectedPersonId = null },
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: TitleDetailDto,
    isFavorite: Boolean,
    favoriteBusy: Boolean,
    favoriteError: String?,
    schedule: TvScheduleDto?,
    scheduleLoading: Boolean,
    selectedSeason: Int?,
    lastAir: TvLastAirDto?,
    playerChoices: List<PlayerChoiceItemDto>,
    selectedPlayerSource: String?,
    playerIframeUrl: String?,
    playerEmbedHtml: String?,
    playerLoading: Boolean,
    playerNeedsLogin: Boolean,
    diaryStatusLabel: String?,
    diaryBusy: Boolean,
    diaryError: String?,
    diaryNotify: String?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetDiaryStatus: (String) -> Unit,
    onRemoveFromDiary: () -> Unit,
    onConsumeDiaryNotify: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    onSelectPlayerSource: (String) -> Unit,
    onOpenTitle: (Int?, Int?, String?) -> Unit,
    onOpenPerson: (Int) -> Unit,
    onOpenGenre: (String, String?) -> Unit,
) {
    val context = LocalContext.current
    val metrics = LocalHomeMetrics.current
    val year = detail.year ?: detail.yearStart
    val titleRu = detail.title?.ru ?: detail.title?.original ?: "Без названия"
    val titleOriginal = detail.title?.original?.takeIf { it != titleRu }
    val prequelsTitle = stringResource(R.string.detail_prequels)
    val sequelsTitle = stringResource(R.string.detail_sequels)
    val similarTitle = stringResource(R.string.detail_similar)
    val castTitle = stringResource(R.string.detail_cast)
    val castFullCount = detail.castTotal?.takeIf { it > 0 } ?: detail.cast.size
    val castFullTitle = stringResource(R.string.detail_cast_full, castFullCount)
    val aboutTitle = stringResource(R.string.detail_about)
    val trailerFallback = stringResource(R.string.detail_trailer)
    val showFullCastLink = !detail.castPageHref.isNullOrBlank() && castFullCount > 0
    val selectedSeasonEpisodes = remember(schedule, selectedSeason) {
        schedule?.seasons
            ?.firstOrNull { it.seasonNumber == selectedSeason }
            ?.episodes
            ?.sortedBy { it.episodeNumber ?: 0 }
            .orEmpty()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            DetailHero(
                detail = detail,
                titleRu = titleRu,
                titleOriginal = titleOriginal,
                year = year,
                isFavorite = isFavorite,
                favoriteBusy = favoriteBusy,
                favoriteError = favoriteError,
                metrics = metrics,
                onBack = onBack,
                onToggleFavorite = onToggleFavorite,
                onOpenGenre = onOpenGenre,
            )
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = metrics.screenPadding)
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = aboutTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = metrics.sectionTitleSize,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                val genreMediaType = when (detail.mediaType) {
                    "tv", "anime", "animation" -> detail.mediaType
                    else -> "movie"
                }
                if (detail.aboutFilm.isNotEmpty()) {
                    detail.aboutFilm.forEach { row ->
                        MetaRow(
                            label = row.label.orEmpty(),
                            value = row.value,
                            personIds = row.personIds.orEmpty(),
                            onOpenPerson = onOpenPerson,
                            linkPartsAsGenres = row.id == "genre",
                            onOpenGenre = { onOpenGenre(it, genreMediaType) },
                        )
                    }
                } else {
                    MetaRow("Год", year?.toString())
                    MetaRow("Страны", detail.countries.joinToString(", ").ifBlank { null })
                    MetaRow(
                        label = "Жанры",
                        value = detail.genres.joinToString(", ").ifBlank { null },
                        linkPartsAsGenres = true,
                        onOpenGenre = { onOpenGenre(it, genreMediaType) },
                    )
                    MetaRow(
                        "Длительность",
                        detail.duration?.formatted
                            ?: detail.duration?.minutes?.let { "$it мин" },
                    )
                    MetaRow(
                        label = "Режиссёр",
                        value = detail.director,
                        personIds = detail.directorPersonIds,
                        onOpenPerson = onOpenPerson,
                    )
                    MetaRow("Дистрибьютор", detail.production?.distributor)
                    MetaRow("Бюджет", detail.production?.budget)
                }

                detail.trailer?.url?.let { url ->
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(296.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = detail.trailer.thumbnail ?: detail.backdrop,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(8.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = detail.trailer.title ?: trailerFallback,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = metrics.searchTextSize,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            detail.trailer.date?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = metrics.cardMetaSize,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (
            playerLoading ||
            playerChoices.isNotEmpty() ||
            !playerIframeUrl.isNullOrBlank() ||
            !playerEmbedHtml.isNullOrBlank() ||
            playerNeedsLogin
        ) {
            item {
                PlayerBlock(
                    choices = playerChoices,
                    selectedSource = selectedPlayerSource,
                    iframeUrl = playerIframeUrl,
                    embedHtml = playerEmbedHtml,
                    loading = playerLoading,
                    needsLogin = playerNeedsLogin,
                    diaryStatusLabel = diaryStatusLabel,
                    diaryBusy = diaryBusy,
                    diaryError = diaryError,
                    diaryNotify = diaryNotify,
                    onSelectSource = onSelectPlayerSource,
                    onSetDiaryStatus = onSetDiaryStatus,
                    onRemoveFromDiary = onRemoveFromDiary,
                    onConsumeDiaryNotify = onConsumeDiaryNotify,
                    padding = metrics.screenPadding,
                    metaSize = metrics.cardMetaSize,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }

        titleDetailScheduleSection(
            schedule = schedule,
            scheduleLoading = scheduleLoading,
            selectedSeason = selectedSeason,
            selectedSeasonEpisodes = selectedSeasonEpisodes,
            lastAir = lastAir,
            screenPadding = metrics.screenPadding,
            onSelectSeason = onSelectSeason,
        )

        if (detail.cast.isNotEmpty()) {
            item {
                SectionHeader(
                    castTitle,
                    metrics.sectionTitleSize,
                    metrics.screenPadding,
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = metrics.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(
                        items = detail.cast,
                        key = { it.id ?: it.name ?: it.hashCode() },
                    ) { actor ->
                        Column(
                            Modifier
                                .width(112.dp)
                                .clickable(enabled = actor.id != null && actor.id > 0) {
                                    actor.id?.let(onOpenPerson)
                                },
                        ) {
                            Box(
                                Modifier
                                    .size(112.dp, 148.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                if (!actor.poster.isNullOrBlank()) {
                                    AsyncImage(
                                        model = actor.poster,
                                        contentDescription = actor.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Text(
                                text = actor.name.orEmpty(),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                            )
                            actor.role?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            if (showFullCastLink) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.screenPadding)
                            .padding(top = 28.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(detail.castPageHref)),
                                )
                            },
                        ) {
                            Text(
                                text = castFullTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        relatedSection(
            title = prequelsTitle,
            items = detail.prequels,
            onOpenTitle = onOpenTitle,
            padding = metrics.screenPadding,
            titleSize = metrics.sectionTitleSize,
            cardWidth = metrics.posterLarge,
            gap = metrics.cardGap,
        )
        relatedSection(
            title = sequelsTitle,
            items = detail.sequels,
            onOpenTitle = onOpenTitle,
            padding = metrics.screenPadding,
            titleSize = metrics.sectionTitleSize,
            cardWidth = metrics.posterLarge,
            gap = metrics.cardGap,
        )
        relatedSection(
            title = similarTitle,
            items = detail.similar,
            onOpenTitle = onOpenTitle,
            padding = metrics.screenPadding,
            titleSize = metrics.sectionTitleSize,
            cardWidth = metrics.posterLarge,
            gap = metrics.cardGap,
        )
    }
}
