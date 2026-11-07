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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.spywer.yoho.R
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.PlayerChoiceItemDto
import com.spywer.yoho.data.remote.SessionCookieJar
import com.spywer.yoho.data.remote.TitleDetailDto
import com.spywer.yoho.data.remote.TvLastAirDto
import com.spywer.yoho.data.remote.TvScheduleDto
import com.spywer.yoho.ui.components.ContentErrorPlaceholder
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.trailer.openTrailerPlayer

@Composable
fun TitleDetailScreen(
    viewModel: TitleDetailViewModel,
    onBack: () -> Unit,
    onOpenTitle: (idKp: Int?, idTmdb: Int?, mediaType: String?) -> Unit,
    onOpenGenre: (label: String, mediaType: String?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by SessionRepository.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    var selectedPersonId by remember { mutableStateOf<Int?>(null) }
    val playerAllowed = session.isLoggedIn || SessionCookieJar.get().hasSessionCookie()

    BackHandler {
        if (selectedPersonId != null) {
            selectedPersonId = null
        } else {
            onBack()
        }
    }

    // Первый sync делает load(); здесь — только смена сессии (иначе плеер мигает дважды).
    var playerAllowedSeen by remember { mutableStateOf(false) }
    LaunchedEffect(playerAllowed) {
        if (!playerAllowedSeen) {
            playerAllowedSeen = true
            return@LaunchedEffect
        }
        viewModel.syncPlayerForSession()
    }

    val inPip = LocalDetailPip.current?.inPipState?.value == true

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (inPip) Color.Black else MaterialTheme.colorScheme.background),
        ) {
            when {
                state.loading && state.detail == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                state.error != null && state.detail == null -> {
                    ContentErrorPlaceholder(
                        onRetry = viewModel::retry,
                    )
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
                            showPlayer = playerAllowed,
                            diaryStatusLabel = state.diaryStatusLabel,
                            diaryBusy = state.diaryBusy,
                            diaryError = state.diaryError,
                            diaryNotify = state.diaryNotify,
                            playerReaction = state.playerReaction,
                            reactionBusy = state.reactionBusy,
                            // В PiP — без hero/scroll; видео уже на decor overlay.
                            pipMode = inPip,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onSetDiaryStatus = viewModel::setDiaryStatus,
                            onRemoveFromDiary = viewModel::removeFromDiary,
                            onConsumeDiaryNotify = viewModel::consumeDiaryNotify,
                            onPlayerReaction = viewModel::setPlayerReaction,
                            onSelectSeason = viewModel::selectSeason,
                            onSelectPlayerSource = viewModel::selectPlayerSource,
                            onOpenTitle = onOpenTitle,
                            onOpenPerson = { selectedPersonId = it },
                            onOpenGenre = onOpenGenre,
                        )
                    }
                }
            }

            if (!inPip) {
                selectedPersonId?.let { personId ->
                    PersonSheet(
                        personId = personId,
                        onDismiss = { selectedPersonId = null },
                    )
                }
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
    showPlayer: Boolean,
    diaryStatusLabel: String?,
    diaryBusy: Boolean,
    diaryError: String?,
    diaryNotify: String?,
    playerReaction: String?,
    reactionBusy: Boolean,
    pipMode: Boolean = false,
    onToggleFavorite: () -> Unit,
    onSetDiaryStatus: (String) -> Unit,
    onRemoveFromDiary: () -> Unit,
    onConsumeDiaryNotify: () -> Unit,
    onPlayerReaction: (String) -> Unit,
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
    val aboutTitle = stringResource(
        when (detail.mediaType?.lowercase()) {
            "tv" -> R.string.detail_about_tv
            "animation" -> R.string.detail_about_animation
            "anime" -> R.string.detail_about_anime
            else -> R.string.detail_about
        },
    )
    val trailerFallback = stringResource(R.string.detail_trailer)
    val trailerClips = remember(detail.trailer) { detail.trailer?.playableClips().orEmpty() }
    val showFullCastLink = !detail.castPageHref.isNullOrBlank() && castFullCount > 0
    val selectedSeasonEpisodes = remember(schedule, selectedSeason) {
        schedule?.seasons
            ?.firstOrNull { it.seasonNumber == selectedSeason }
            ?.episodes
            ?.sortedBy { it.episodeNumber ?: 0 }
            .orEmpty()
    }
    val playerHolder = remember(detail.idKp, detail.idTmdb) { PlayerWebViewHolder() }
    val pipController = LocalDetailPip.current
    val pipActivity = context.findActivity()
    DisposableEffect(playerHolder) {
        onDispose {
            pipController?.clearPlayer(pipActivity)
            playerHolder.destroy()
        }
    }
    if (pipMode && showPlayer) {
        DetailPipEligibilityEffect(eligible = true)
        DetailPipWebViewBindEffect(webView = playerHolder.webView)
        DisposableEffect(playerHolder) {
            playerHolder.onCinemaExit = {
                playerHolder.detachCinemaFullscreen()
                if (playerHolder.isHtmlFullscreen) {
                    playerHolder.requestExitHtmlFullscreen()
                }
            }
            onDispose { playerHolder.onCinemaExit = null }
        }
        PlayerPipSurface(holder = playerHolder, modifier = Modifier.fillMaxSize())
        return
    }
    // verticalScroll: плеер не dispose при повороте (в отличие от LazyColumn).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = if (metrics.isTablet) 32.dp else 20.dp),
    ) {
            DetailHero(
                detail = detail,
                titleRu = titleRu,
                titleOriginal = titleOriginal,
                year = year,
                isFavorite = isFavorite,
                favoriteBusy = favoriteBusy,
                favoriteError = favoriteError,
                metrics = metrics,
                onToggleFavorite = onToggleFavorite,
                onOpenGenre = onOpenGenre,
            )

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

                if (trailerClips.isNotEmpty()) {
                    Spacer(Modifier.height(metrics.sectionGap))
                    val trailer = detail.trailer
                    val trailerMod = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            context.openTrailerPlayer(
                                clips = trailerClips,
                                title = detail.trailer?.title ?: trailerFallback,
                            )
                        }
                        .padding(if (metrics.isTablet) 12.dp else 10.dp)
                    val thumbW = if (metrics.isTablet) 296.dp else 112.dp
                    val playIcon = if (metrics.isTablet) 48.dp else 28.dp
                    Row(
                        modifier = trailerMod,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(thumbW)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(if (metrics.isTablet) 10.dp else 8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = trailer?.thumbnail ?: detail.backdrop,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(playIcon)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(if (metrics.isTablet) 8.dp else 4.dp),
                            )
                        }
                        Spacer(Modifier.width(if (metrics.isTablet) 14.dp else 10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = trailer?.title ?: trailerFallback,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = if (metrics.isTablet) {
                                    metrics.searchTextSize
                                } else {
                                    metrics.cardMetaSize
                                },
                                lineHeight = metrics.lineOf(
                                    if (metrics.isTablet) {
                                        metrics.searchTextSize
                                    } else {
                                        metrics.cardMetaSize
                                    },
                                ),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val metaParts = buildList {
                                trailer?.date?.let { raw ->
                                    formatLongDateRu(raw)?.let(::add)
                                }
                                if (trailerClips.size > 1) {
                                    add(
                                        stringResource(
                                            R.string.detail_trailer_clips_count,
                                            trailerClips.size,
                                        ),
                                    )
                                }
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString(" · "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = metrics.cardMetaSize,
                                    lineHeight = metrics.lineOf(metrics.cardMetaSize),
                                    modifier = Modifier.padding(top = metrics.blockGap),
                                )
                            }
                        }
                    }
                }
            }

            if (showPlayer) {
                PlayerBlock(
                    choices = playerChoices,
                    selectedSource = selectedPlayerSource,
                    iframeUrl = playerIframeUrl,
                    embedHtml = playerEmbedHtml,
                    loading = playerLoading,
                    needsLogin = playerNeedsLogin,
                    mediaType = detail.mediaType,
                    diaryStatusLabel = diaryStatusLabel,
                    diaryBusy = diaryBusy,
                    diaryError = diaryError,
                    diaryNotify = diaryNotify,
                    playerReaction = playerReaction,
                    reactionBusy = reactionBusy,
                    onSelectSource = onSelectPlayerSource,
                    onSetDiaryStatus = onSetDiaryStatus,
                    onRemoveFromDiary = onRemoveFromDiary,
                    onConsumeDiaryNotify = onConsumeDiaryNotify,
                    onPlayerReaction = onPlayerReaction,
                    padding = metrics.screenPadding,
                    metaSize = metrics.cardMetaSize,
                    webHolder = playerHolder,
                    modifier = Modifier.padding(top = metrics.sectionGap),
                )
            }

        TitleDetailScheduleSection(
            schedule = schedule,
            scheduleLoading = scheduleLoading,
            selectedSeason = selectedSeason,
            selectedSeasonEpisodes = selectedSeasonEpisodes,
            lastAir = lastAir,
            screenPadding = metrics.screenPadding,
            onSelectSeason = onSelectSeason,
        )

        if (detail.cast.isNotEmpty()) {
                SectionHeader(
                    castTitle,
                    metrics.sectionTitleSize,
                    metrics.screenPadding,
                )
                val castCardW = if (metrics.isTablet) 112.dp else 88.dp
                val castCardH = if (metrics.isTablet) 148.dp else 116.dp
                LazyRow(
                    contentPadding = PaddingValues(horizontal = metrics.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (metrics.isTablet) 18.dp else 12.dp,
                    ),
                ) {
                    items(
                        items = detail.cast,
                        key = { it.id ?: it.name ?: it.hashCode() },
                    ) { actor ->
                        Column(
                            Modifier
                                .width(castCardW)
                                .clickable(enabled = actor.id != null && actor.id > 0) {
                                    actor.id?.let(onOpenPerson)
                                },
                        ) {
                            Box(
                                Modifier
                                    .size(castCardW, castCardH)
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
                                fontSize = metrics.castNameSize,
                                lineHeight = metrics.lineOf(metrics.castNameSize),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = metrics.blockGap),
                            )
                            actor.role?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = metrics.castRoleSize,
                                    lineHeight = metrics.lineOf(metrics.castRoleSize),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            if (showFullCastLink) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.screenPadding)
                            .padding(
                                top = if (metrics.isTablet) 28.dp else 16.dp,
                                bottom = 4.dp,
                            ),
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
                                fontSize = metrics.detailFavTextSize,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
            }
        }

        RelatedSection(
            title = prequelsTitle,
            items = detail.prequels,
            onOpenTitle = onOpenTitle,
            padding = metrics.screenPadding,
            titleSize = metrics.sectionTitleSize,
            cardWidth = metrics.posterLarge,
            gap = metrics.cardGap,
        )
        RelatedSection(
            title = sequelsTitle,
            items = detail.sequels,
            onOpenTitle = onOpenTitle,
            padding = metrics.screenPadding,
            titleSize = metrics.sectionTitleSize,
            cardWidth = metrics.posterLarge,
            gap = metrics.cardGap,
        )
        RelatedSection(
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
