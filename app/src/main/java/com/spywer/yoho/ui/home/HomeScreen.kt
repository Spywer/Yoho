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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.detail.TitleDetailActivity
import com.spywer.yoho.ui.search.SearchActivity
import com.spywer.yoho.ui.theme.YohoThemeExtras

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

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ContentColumn(metrics) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = metrics.screenPadding, vertical = 12.dp),
                    ) {
                        Text(
                            text = "YH",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = metrics.brandSize,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        Spacer(Modifier.height(12.dp))
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
                                contentPadding = PaddingValues(bottom = 32.dp),
                            ) {
                                item {
                                    PosterSection(
                                        title = stringResource(R.string.recommendations),
                                        items = state.filteredRecommendations,
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
                                item {
                                    TrailerSection(
                                        title = stringResource(R.string.trailers),
                                        items = state.trailers,
                                        cardWidth = metrics.trailer,
                                        gap = metrics.cardGap,
                                        padding = metrics.screenPadding,
                                        titleSize = metrics.sectionTitleSize,
                                        onOpen = ::openTitle,
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
    Column(modifier.padding(top = 8.dp)) {
        SectionTitle(title, titleSize, edgePaddingStart)
        LazyRow(
            contentPadding = PaddingValues(start = edgePaddingStart, end = edgePaddingEnd),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(items, key = { it.idKp ?: it.idTmdb ?: it.hashCode() }) { item ->
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
) {
    if (items.isEmpty()) return
    Column(Modifier.padding(top = 8.dp)) {
        SectionTitle(title, titleSize, padding)
        LazyRow(
            contentPadding = PaddingValues(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(items, key = { "tr-${it.idKp ?: it.idTmdb}" }) { item ->
                TrailerCard(
                    item = item,
                    modifier = Modifier.width(cardWidth),
                    onClick = { onOpen(item) },
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
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = padding, end = padding, top = 12.dp, bottom = 12.dp),
    )
}
