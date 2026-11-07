package com.spywer.yoho.ui.favorites

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.DiaryEntryDto
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.TitleNameDto
import com.spywer.yoho.ui.components.ContentErrorPlaceholder
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.detail.TitleDetailActivity
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.theme.YohoThemeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNeedLogin: () -> Unit,
    onOpenTitle: ((idKp: Int?, idTmdb: Int?, mediaType: String?) -> Unit)? = null,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    val context = LocalContext.current

    fun openFavorite(item: FavoriteItemDto) {
        val kp = item.contentId
            ?: item.id?.removePrefix("kp:")?.toIntOrNull()
        if (kp == null || kp <= 0) return
        val media = item.mediaType ?: item.contentType ?: "movie"
        onOpenTitle?.invoke(kp, null, media)
            ?: context.startActivity(
                TitleDetailActivity.intent(context, kp, null, media),
            )
    }

    fun openDiary(item: DiaryEntryDto) {
        val kp = item.resolvedKp()
        if (kp == null || kp <= 0) return
        val media = item.mediaType ?: "movie"
        onOpenTitle?.invoke(kp, item.tmdbId, media)
            ?: context.startActivity(
                TitleDetailActivity.intent(context, kp, item.tmdbId, media),
            )
    }

    fun FavoriteItemDto.toTitleDto(): TitleDto {
        val kp = contentId ?: id?.removePrefix("kp:")?.toIntOrNull()
        return TitleDto(
            idKp = kp,
            mediaType = mediaType ?: contentType,
            title = TitleNameDto(ru = title),
            year = year?.toIntOrNull(),
            poster = posterUrl,
        )
    }

    fun DiaryEntryDto.toTitleDto(): TitleDto =
        TitleDto(
            idKp = resolvedKp(),
            idTmdb = tmdbId,
            mediaType = mediaType,
            title = TitleNameDto(ru = title),
            year = year?.toIntOrNull(),
            poster = resolvedPoster(),
            reason = displayStatus(),
        )

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                state.needsLogin -> {
                    val loginTextSize = if (metrics.isTablet) {
                        metrics.sectionTitleSize
                    } else {
                        metrics.searchTextSize
                    }
                    val loginBtnHeight = if (metrics.isTablet) 56.dp else metrics.searchHeight
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(if (metrics.isTablet) 40.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.favorites_need_login),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = loginTextSize,
                            lineHeight = metrics.lineOf(loginTextSize),
                            fontWeight = if (metrics.isTablet) FontWeight.Medium else FontWeight.Normal,
                        )
                        Button(
                            onClick = onNeedLogin,
                            modifier = Modifier
                                .padding(top = if (metrics.isTablet) 28.dp else 16.dp)
                                .widthIn(min = if (metrics.isTablet) 280.dp else 0.dp)
                                .height(loginBtnHeight),
                        ) {
                            Text(
                                text = stringResource(R.string.account_sign_in),
                                fontSize = if (metrics.isTablet) {
                                    metrics.searchTextSize
                                } else {
                                    metrics.chipTextSize
                                },
                            )
                        }
                    }
                }

                state.error != null &&
                    state.favorites.isEmpty() &&
                    state.history.isEmpty() &&
                    state.diary.isEmpty() -> {
                    ContentErrorPlaceholder(
                        onRetry = { viewModel.load() },
                    )
                }

                else -> {
                    ShelfChips(
                        selected = state.shelf,
                        onSelect = viewModel::selectShelf,
                        textSize = metrics.chipTextSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = metrics.screenPadding,
                                vertical = metrics.blockGap,
                            ),
                    )
                    PullToRefreshBox(
                        isRefreshing = state.refreshing,
                        onRefresh = { viewModel.load(refresh = true) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val emptyText = when (state.shelf) {
                            CollectionShelf.Favorites -> stringResource(R.string.favorites_empty)
                            CollectionShelf.History -> stringResource(R.string.history_empty)
                            CollectionShelf.Diary -> stringResource(R.string.diary_empty)
                        }
                        val shelfEmpty = when (state.shelf) {
                            CollectionShelf.Favorites -> state.favorites.isEmpty()
                            CollectionShelf.History -> state.history.isEmpty()
                            CollectionShelf.Diary -> state.diary.isEmpty()
                        }
                        if (shelfEmpty) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = emptyText,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = metrics.screenPadding),
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = metrics.poster),
                                contentPadding = PaddingValues(
                                    horizontal = metrics.screenPadding,
                                    vertical = metrics.blockGap,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
                                verticalArrangement = Arrangement.spacedBy(metrics.cardGap),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                when (state.shelf) {
                                    CollectionShelf.Favorites -> {
                                        items(
                                            state.favorites,
                                            key = { "fav-${it.id ?: it.contentId}" },
                                        ) { item ->
                                            PosterCard(
                                                item = item.toTitleDto(),
                                                onClick = { openFavorite(item) },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                    CollectionShelf.History -> {
                                        items(
                                            state.history,
                                            key = { "hist-${it.id ?: it.contentId}-${it.ts}" },
                                        ) { item ->
                                            PosterCard(
                                                item = item.toTitleDto(),
                                                onClick = { openFavorite(item) },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                    CollectionShelf.Diary -> {
                                        items(
                                            state.diary,
                                            key = { "diary-${it.id ?: it.itemId}" },
                                        ) { item ->
                                            PosterCard(
                                                item = item.toTitleDto(),
                                                onClick = { openDiary(item) },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfChips(
    selected: CollectionShelf,
    onSelect: (CollectionShelf) -> Unit,
    textSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val yoho = YohoThemeExtras.colors
    val labels = listOf(
        CollectionShelf.Favorites to stringResource(R.string.favorites_section),
        CollectionShelf.History to stringResource(R.string.history_section),
        CollectionShelf.Diary to stringResource(R.string.diary_section),
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { (shelf, label) ->
            FilterChip(
                selected = shelf == selected,
                onClick = { onSelect(shelf) },
                label = { Text(label, fontSize = textSize) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = yoho.chipSelected,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = yoho.chipUnselected,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = null,
            )
        }
    }
}
