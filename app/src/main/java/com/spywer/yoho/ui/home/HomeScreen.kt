package com.spywer.yoho.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.components.TrailerCard
import com.spywer.yoho.ui.components.TrailerPlayerDialog
import com.spywer.yoho.ui.components.youtubeEmbedUrl
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.detail.TitleDetailActivity
import com.spywer.yoho.ui.search.SearchActivity
import com.spywer.yoho.ui.theme.YohoThemeExtras
import com.spywer.yoho.data.remote.displayTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenCatalog: (() -> Unit)? = null,
    onOpenTitle: ((idKp: Int?, idTmdb: Int?, mediaType: String?) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    val context = LocalContext.current

    fun openSearch() {
        onOpenCatalog?.invoke()
            ?: context.startActivity(SearchActivity.intent(context))
    }

    fun openTitle(item: TitleDto) {
        if (item.idKp == null && item.idTmdb == null) return
        onOpenTitle?.invoke(item.idKp, item.idTmdb, item.mediaType)
            ?: context.startActivity(
                TitleDetailActivity.intent(context, item.idKp, item.idTmdb, item.mediaType),
            )
    }

    var playingTrailer by remember {
        mutableStateOf<Pair<String, String?>?>(null)
    }
    playingTrailer?.let { (url, title) ->
        TrailerPlayerDialog(
            urlOrKey = url,
            title = title,
            onDismiss = { playingTrailer = null },
        )
    }

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ContentColumn(metrics) {
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
                        Text(
                            text = "YH",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = metrics.brandSize,
                            lineHeight = metrics.lineOf(metrics.brandSize),
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(metrics.sectionGap))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(metrics.blockGap),
                        ) {
                            HomeSearchEntry(
                                height = metrics.searchHeight,
                                textSize = metrics.searchTextSize,
                                onClick = ::openSearch,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = ::openSearch,
                                modifier = Modifier
                                    .size(metrics.searchHeight)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = stringResource(R.string.filters),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(metrics.sectionGap))
                        CategoryChips(
                            selected = state.filter,
                            onSelect = viewModel::setFilter,
                            textSize = metrics.chipTextSize,
                        )
                    }
                }
            },
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.load(refresh = true) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    state.error != null && !state.hasContent -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.error_load),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = metrics.searchTextSize,
                            )
                            Text(
                                text = state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = metrics.cardMetaSize,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            TextButton(onClick = { viewModel.load() }) {
                                Text(stringResource(R.string.retry), fontSize = metrics.searchTextSize)
                            }
                        }
                    }

                    else -> {
                        ContentColumn(metrics) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    bottom = if (metrics.isTablet) 32.dp else 20.dp,
                                ),
                            ) {
                                item {
                                    PosterSection(
                                        title = stringResource(R.string.recommendations),
                                        items = state.recommendations,
                                        cardWidth = metrics.posterLarge,
                                        gap = metrics.cardGap,
                                        padding = metrics.screenPadding,
                                        titleSize = metrics.sectionTitleSize,
                                        onOpen = ::openTitle,
                                    )
                                }
                                item {
                                    PosterSection(
                                        title = stringResource(R.string.now_popular),
                                        items = state.nowPopular,
                                        cardWidth = metrics.posterLarge,
                                        gap = metrics.cardGap,
                                        padding = metrics.screenPadding,
                                        titleSize = metrics.sectionTitleSize,
                                        onOpen = ::openTitle,
                                    )
                                }
                                if (state.isAnimeLayout) {
                                    // Как /anime: сезон → за всё время → топ 100 (без трейлеров/новинок).
                                    val seasonLabel = state.seasonLabel
                                    if (seasonLabel != null && state.seasonPopular.isNotEmpty()) {
                                        item {
                                            PosterSection(
                                                title = stringResource(
                                                    R.string.anime_season_popular,
                                                    seasonLabel,
                                                ),
                                                items = state.seasonPopular,
                                                cardWidth = metrics.posterLarge,
                                                gap = metrics.cardGap,
                                                padding = metrics.screenPadding,
                                                titleSize = metrics.sectionTitleSize,
                                                onOpen = ::openTitle,
                                            )
                                        }
                                    }
                                    item {
                                        PosterSection(
                                            title = stringResource(R.string.popular_all_time),
                                            items = state.popular,
                                            cardWidth = metrics.posterLarge,
                                            gap = metrics.cardGap,
                                            padding = metrics.screenPadding,
                                            titleSize = metrics.sectionTitleSize,
                                            onOpen = ::openTitle,
                                        )
                                    }
                                    item {
                                        PosterSection(
                                            title = stringResource(R.string.anime_top100),
                                            items = state.animeTop,
                                            cardWidth = metrics.posterLarge,
                                            gap = metrics.cardGap,
                                            padding = metrics.screenPadding,
                                            titleSize = metrics.sectionTitleSize,
                                            onOpen = ::openTitle,
                                        )
                                    }
                                } else {
                                    item {
                                        TrailerSection(
                                            title = stringResource(R.string.trailers),
                                            items = state.trailers,
                                            cardWidth = metrics.trailer,
                                            gap = metrics.cardGap,
                                            padding = metrics.screenPadding,
                                            titleSize = metrics.sectionTitleSize,
                                            onOpen = ::openTitle,
                                            onPlayTrailer = { item ->
                                                val url = item.trailer?.url
                                                if (url != null && youtubeEmbedUrl(url) != null) {
                                                    playingTrailer = url to item.displayTitle()
                                                } else {
                                                    openTitle(item)
                                                }
                                            },
                                        )
                                    }
                                    if (metrics.isTablet) {
                                        item {
                                            TabletPosterPair(
                                                leftTitle = stringResource(R.string.popular),
                                                leftItems = state.popular,
                                                rightTitle = stringResource(R.string.newcomers),
                                                rightItems = state.newcomers,
                                                metrics = metrics,
                                                onOpen = ::openTitle,
                                            )
                                        }
                                    } else {
                                        item {
                                            PosterSection(
                                                title = stringResource(R.string.popular),
                                                items = state.popular,
                                                cardWidth = metrics.posterLarge,
                                                gap = metrics.cardGap,
                                                padding = metrics.screenPadding,
                                                titleSize = metrics.sectionTitleSize,
                                                onOpen = ::openTitle,
                                            )
                                        }
                                        item {
                                            PosterSection(
                                                title = stringResource(R.string.newcomers),
                                                items = state.newcomers,
                                                cardWidth = metrics.poster,
                                                gap = metrics.cardGap,
                                                padding = metrics.screenPadding,
                                                titleSize = metrics.sectionTitleSize,
                                                onOpen = ::openTitle,
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
private fun HomeSearchEntry(
    height: Dp,
    textSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.search_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = textSize,
        )
    }
}

@Composable
private fun ContentColumn(
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

@Composable
private fun CategoryChips(
    selected: ContentFilter,
    onSelect: (ContentFilter) -> Unit,
    textSize: TextUnit,
) {
    val yoho = YohoThemeExtras.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContentFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = { Text(filter.labelRes, fontSize = textSize) },
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

@Composable
private fun TabletPosterPair(
    leftTitle: String,
    leftItems: List<TitleDto>,
    rightTitle: String,
    rightItems: List<TitleDto>,
    metrics: HomeMetrics,
    onOpen: (TitleDto) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
    ) {
        PosterSection(
            title = leftTitle,
            items = leftItems,
            cardWidth = metrics.posterLarge,
            gap = metrics.cardGap,
            padding = metrics.screenPadding,
            titleSize = metrics.sectionTitleSize,
            modifier = Modifier.weight(1f),
            edgePaddingEnd = metrics.cardGap / 2,
            onOpen = onOpen,
        )
        PosterSection(
            title = rightTitle,
            items = rightItems,
            cardWidth = metrics.poster,
            gap = metrics.cardGap,
            padding = metrics.screenPadding,
            titleSize = metrics.sectionTitleSize,
            modifier = Modifier.weight(1f),
            edgePaddingStart = metrics.cardGap / 2,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun PosterSection(
    title: String,
    items: List<TitleDto>,
    cardWidth: Dp,
    gap: Dp,
    padding: Dp,
    titleSize: TextUnit,
    onOpen: (TitleDto) -> Unit,
    modifier: Modifier = Modifier,
    edgePaddingStart: Dp = padding,
    edgePaddingEnd: Dp = padding,
) {
    if (items.isEmpty()) return
    val metrics = LocalHomeMetrics.current
    Column(modifier.padding(top = metrics.blockGap)) {
        SectionTitle(title, titleSize, edgePaddingStart)
        LazyRow(
            contentPadding = PaddingValues(start = edgePaddingStart, end = edgePaddingEnd),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            // index в ключе: в /api/anime один kpId может повториться в ленте.
            itemsIndexed(
                items,
                key = { index, item -> "${item.idKp ?: item.idTmdb ?: item.hashCode()}-$index" },
            ) { _, item ->
                PosterCard(
                    item = item,
                    modifier = Modifier.width(cardWidth),
                    onClick = { onOpen(item) },
                )
            }
        }
    }
}

@Composable
private fun TrailerSection(
    title: String,
    items: List<TitleDto>,
    cardWidth: Dp,
    gap: Dp,
    padding: Dp,
    titleSize: TextUnit,
    onOpen: (TitleDto) -> Unit,
    onPlayTrailer: (TitleDto) -> Unit = onOpen,
) {
    if (items.isEmpty()) return
    val metrics = LocalHomeMetrics.current
    Column(Modifier.padding(top = metrics.blockGap)) {
        SectionTitle(title, titleSize, padding)
        LazyRow(
            contentPadding = PaddingValues(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            itemsIndexed(
                items,
                key = { index, item -> "tr-${item.idKp ?: item.idTmdb}-$index" },
            ) { _, item ->
                TrailerCard(
                    item = item,
                    modifier = Modifier.width(cardWidth),
                    onClick = { onPlayTrailer(item) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    size: TextUnit,
    padding: Dp,
) {
    val metrics = LocalHomeMetrics.current
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = size,
        lineHeight = metrics.lineOf(size),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = padding,
            end = padding,
            top = metrics.sectionGap,
            bottom = metrics.blockGap,
        ),
    )
}
