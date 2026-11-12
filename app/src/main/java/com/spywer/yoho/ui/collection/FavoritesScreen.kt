package com.spywer.yoho.ui.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.DiaryEntryDto
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.TitleNameDto
import com.spywer.yoho.ui.components.ContentErrorPlaceholder
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.components.YohoBackButton
import com.spywer.yoho.ui.detail.TitleDetailActivity
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.theme.YohoThemeExtras

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    onNeedLogin: () -> Unit,
    onBack: (() -> Unit)? = null,
    fixedShelf: CollectionShelf? = null,
    onOpenTitle: ((idKp: Int?, idTmdb: Int?, mediaType: String?) -> Unit)? = null,
    viewModel: FavoritesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shelf = fixedShelf ?: state.shelf
    val metrics = rememberHomeMetrics()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    var statusEditEntry by remember { mutableStateOf<DiaryEntryDto?>(null) }
    val titleRes = when (shelf) {
        CollectionShelf.Favorites -> R.string.favorites_section
        CollectionShelf.Diary -> R.string.diary_section
        CollectionShelf.History -> R.string.history_section
    }

    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    LaunchedEffect(state.actionError) {
        val msg = state.actionError ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.consumeActionError()
    }

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

    fun DiaryEntryDto.toTitleDto(statusPlaceholder: String): TitleDto =
        TitleDto(
            idKp = resolvedKp(),
            idTmdb = tmdbId,
            mediaType = mediaType,
            title = TitleNameDto(ru = title),
            year = year?.toIntOrNull(),
            poster = resolvedPoster(),
            reason = statusLabel?.takeIf { it.isNotBlank() } ?: statusPlaceholder,
        )

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Box(Modifier.fillMaxSize()) {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = metrics.screenPadding)
                                .padding(top = metrics.blockGap),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(metrics.blockGap),
                        ) {
                            if (onBack != null) {
                                YohoBackButton(
                                    onClick = onBack,
                                    size = if (metrics.isTablet) 48.dp else 40.dp,
                                )
                            }
                            Text(
                                text = stringResource(titleRes),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = (metrics.sectionTitleSize.value + 2f).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ShelfActionsBar(
                            state = state.copy(shelf = shelf),
                            metrics = metrics,
                            onFavoritesSort = viewModel::setFavoritesSort,
                            onHistorySort = viewModel::setHistorySort,
                            onDiarySort = viewModel::setDiarySort,
                            onEnterSelect = viewModel::startSelectionMode,
                            onSelectAll = viewModel::selectAllVisible,
                            onClearSelection = viewModel::clearSelection,
                            onDelete = { confirmDelete = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = metrics.screenPadding)
                                .padding(vertical = metrics.blockGap),
                        )
                        if (shelf == CollectionShelf.Diary) {
                            DiaryStatusChips(
                                statusSelected = state.diaryStatusFilter,
                                reactionSelected = state.diaryReactionFilter,
                                onStatusSelect = viewModel::setDiaryStatusFilter,
                                onReactionSelect = viewModel::setDiaryReactionFilter,
                                metrics = metrics,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = metrics.screenPadding,
                                        vertical = metrics.blockGap,
                                    ),
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(top = metrics.blockGap),
                        ) {
                        key(
                            shelf,
                            state.diaryStatusFilter,
                            state.diaryReactionFilter,
                            state.favoritesSort,
                            state.historySort,
                            state.diarySort,
                        ) {
                        PullToRefreshBox(
                            isRefreshing = state.refreshing,
                            onRefresh = { viewModel.load(refresh = true) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            val emptyText = when (shelf) {
                                CollectionShelf.Favorites -> stringResource(R.string.favorites_empty)
                                CollectionShelf.History -> stringResource(R.string.history_empty)
                                CollectionShelf.Diary -> stringResource(R.string.diary_empty)
                            }
                            val shelfEmpty = when (shelf) {
                                CollectionShelf.Favorites -> state.displayedFavorites.isEmpty()
                                CollectionShelf.History -> state.displayedHistory.isEmpty()
                                CollectionShelf.Diary -> state.displayedDiary.isEmpty()
                            }
                            if (shelfEmpty) {
                                val emptySize = if (metrics.isTablet) {
                                    metrics.cardMetaSize
                                } else {
                                    metrics.chipTextSize
                                }
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emptyText,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        fontSize = emptySize,
                                        lineHeight = metrics.lineOf(emptySize),
                                        fontWeight = if (metrics.isTablet) {
                                            FontWeight.Medium
                                        } else {
                                            FontWeight.Normal
                                        },
                                        modifier = Modifier.padding(horizontal = metrics.screenPadding),
                                    )
                                }
                            } else {
                                val diaryStatusPlaceholder =
                                    stringResource(R.string.collection_diary_set_status)
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
                                    when (shelf) {
                                        CollectionShelf.Favorites -> {
                                            items(
                                                state.displayedFavorites,
                                                key = { "fav-${it.selectionKey()}" },
                                            ) { item ->
                                                val key = item.selectionKey()
                                                val selected = key in state.selectedIds
                                                PosterCard(
                                                    item = item.toTitleDto(),
                                                    selected = selected,
                                                    selectionMode = state.selectionMode,
                                                    onClick = {
                                                        if (state.selectionMode) {
                                                            viewModel.toggleSelection(key)
                                                        } else {
                                                            openFavorite(item)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        viewModel.enterSelection(key)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }
                                        CollectionShelf.History -> {
                                            items(
                                                state.displayedHistory,
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
                                                state.displayedDiary,
                                                key = { "diary-${it.selectionKey()}" },
                                            ) { item ->
                                                val key = item.selectionKey()
                                                val selected = key in state.selectedIds
                                                PosterCard(
                                                    item = item.toTitleDto(diaryStatusPlaceholder),
                                                    selected = selected,
                                                    selectionMode = state.selectionMode,
                                                    reaction = item.sourceReaction
                                                        ?.takeIf { it == "like" || it == "dislike" },
                                                    topStartLabel = formatWatchedDate(item.watchedAt),
                                                    onReasonClick = if (!state.selectionMode) {
                                                        { statusEditEntry = item }
                                                    } else {
                                                        null
                                                    },
                                                    onClick = {
                                                        if (state.selectionMode) {
                                                            viewModel.toggleSelection(key)
                                                        } else {
                                                            openDiary(item)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        viewModel.enterSelection(key)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                        if (state.mutating) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = metrics.blockGap),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.collection_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.collection_delete_confirm_body,
                        state.selectedIds.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteSelected()
                    },
                    enabled = !state.mutating && state.selectedIds.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.collection_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.collection_cancel_select))
                }
            },
        )
    }

    statusEditEntry?.let { entry ->
        val menuTitleSize = if (metrics.isTablet) metrics.sectionTitleSize else metrics.searchTextSize
        val menuItemSize = if (metrics.isTablet) metrics.cardMetaSize else metrics.searchTextSize
        val menuItemMin = if (metrics.isTablet) 56.dp else 48.dp
        AlertDialog(
            onDismissRequest = { statusEditEntry = null },
            title = {
                Text(
                    text = stringResource(R.string.collection_diary_change_status),
                    fontSize = menuTitleSize,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DiaryStatusLabels.forEach { label ->
                        val active = entry.statusLabel == label
                        TextButton(
                            onClick = {
                                viewModel.changeDiaryStatus(entry.selectionKey(), label)
                                statusEditEntry = null
                            },
                            enabled = !state.mutating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = menuItemMin),
                        ) {
                            Text(
                                text = label,
                                fontSize = menuItemSize,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    if (!entry.statusLabel.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.clearDiaryStatus(entry.selectionKey())
                                statusEditEntry = null
                            },
                            enabled = !state.mutating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = menuItemMin)
                                .padding(top = metrics.blockGap),
                        ) {
                            Text(
                                text = stringResource(R.string.collection_diary_remove_status),
                                fontSize = menuItemSize,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { statusEditEntry = null }) {
                    Text(
                        text = stringResource(R.string.collection_cancel_select),
                        fontSize = menuItemSize,
                    )
                }
            },
        )
    }
}

@Composable
private fun ShelfActionsBar(
    state: FavoritesUiState,
    metrics: HomeMetrics,
    onFavoritesSort: (FavoritesSort) -> Unit,
    onHistorySort: (FavoritesSort) -> Unit,
    onDiarySort: (DiarySort) -> Unit,
    onEnterSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortOpen by remember { mutableStateOf(false) }
    val textSize = if (metrics.isTablet) metrics.cardMetaSize else metrics.searchTextSize
    val iconSize = if (metrics.isTablet) 32.dp else 24.dp
    val actionMin = if (metrics.isTablet) 56.dp else 44.dp
    val menuTextSize = if (metrics.isTablet) metrics.cardMetaSize else metrics.searchTextSize
    val canSelect = state.shelf == CollectionShelf.Favorites ||
        state.shelf == CollectionShelf.Diary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            TextButton(
                onClick = { sortOpen = true },
                modifier = Modifier.heightIn(min = actionMin),
            ) {
                Icon(
                    Icons.Outlined.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
                Text(
                    text = "  " + when (state.shelf) {
                        CollectionShelf.Favorites -> when (state.favoritesSort) {
                            FavoritesSort.DateDesc ->
                                stringResource(R.string.collection_sort_date_desc)
                            FavoritesSort.DateAsc ->
                                stringResource(R.string.collection_sort_date_asc)
                        }
                        CollectionShelf.History -> when (state.historySort) {
                            FavoritesSort.DateDesc ->
                                stringResource(R.string.collection_sort_date_desc)
                            FavoritesSort.DateAsc ->
                                stringResource(R.string.collection_sort_date_asc)
                        }
                        CollectionShelf.Diary -> when (state.diarySort) {
                            DiarySort.AddedDesc ->
                                stringResource(R.string.collection_sort_added_desc)
                            DiarySort.AddedAsc ->
                                stringResource(R.string.collection_sort_added_asc)
                            DiarySort.WatchedDesc ->
                                stringResource(R.string.collection_sort_watched_desc)
                            DiarySort.WatchedAsc ->
                                stringResource(R.string.collection_sort_watched_asc)
                        }
                    },
                    fontSize = textSize,
                    fontWeight = if (metrics.isTablet) FontWeight.Medium else FontWeight.Normal,
                )
            }
            val menuItemPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = if (metrics.isTablet) 16.dp else 12.dp,
            )
            @Composable
            fun SortMenuItem(
                label: String,
                selected: Boolean,
                onClick: () -> Unit,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontSize = menuTextSize,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onClick()
                        sortOpen = false
                    },
                    trailingIcon = if (selected) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (metrics.isTablet) 24.dp else 20.dp),
                            )
                        }
                    } else {
                        null
                    },
                    contentPadding = menuItemPadding,
                )
            }
            DropdownMenu(
                expanded = sortOpen,
                onDismissRequest = { sortOpen = false },
                modifier = Modifier.widthIn(
                    min = if (metrics.isTablet) 420.dp else 300.dp,
                ),
            ) {
                when (state.shelf) {
                    CollectionShelf.Favorites -> {
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_date_desc),
                            selected = state.favoritesSort == FavoritesSort.DateDesc,
                            onClick = { onFavoritesSort(FavoritesSort.DateDesc) },
                        )
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_date_asc),
                            selected = state.favoritesSort == FavoritesSort.DateAsc,
                            onClick = { onFavoritesSort(FavoritesSort.DateAsc) },
                        )
                    }
                    CollectionShelf.History -> {
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_date_desc),
                            selected = state.historySort == FavoritesSort.DateDesc,
                            onClick = { onHistorySort(FavoritesSort.DateDesc) },
                        )
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_date_asc),
                            selected = state.historySort == FavoritesSort.DateAsc,
                            onClick = { onHistorySort(FavoritesSort.DateAsc) },
                        )
                    }
                    CollectionShelf.Diary -> {
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_added_desc),
                            selected = state.diarySort == DiarySort.AddedDesc,
                            onClick = { onDiarySort(DiarySort.AddedDesc) },
                        )
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_added_asc),
                            selected = state.diarySort == DiarySort.AddedAsc,
                            onClick = { onDiarySort(DiarySort.AddedAsc) },
                        )
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_watched_desc),
                            selected = state.diarySort == DiarySort.WatchedDesc,
                            onClick = { onDiarySort(DiarySort.WatchedDesc) },
                        )
                        SortMenuItem(
                            label = stringResource(R.string.collection_sort_watched_asc),
                            selected = state.diarySort == DiarySort.WatchedAsc,
                            onClick = { onDiarySort(DiarySort.WatchedAsc) },
                        )
                    }
                }
            }
        }
        if (!canSelect) return@Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.selectionMode) {
                if (state.selectedIds.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.collection_selected_count,
                            state.selectedIds.size,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = textSize,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                if (metrics.isTablet) {
                    TextButton(onClick = onSelectAll) {
                        Icon(
                            Icons.Outlined.SelectAll,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                        )
                        Text(
                            "  " + stringResource(R.string.collection_select_all),
                            fontSize = textSize,
                        )
                    }
                    TextButton(
                        onClick = onDelete,
                        enabled = state.selectedIds.isNotEmpty() && !state.mutating,
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(iconSize),
                        )
                        Text(
                            "  " + stringResource(R.string.collection_delete),
                            fontSize = textSize,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onClearSelection) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                        )
                        Text(
                            "  " + stringResource(R.string.collection_cancel_select),
                            fontSize = textSize,
                        )
                    }
                } else {
                    IconButton(onClick = onSelectAll) {
                        Icon(
                            Icons.Outlined.SelectAll,
                            contentDescription = stringResource(R.string.collection_select_all),
                            modifier = Modifier.size(iconSize),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = state.selectedIds.isNotEmpty() && !state.mutating,
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.collection_delete),
                            tint = if (state.selectedIds.isNotEmpty()) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(iconSize),
                        )
                    }
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.collection_cancel_select),
                            modifier = Modifier.size(iconSize),
                        )
                    }
                }
            } else if (metrics.isTablet) {
                TextButton(onClick = onEnterSelect) {
                    Icon(
                        Icons.Outlined.CheckBox,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                    Text(
                        "  " + stringResource(R.string.collection_select),
                        fontSize = textSize,
                    )
                }
            } else {
                IconButton(onClick = onEnterSelect) {
                    Icon(
                        Icons.Outlined.CheckBox,
                        contentDescription = stringResource(R.string.collection_select),
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryStatusChips(
    statusSelected: String?,
    reactionSelected: String?,
    onStatusSelect: (String?) -> Unit,
    onReactionSelect: (String?) -> Unit,
    metrics: HomeMetrics,
    modifier: Modifier = Modifier,
) {
    val yoho = YohoThemeExtras.colors
    val statusColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = yoho.chipSelected,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = yoho.chipUnselected,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val likeGreen = Color(0xFF22C55E)
    val reactionSelectedColor = when (reactionSelected) {
        DiaryFilterLike -> likeGreen
        DiaryFilterDislike -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    val reactionColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = reactionSelectedColor,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = yoho.chipUnselected,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val statusOptions = buildList {
        add(null to stringResource(R.string.collection_status_all))
        DiaryStatusLabels.forEach { add(it to it) }
    }
    val reactionAll = stringResource(R.string.collection_reaction_all)
    val reactionOptions = listOf(
        null to reactionAll,
        DiaryFilterLike to stringResource(R.string.collection_filter_like),
        DiaryFilterDislike to stringResource(R.string.collection_filter_dislike),
    )
    val reactionLabel = reactionOptions.first { it.first == reactionSelected }.second
    val textSize = if (metrics.isTablet) metrics.searchTextSize else metrics.chipTextSize
    val chipHeight = if (metrics.isTablet) 44.dp else 32.dp
    val gap = if (metrics.isTablet) 10.dp else 8.dp
    val menuTextSize = if (metrics.isTablet) metrics.cardMetaSize else metrics.searchTextSize
    var reactionOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            FilterChip(
                selected = reactionSelected != null,
                onClick = { reactionOpen = true },
                label = {
                    Text(
                        text = reactionLabel,
                        fontSize = textSize,
                        fontWeight = if (metrics.isTablet) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(if (metrics.isTablet) 22.dp else 18.dp),
                    )
                },
                colors = reactionColors,
                border = null,
                modifier = Modifier.height(chipHeight),
            )
            DropdownMenu(
                expanded = reactionOpen,
                onDismissRequest = { reactionOpen = false },
                modifier = Modifier.widthIn(min = if (metrics.isTablet) 260.dp else 200.dp),
            ) {
                reactionOptions.forEach { (key, label) ->
                    val active = reactionSelected == key
                    val accent = when (key) {
                        DiaryFilterLike -> likeGreen
                        DiaryFilterDislike -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontSize = menuTextSize,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) accent else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            onReactionSelect(key)
                            reactionOpen = false
                        },
                        trailingIcon = if (active) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(if (metrics.isTablet) 24.dp else 20.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            statusOptions.forEach { (key, label) ->
                FilterChip(
                    selected = statusSelected == key,
                    onClick = { onStatusSelect(key) },
                    label = {
                        Text(
                            text = label,
                            fontSize = textSize,
                            fontWeight = if (metrics.isTablet) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.padding(
                                horizontal = if (metrics.isTablet) 4.dp else 0.dp,
                                vertical = if (metrics.isTablet) 2.dp else 0.dp,
                            ),
                        )
                    },
                    colors = statusColors,
                    border = null,
                    modifier = Modifier.height(chipHeight),
                )
            }
        }
    }
}

private fun formatWatchedDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val instant = runCatching { java.time.Instant.parse(raw) }.getOrNull() ?: return null
    val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return date.format(
        java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("ru")),
    )
}
