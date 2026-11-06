package com.spywer.yoho.ui.search

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.ui.components.ContentErrorPlaceholder
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.components.YohoBackButton
import com.spywer.yoho.ui.detail.TitleDetailActivity
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.theme.YohoThemeExtras
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    showBack: Boolean = true,
    title: String? = null,
    /** false, пока поверх каталога открыта карточка тайтла. */
    restoreSearchFocus: Boolean = true,
    onOpenTitle: ((idKp: Int?, idTmdb: Int?, mediaType: String?) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }

    BackHandler(enabled = showFilterSheet || showBack) {
        when {
            showFilterSheet -> showFilterSheet = false
            else -> onBack()
        }
    }

    LaunchedEffect(restoreSearchFocus) {
        if (!restoreSearchFocus || state.query.isBlank()) return@LaunchedEffect
        delay(50)
        runCatching { searchFocus.requestFocus() }
    }

    fun openTitle(item: TitleDto) {
        if (item.idKp == null && item.idTmdb == null) return
        onOpenTitle?.invoke(item.idKp, item.idTmdb, item.mediaType)
            ?: context.startActivity(
                TitleDetailActivity.intent(context, item.idKp, item.idTmdb, item.mediaType),
            )
    }

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                SearchContentColumn(metrics) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(
                                horizontal = metrics.screenPadding,
                                vertical = metrics.sectionGap,
                            ),
                    ) {
                        if (!title.isNullOrBlank() && !showBack) {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = metrics.sectionTitleSize,
                                lineHeight = metrics.lineOf(metrics.sectionTitleSize),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = metrics.sectionGap),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(metrics.blockGap),
                        ) {
                            if (showBack) {
                                YohoBackButton(onClick = onBack)
                            }
                            SearchBar(
                                query = state.query,
                                onQueryChange = viewModel::onQueryChange,
                                onClear = viewModel::clearQuery,
                                height = metrics.searchHeight,
                                textSize = metrics.searchTextSize,
                                focusRequester = searchFocus,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(metrics.sectionGap))
                        SearchControlsRow(
                            state = state,
                            textSize = metrics.chipTextSize,
                            onToggleType = viewModel::toggleType,
                            onRemoveChip = viewModel::removeChip,
                            onOpenFilters = { showFilterSheet = true },
                            onResetFilters = viewModel::clearFilters,
                        )
                    }
                }
            },
        ) { padding ->
            if (showFilterSheet) {
                FilterSheet(
                    facets = state.facets,
                    catalog = state.filterCatalog,
                    filters = state.filters,
                    onDismiss = { showFilterSheet = false },
                    onUpdate = viewModel::updateFilters,
                    onClear = viewModel::clearFilters,
                )
            }
            PullToRefreshBox(
                // Не дублировать синий CircularProgress при пустой выдаче.
                isRefreshing = state.loading && state.results.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SearchResultsPane(
                    state = state,
                    metrics = metrics,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::refresh,
                    onOpenTitle = ::openTitle,
                )
            }
        }
    }
}

