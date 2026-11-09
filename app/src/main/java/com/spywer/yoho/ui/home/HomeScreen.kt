package com.spywer.yoho.ui.home

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.spywer.yoho.MainActivity
import com.spywer.yoho.R
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.MeActivityRequest
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SearchPersonDto
import com.spywer.yoho.data.remote.SessionCookieJar
import com.spywer.yoho.data.remote.SuggestItemDto
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.absMediaUrl
import com.spywer.yoho.data.remote.displayTitle
import com.spywer.yoho.ui.components.ContentErrorPlaceholder
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.components.TrailerCard
import com.spywer.yoho.ui.detail.PersonSheet
import com.spywer.yoho.ui.detail.TitleDetailActivity
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.search.genreLabelsOf
import com.spywer.yoho.ui.theme.YohoThemeExtras
import com.spywer.yoho.ui.trailer.openTrailerPlayer
import com.spywer.yoho.ui.trailer.youtubeEmbedUrl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val keyboard = LocalSoftwareKeyboardController.current

    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var titles by remember { mutableStateOf<List<SuggestItemDto>>(emptyList()) }
    var persons by remember { mutableStateOf<List<SearchPersonDto>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var selectedPersonId by remember { mutableStateOf<Int?>(null) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favoriteBusyIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val trimmedQuery = query.trim()

    val searchFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val session by SessionRepository.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val headerOffsetPx = remember { mutableFloatStateOf(0f) }

    // Корректировка смещения при изменении высоты хедера
    var prevHeaderHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(headerHeightPx) {
        val oldH = prevHeaderHeightPx
        val newH = headerHeightPx
        if (oldH > 0f && newH > 0f && newH != oldH) {
            val offset = headerOffsetPx.floatValue
            if (offset <= -oldH + 1f) {
                headerOffsetPx.floatValue = -newH
            } else {
                headerOffsetPx.floatValue = 0f
            }
        }
        prevHeaderHeightPx = newH
    }

    val headerNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val h = headerHeightPx
                if (h <= 0f) return Offset.Zero

                val delta = available.y
                if (delta == 0f) return Offset.Zero

                if (!listState.canScrollForward
                    && !listState.canScrollBackward
                    && headerOffsetPx.floatValue >= 0f
                ) {
                    return Offset.Zero
                }

                val oldOffset = headerOffsetPx.floatValue
                val newOffset = (oldOffset + delta).coerceIn(-h, 0f)
                val consumed = newOffset - oldOffset
                headerOffsetPx.floatValue = newOffset
                return Offset(0f, consumed)
            }
        }
    }

    fun openCatalog() {
        onOpenCatalog?.invoke()
    }

    fun openTitle(item: TitleDto) {
        if (item.idKp == null && item.idTmdb == null) return
        onOpenTitle?.invoke(item.idKp, item.idTmdb, item.mediaType)
            ?: context.startActivity(
                TitleDetailActivity.intent(context, item.idKp, item.idTmdb, item.mediaType),
            )
    }

    fun openSuggest(item: SuggestItemDto) {
        val kp = item.kpId
        val tmdb = item.tmdbId?.takeIf { it > 0 }
        if (kp == null && tmdb == null) return
        searchActive = false
        query = ""
        titles = emptyList()
        persons = emptyList()
        keyboard?.hide()
        onOpenTitle?.invoke(kp, tmdb, item.displayType)
            ?: context.startActivity(
                TitleDetailActivity.intent(context, kp, tmdb, item.displayType),
            )
    }

    fun closeSearch() {
        searchActive = false
        query = ""
        titles = emptyList()
        persons = emptyList()
        searching = false
        keyboard?.hide()
    }

    BackHandler(enabled = searchActive) { closeSearch() }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }

    LaunchedEffect(searchActive, session.isLoggedIn) {
        if (!searchActive) return@LaunchedEffect
        if (!session.isLoggedIn && !SessionCookieJar.get().hasSessionCookie()) {
            favoriteIds = emptySet()
            return@LaunchedEffect
        }
        runCatching { NetworkModule.api.sync() }
            .onSuccess { sync ->
                favoriteIds = sync.favorites.mapNotNull { fav ->
                    fav.id?.takeIf { it.isNotBlank() }
                        ?: fav.contentId?.takeIf { it > 0 }?.let { "kp:$it" }
                }.toSet()
            }
    }

    @SuppressLint("LocalContextGetResourceValueCall")
    fun toggleSuggestFavorite(item: SuggestItemDto) {
        val itemId = item.favoriteItemId() ?: return
        val activityItem = item.toFavoriteActivityItem() ?: return
        if (itemId in favoriteBusyIds) return

        if (!session.isLoggedIn && !SessionCookieJar.get().hasSessionCookie()) {
            Toast.makeText(
                context,
                context.getString(R.string.detail_favorite_login),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val adding = itemId !in favoriteIds
        scope.launch {
            favoriteBusyIds = favoriteBusyIds + itemId
            runCatching {
                val resp = NetworkModule.api.meActivity(
                    MeActivityRequest(
                        op = if (adding) "addFavorite" else "removeFavorite",
                        item = activityItem,
                    ),
                )
                when {
                    resp.isSuccessful || resp.code() == 204 -> Unit
                    resp.code() == 401 || resp.code() == 403 ->
                        error(context.getString(R.string.detail_favorite_login))
                    else -> error("HTTP ${resp.code()}")
                }
            }.onSuccess {
                favoriteIds = if (adding) favoriteIds + itemId else favoriteIds - itemId
            }.onFailure { e ->
                Toast.makeText(
                    context,
                    e.message ?: context.getString(R.string.detail_favorite_error),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            favoriteBusyIds = favoriteBusyIds - itemId
        }
    }

    LaunchedEffect(query, searchActive) {
        if (!searchActive) return@LaunchedEffect
        val q = query.trim()
        if (q.length < 2) {
            titles = emptyList()
            persons = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(280)
        if (query.trim() != q) return@LaunchedEffect
        coroutineScope {
            val titlesDef = async {
                runCatching { NetworkModule.api.suggest(q, limit = 10).results }
                    .getOrDefault(emptyList())
            }
            val personsDef = async {
                runCatching { NetworkModule.api.searchPersons(q).persons }
                    .getOrDefault(emptyList())
            }
            titles = titlesDef.await()
            persons = personsDef.await()
        }
        searching = false
    }

    LaunchedEffect(state.filter) {
        listState.scrollToItem(0)
        headerOffsetPx.floatValue = 0f
    }

    LaunchedEffect(searchActive) {
        if (searchActive) headerOffsetPx.floatValue = 0f
    }

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            selectedPersonId?.let { personId ->
                PersonSheet(
                    personId = personId,
                    onDismiss = { selectedPersonId = null },
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.load(refresh = true) },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(headerNestedScrollConnection),
                ) {
                    when {
                        state.loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        state.error != null && !state.hasContent -> {
                            ContentErrorPlaceholder(
                                onRetry = { viewModel.load() },
                            )
                        }

                        else -> {
                            ContentColumn(metrics) {
                                val density = LocalDensity.current
                                val statusBarHeight = with(density) {
                                    ViewCompat.getRootWindowInsets(LocalView.current)
                                        ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
                                        ?.toFloat() ?: 0f
                                }
                                val headerTopPadding = with(density) {
                                    ((headerHeightPx + headerOffsetPx.floatValue + statusBarHeight).coerceAtLeast(0f)).toDp()
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        top = headerTopPadding,
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
                                                        context.openTrailerPlayer(
                                                            urlOrKey = url,
                                                            title = item.displayTitle(),
                                                        )
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

                if (searchActive && trimmedQuery.length >= 2) {
                    val searchOverlayTop = with(LocalDensity.current) {
                        (headerHeightPx + headerOffsetPx.floatValue).coerceAtLeast(0f).toDp()
                    }

                    HomeSearchResults(
                        titles = titles,
                        persons = persons,
                        loading = searching,
                        textSize = metrics.searchTextSize,
                        favoriteIds = favoriteIds,
                        favoriteBusyIds = favoriteBusyIds,
                        onPickTitle = ::openSuggest,
                        onToggleFavorite = ::toggleSuggestFavorite,
                        onPickPerson = { id ->
                            val person = persons.find { it.id == id }
                            val name = person?.name?.takeIf { it.isNotBlank() }
                                ?: return@HomeSearchResults
                            val kp = person.id?.toString().takeIf { it != null }
                            searchActive = false
                            query = ""
                            titles = emptyList()
                            persons = emptyList()
                            keyboard?.hide()
                            kp?.let { actorKp ->
                                context.startActivity(
                                    MainActivity.intentOpenCatalogWithActor(context, name, actorKp),
                                )
                            }
                        },
                        onPersonSheet = { id ->
                            selectedPersonId = id
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
                            .padding(top = searchOverlayTop),
                    )
                }
            }

            // Header
            ContentColumn(metrics) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .zIndex(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = headerOffsetPx.floatValue
                                alpha = if (headerHeightPx > 0f) {
                                    (1f + translationY / headerHeightPx).coerceIn(0f, 1f)
                                } else {
                                    1f
                                }
                            }
                            .background(MaterialTheme.colorScheme.background)
                            .statusBarsPadding()
                            .onSizeChanged { size ->
                                headerHeightPx = size.height.toFloat()
                            }
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
                            horizontalArrangement = Arrangement.spacedBy(
                                if (metrics.isTablet) metrics.blockGap else 10.dp,
                            ),
                        ) {
                            if (searchActive) {
                                HomeSearchField(
                                    query = query,
                                    onQueryChange = { query = it },
                                    onClear = {
                                        if (query.isNotEmpty()) {
                                            query = ""
                                            titles = emptyList()
                                            persons = emptyList()
                                        } else {
                                            closeSearch()
                                        }
                                    },
                                    height = metrics.searchHeight,
                                    textSize = metrics.searchTextSize,
                                    focusRequester = searchFocus,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                HomeSearchEntry(
                                    height = metrics.searchHeight,
                                    textSize = metrics.searchTextSize,
                                    onClick = { searchActive = true },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(metrics.searchHeight)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(onClick = ::openCatalog),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = stringResource(R.string.filters),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(
                                        if (metrics.isTablet) 28.dp else 22.dp,
                                    ),
                                )
                            }
                        }

                        val hideChips = searchActive && trimmedQuery.length >= 2
                        if (!hideChips) {
                            Spacer(Modifier.height(metrics.sectionGap))
                            CategoryChips(
                                selected = state.filter,
                                onSelect = viewModel::setFilter,
                                textSize = metrics.chipTextSize,
                            )
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
}

@Composable
private fun HomeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    height: Dp,
    textSize: TextUnit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
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
                .focusRequester(focusRequester),
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
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeSearchResults(
    titles: List<SuggestItemDto>,
    persons: List<SearchPersonDto>,
    loading: Boolean,
    textSize: TextUnit,
    favoriteIds: Set<String>,
    favoriteBusyIds: Set<String>,
    onPickTitle: (SuggestItemDto) -> Unit,
    onToggleFavorite: (SuggestItemDto) -> Unit,
    onPickPerson: (Int) -> Unit,
    onPersonSheet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalHomeMetrics.current
    val sectionSize = if (metrics.isTablet) metrics.sectionTitleSize * 0.7f else textSize

    ContentColumn(metrics) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = metrics.screenPadding),
        ) {
            when {
                loading && titles.isEmpty() && persons.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                titles.isEmpty() && persons.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.search_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (metrics.isTablet) metrics.cardTitleSize else textSize,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        if (titles.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.search_section_titles),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = sectionSize,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = metrics.blockGap),
                                )
                            }

                            items(titles, key = { it.key ?: "${it.kpId}_${it.title}" }) { item ->
                                val favId = item.favoriteItemId()
                                SuggestTitleRow(
                                    item = item,
                                    isFavorite = favId != null && favId in favoriteIds,
                                    favoriteBusy = favId != null && favId in favoriteBusyIds,
                                    onClick = { onPickTitle(item) },
                                    onToggleFavorite = { onToggleFavorite(item) },
                                )
                            }
                        }

                        if (persons.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.search_section_persons),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = sectionSize,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        top = metrics.sectionGap,
                                        bottom = metrics.blockGap,
                                    ),
                                )
                            }

                            items(persons, key = { it.id ?: it.name.hashCode() }) { person ->
                                val id = person.id ?: return@items
                                val name = person.name?.takeIf { it.isNotBlank() } ?: return@items
                                SuggestPersonRow(
                                    name = name,
                                    originalName = person.originalName,
                                    photoUrl = person.photoUrl,
                                    onClick = { onPickPerson(id) },
                                    onImageClick = { onPersonSheet(id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestTitleRow(
    item: SuggestItemDto,
    isFavorite: Boolean,
    favoriteBusy: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val metrics = LocalHomeMetrics.current
    val colors = YohoThemeExtras.colors

    val posterW = if (metrics.isTablet) 84.dp else 48.dp
    val posterH = if (metrics.isTablet) 118.dp else 68.dp
    val titleSize = if (metrics.isTablet) metrics.cardTitleSize else metrics.searchTextSize
    val metaSize = metrics.cardMetaSize
    val favHit = if (metrics.isTablet) 56.dp else 36.dp
    val favIcon = if (metrics.isTablet) 36.dp else 24.dp

    val genres = remember(item.genres, metrics.isTablet) {
        genreLabelsOf(item.genres, limit = if (metrics.isTablet) 4 else 3)
    }
    val typeYear = listOfNotNull(
        item.typeLabelRu().takeIf { it.isNotBlank() },
        item.year?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    val metaInline = if (metrics.isTablet) {
        listOfNotNull(
            typeYear.takeIf { it.isNotBlank() },
            genres.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
    } else {
        typeYear
    }

    val canFavorite = item.favoriteItemId() != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = if (metrics.isTablet) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.displayPoster.absMediaUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(posterW, posterH)
                .clip(RoundedCornerShape(if (metrics.isTablet) 10.dp else 6.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        )

        Spacer(Modifier.width(if (metrics.isTablet) 16.dp else 12.dp))

        Column(Modifier.weight(1f)) {
            val ruTitle = item.title.orEmpty()
            val original = item.originalTitle
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals(ruTitle, ignoreCase = true) }

            if (original == null) {
                Text(
                    text = ruTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = titleSize,
                    lineHeight = metrics.lineOf(titleSize),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                val onBg = MaterialTheme.colorScheme.onBackground
                val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

                val annotatedTitle = remember(ruTitle, original, titleSize, onBg, onSurfaceVar) {
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = onBg,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = titleSize,
                            ),
                        ) {
                            append(ruTitle)
                        }
                        withStyle(
                            SpanStyle(
                                color = onSurfaceVar.copy(alpha = 0.65f),
                                fontWeight = FontWeight.Normal,
                                fontSize = titleSize,
                            ),
                        ) {
                            append(" — ")
                            append(original)
                        }
                    }
                }

                Text(
                    text = annotatedTitle,
                    lineHeight = metrics.lineOf(titleSize),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (metaInline.isNotBlank()) {
                Text(
                    text = metaInline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = metaSize,
                    lineHeight = metrics.lineOf(metaSize),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (!metrics.isTablet && genres.isNotBlank()) {
                Text(
                    text = genres,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = metaSize,
                    lineHeight = metrics.lineOf(metaSize),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        val kp = item.kpScore()
        val imdb = item.imdbScore()

        if (kp != null || imdb != null) {
            Spacer(Modifier.width(if (metrics.isTablet) 12.dp else 6.dp))
            Column(horizontalAlignment = Alignment.End) {
                kp?.let {
                    SuggestRatingChip(
                        label = "КП",
                        value = it,
                        color = colors.ratingKp,
                        fontSize = metrics.ratingChipSize,
                    )
                }
                if (kp != null && imdb != null) {
                    Spacer(Modifier.height(if (metrics.isTablet) 6.dp else 4.dp))
                }
                imdb?.let {
                    SuggestRatingChip(
                        label = "IM",
                        value = it,
                        color = colors.ratingImdb,
                        fontSize = metrics.ratingChipSize,
                    )
                }
            }
        }

        if (canFavorite) {
            Spacer(Modifier.width(if (metrics.isTablet) 16.dp else 4.dp))
            IconButton(
                onClick = onToggleFavorite,
                enabled = !favoriteBusy,
                modifier = Modifier.size(favHit),
            ) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = stringResource(
                        if (isFavorite) {
                            R.string.detail_favorite_remove
                        } else {
                            R.string.detail_favorite_add
                        },
                    ),
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(favIcon),
                )
            }
        }
    }
}

@Composable
private fun SuggestPersonRow(
    name: String,
    originalName: String?,
    photoUrl: String?,
    onClick: () -> Unit,
    onImageClick: () -> Unit,
) {
    val metrics = LocalHomeMetrics.current

    val posterW = if (metrics.isTablet) 84.dp else 48.dp
    val posterH = if (metrics.isTablet) 118.dp else 68.dp
    val titleSize = if (metrics.isTablet) metrics.cardTitleSize else metrics.searchTextSize
    val metaSize = metrics.cardMetaSize

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = if (metrics.isTablet) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = photoUrl.absMediaUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(posterW, posterH)
                .clip(RoundedCornerShape(if (metrics.isTablet) 10.dp else 6.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                .clickable(onClick = onImageClick),
        )

        Spacer(Modifier.width(if (metrics.isTablet) 16.dp else 12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = titleSize,
                lineHeight = metrics.lineOf(titleSize),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )

            if (!originalName.isNullOrBlank()) {
                Text(
                    text = originalName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = metaSize,
                    lineHeight = metrics.lineOf(metaSize),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (metrics.isTablet) 32.dp else 24.dp),
        )
    }
}

@Composable
private fun SuggestRatingChip(
    label: String,
    value: Double,
    color: Color,
    fontSize: TextUnit,
) {
    val formatted = remember(value) { "%.1f".format(value) }
    Text(
        text = "$label $formatted",
        color = Color.Black,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
private fun ContentColumn(
    metrics: HomeMetrics,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = metrics.contentMaxWidth),
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
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