@Composable
private fun SearchContentColumn(
    metrics: HomeMetrics,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (metrics.contentMaxWidth != Dp.Unspecified) {
                        Modifier.widthIn(max = metrics.contentMaxWidth)
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchControlsRow(
    state: SearchUiState,
    textSize: TextUnit,
    onToggleType: (String) -> Unit,
    onRemoveChip: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onResetFilters: () -> Unit,
) {
    val yoho = YohoThemeExtras.colors
    val types = state.facets.types.ifEmpty {
        DefaultSearchTypes.map {
            com.spywer.yoho.data.remote.FacetItemDto(it.value, it.label)
        }
    }
    val selectedTypes = state.filters.types
    val activeGroups = state.filters.activeGroups(state.facets, state.filterCatalog)
    val filtersActive = state.filters.hasAdvanced
    var typeMenu by remember { mutableStateOf(false) }

    val typeLabel = when {
        selectedTypes.isEmpty() -> types.find { it.value == "all" }?.label ?: "Все"
        else -> selectedTypes.mapNotNull { value ->
            types.find { it.value == value }?.label ?: value
        }.joinToString(", ")
    }

    val metrics = LocalHomeMetrics.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.blockGap),
        verticalArrangement = Arrangement.spacedBy(metrics.blockGap),
    ) {
        Box {
            FilterChip(
                selected = selectedTypes.isNotEmpty(),
                onClick = { typeMenu = true },
                label = {
                    Text(
                        text = typeLabel,
                        fontSize = textSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = yoho.chipSelected,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = yoho.chipUnselected,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = null,
            )
            DropdownMenu(
                expanded = typeMenu,
                onDismissRequest = { typeMenu = false },
            ) {
                types.forEach { opt ->
                    val value = opt.value ?: return@forEach
                    val checked = if (value == "all") {
                        selectedTypes.isEmpty()
                    } else {
                        value in selectedTypes
                    }
                    DropdownMenuItem(
                        text = {
                            Text(opt.label ?: value, fontSize = textSize)
                        },
                        onClick = { onToggleType(value) },
                        trailingIcon = {
                            if (checked) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                }
            }
        }
        activeGroups.forEach { group ->
            Text(
                text = group.title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = textSize,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 2.dp),
            )
            group.chips.forEach { chip ->
                FilterChip(
                    selected = true,
                    onClick = { onRemoveChip(chip.id) },
                    label = { Text(chip.label, fontSize = textSize) },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = yoho.chipSelected,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    border = null,
                )
            }
        }
        FilterChip(
            selected = false,
            onClick = onOpenFilters,
            label = {
                Text(
                    text = stringResource(R.string.filter_add),
                    fontSize = textSize,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.primary,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.primary,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                borderWidth = 1.5.dp,
            ),
        )
        if (filtersActive) {
            FilterChip(
                selected = false,
                onClick = onResetFilters,
                label = {
                    Text(
                        text = stringResource(R.string.filters_reset),
                        fontSize = textSize,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.error,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = MaterialTheme.colorScheme.error,
                    selectedBorderColor = MaterialTheme.colorScheme.error,
                    borderWidth = 1.5.dp,
                ),
            )
        }
    }
}

@Composable
private fun SearchResultsPane(
    state: SearchUiState,
    metrics: HomeMetrics,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenTitle: (TitleDto) -> Unit,
) {
    SearchContentColumn(metrics) {
        when {
            state.loading && state.results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.error != null && state.results.isEmpty() -> {
                ContentErrorPlaceholder(
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            state.results.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.search_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = metrics.searchTextSize,
                    )
                }
            }

            else -> {
                val gridState = rememberLazyGridState()
                val shouldLoadMore by remember(state.hasMore, state.loadingMore, state.loading) {
                    derivedStateOf {
                        val info = gridState.layoutInfo
                        val total = info.totalItemsCount
                        if (!state.hasMore || state.loadingMore || state.loading || total == 0) {
                            return@derivedStateOf false
                        }
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
                        last >= total - 6
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) onLoadMore()
                }
                Column(Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.search_found, state.total),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = metrics.cardMetaSize,
                        modifier = Modifier.padding(
                            horizontal = metrics.screenPadding,
                            vertical = 8.dp,
                        ),
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(metrics.posterLarge),
                        state = gridState,
                        contentPadding = PaddingValues(
                            horizontal = metrics.screenPadding,
                            vertical = 8.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
                        verticalArrangement = Arrangement.spacedBy(metrics.cardGap),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            state.results,
                            key = { it.idKp ?: it.idTmdb ?: it.hashCode() },
                        ) { item ->
                            PosterCard(
                                item = item,
                                onClick = { onOpenTitle(item) },
                            )
                        }
                        if (state.hasMore || state.loadingMore) {
                            item(
                                key = "search_load_more",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.loadingMore) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp),
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

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    height: Dp,
    textSize: TextUnit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            maxLines = 1,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = textSize,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (LocalHomeMetrics.current.isTablet) {
                                R.string.search_hint
                            } else {
                                R.string.search_hint_compact
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = textSize,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
        if (focused) {
            IconButton(
                onClick = {
                    onClear()
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
