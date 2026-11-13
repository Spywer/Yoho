package com.spywer.yoho.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spywer.yoho.R
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.ui.account.AccountScreen
import com.spywer.yoho.ui.detail.LocalDetailPip
import com.spywer.yoho.ui.detail.TitleDetailScreen
import com.spywer.yoho.ui.detail.TitleDetailViewModel
import com.spywer.yoho.ui.collection.CollectionHubScreen
import com.spywer.yoho.ui.collection.CollectionScreen
import com.spywer.yoho.ui.collection.CollectionShelf
import com.spywer.yoho.ui.collection.CollectionViewModel
import com.spywer.yoho.ui.home.HomeScreen
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.search.SearchActivity
import com.spywer.yoho.ui.search.SearchScreen
import com.spywer.yoho.ui.search.SearchViewModel
import com.spywer.yoho.ui.search.genreSlugOf

enum class MainTab {
    Home,
    Catalog,
    Favorites,
    Account,
}

/** Внешний запрос открыть деталку (из Diary/Favorites Activity → Main). */
data class TitleOpenRequest(
    val idKp: Int?,
    val idTmdb: Int?,
    val mediaType: String?,
)

private data class TabSpec(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private data class TitleRoute(
    val idKp: Int?,
    val idTmdb: Int?,
    val mediaType: String?,
) {
    fun encode(): String =
        "${idKp ?: 0}|${idTmdb ?: 0}|${mediaType.orEmpty()}"

    companion object {
        fun decode(raw: String): TitleRoute {
            val parts = raw.split('|')
            val kp = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it > 0 }
            val tmdb = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
            val type = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
            return TitleRoute(kp, tmdb, type)
        }
    }
}

@Composable
fun MainShell(
    initialTab: MainTab? = null,
    openTabRequest: MainTab? = null,
    onOpenTabConsumed: () -> Unit = {},
    openTitleRequest: TitleOpenRequest? = null,
    onOpenTitleConsumed: () -> Unit = {},
    openShelfRequest: CollectionShelf? = null,
    onOpenShelfConsumed: () -> Unit = {},
    openActorRequest: Pair<String, String>? = null,
    onOpenActorConsumed: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(initialTab ?: MainTab.Home) }
    var collectionShelf by rememberSaveable { mutableStateOf<String?>(null) }
    var lastCatalogTab by remember { mutableStateOf<MainTab?>(null) }
    val catalogVm: SearchViewModel = viewModel()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val titleStack = rememberSaveable(
        saver = listSaver(
            save = { list -> list.map { it.encode() } },
            restore = { saved -> saved.map { TitleRoute.decode(it) }.toMutableStateList() },
        ),
    ) {
        emptyList<TitleRoute>().toMutableStateList()
    }
    LaunchedEffect(initialTab) {
        if (initialTab != null) tab = initialTab
    }
    LaunchedEffect(openTabRequest) {
        val target = openTabRequest ?: return@LaunchedEffect
        titleStack.clear()
        collectionShelf = null
        tab = target
        onOpenTabConsumed()
    }
    LaunchedEffect(openTabRequest, openActorRequest) {
        if (openTabRequest == MainTab.Catalog) {
            catalogVm.clearFilters()
        }
        val req = openActorRequest ?: return@LaunchedEffect
        catalogVm.setInitial(actor = req.first, actorKp = req.second)
        onOpenActorConsumed()
    }

    fun openTitle(idKp: Int?, idTmdb: Int?, mediaType: String?) {
        if ((idKp == null || idKp <= 0) && (idTmdb == null || idTmdb <= 0)) return
        titleStack.add(TitleRoute(idKp, idTmdb, mediaType))
    }

    fun closeTitle() {
        if (titleStack.isNotEmpty()) {
            titleStack.removeAt(titleStack.lastIndex)
        }
    }

    fun openShelf(shelf: CollectionShelf) {
        tab = MainTab.Favorites
        collectionShelf = shelf.name
    }

    LaunchedEffect(openTitleRequest) {
        val req = openTitleRequest ?: return@LaunchedEffect
        openTitle(req.idKp, req.idTmdb, req.mediaType)
        onOpenTitleConsumed()
    }

    LaunchedEffect(openShelfRequest) {
        val shelf = openShelfRequest ?: return@LaunchedEffect
        openShelf(shelf)
        onOpenShelfConsumed()
    }

    LaunchedEffect(Unit) {
        SessionRepository.refresh()
    }

    // Каталог держит IME; при уходе с вкладки / открытии карточки — закрыть.
    // При уходе с Catalog — сбросить фильтры, чтобы при возврате каталог был пустым.
    // lastCatalogTab — отслеживаем, когда пользователь реально переключился с Catalog.
    LaunchedEffect(tab, titleStack.size) {
        if (tab != MainTab.Catalog || titleStack.isNotEmpty()) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
        if (lastCatalogTab == MainTab.Catalog && tab != MainTab.Catalog && titleStack.isEmpty()) {
            catalogVm.clearFilters()
        }
        if (tab == MainTab.Catalog) {
            lastCatalogTab = MainTab.Catalog
        }
    }

    val tabs = listOf(
        TabSpec(MainTab.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
        TabSpec(MainTab.Catalog, R.string.nav_catalog, Icons.Filled.Explore, Icons.Outlined.Explore),
        TabSpec(MainTab.Favorites, R.string.nav_favorites, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
        TabSpec(MainTab.Account, R.string.nav_account, Icons.Filled.Person, Icons.Outlined.Person),
    )
    val metrics = rememberHomeMetrics()

    // Полка → хаб; иначе каталог/избранное/аккаунт → главная (деталка сама ловит назад).
    BackHandler(
        enabled = titleStack.isEmpty() && (collectionShelf != null || tab != MainTab.Home),
    ) {
        when {
            collectionShelf != null -> collectionShelf = null
            else -> tab = MainTab.Home
        }
    }

    val inPip = LocalDetailPip.current?.inPipState?.value == true

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
    Scaffold(
        containerColor = if (inPip) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.background
        },
        // Только bottomBar; statusBars — на экранах (иначе двойной верхний отступ).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // В PiP — только видео, без навбара.
            if (!inPip) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding(),
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.height(metrics.navBarHeight),
                    ) {
                        tabs.forEach { spec ->
                            val selected = tab == spec.tab
                            val active = MaterialTheme.colorScheme.primary
                            val inactive = MaterialTheme.colorScheme.onSurfaceVariant
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    titleStack.clear()
                                    collectionShelf = null
                                    tab = spec.tab
                                },
                                icon = {
                                    val compactNav = !metrics.isTablet
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(
                                            if (compactNav) 0.dp else 2.dp,
                                        ),
                                        modifier = Modifier.padding(
                                            horizontal = if (compactNav) 4.dp else 6.dp,
                                            vertical = if (compactNav) 0.dp else 2.dp,
                                        ),
                                    ) {
                                        Icon(
                                            imageVector = if (selected) {
                                                spec.selectedIcon
                                            } else {
                                                spec.unselectedIcon
                                            },
                                            contentDescription = stringResource(spec.labelRes),
                                            modifier = Modifier.size(metrics.navIconSize),
                                            tint = if (selected) active else inactive,
                                        )
                                        Text(
                                            text = stringResource(spec.labelRes),
                                            fontSize = metrics.navLabelSize,
                                            fontWeight = if (selected) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Medium
                                            },
                                            color = if (selected) active else inactive,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                },
                                label = null,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = active,
                                    selectedTextColor = active,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedIconColor = inactive,
                                    unselectedTextColor = inactive,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .then(if (inPip) Modifier else Modifier.padding(padding)),
        ) {
            if (!inPip) {
                when (tab) {
                    MainTab.Home -> HomeScreen(
                        onOpenCatalog = { tab = MainTab.Catalog },
                        onOpenTitle = ::openTitle,
                    )
                    MainTab.Catalog -> SearchScreen(
                        viewModel = catalogVm,
                        onBack = { tab = MainTab.Home },
                        showBack = false,
                        title = stringResource(R.string.nav_catalog),
                        restoreSearchFocus = titleStack.isEmpty(),
                        onOpenTitle = ::openTitle,
                    )
                    MainTab.Favorites -> {
                        val shelf = collectionShelf?.let { name ->
                            runCatching { CollectionShelf.valueOf(name) }.getOrNull()
                        }
                        if (shelf != null) {
                            val shelfVm: CollectionViewModel = viewModel(
                                key = "collection-shelf-${shelf.name}",
                                factory = CollectionViewModel.factory(shelf),
                            )
                            CollectionScreen(
                                viewModel = shelfVm,
                                fixedShelf = shelf,
                                onBack = { collectionShelf = null },
                                onNeedLogin = {
                                    collectionShelf = null
                                    tab = MainTab.Account
                                },
                                onOpenTitle = ::openTitle,
                            )
                        } else {
                            CollectionHubScreen(
                                onOpenTitle = ::openTitle,
                                onOpenShelf = ::openShelf,
                                onNeedLogin = { tab = MainTab.Account },
                            )
                        }
                    }
                    MainTab.Account -> AccountScreen(
                        onBack = { tab = MainTab.Home },
                    )
                }
            }

            val route = titleStack.lastOrNull()
            if (route != null) {
                val detailKey = remember(route.idKp, route.idTmdb, route.mediaType, titleStack.size) {
                    "${route.idKp}_${route.idTmdb}_${route.mediaType}_${titleStack.size}"
                }
                val detailVm: TitleDetailViewModel = viewModel(key = detailKey)
                LaunchedEffect(detailKey) {
                    detailVm.load(route.idKp, route.idTmdb, route.mediaType)
                }
                TitleDetailScreen(
                    viewModel = detailVm,
                    onBack = ::closeTitle,
                    onOpenTitle = ::openTitle,
                    onNeedLogin = {
                        titleStack.clear()
                        collectionShelf = null
                        tab = MainTab.Account
                    },
                    onOpenGenre = { genreLabel, mediaType ->
                        val slug = genreSlugOf(genreLabel) ?: return@TitleDetailScreen
                        context.startActivity(
                            SearchActivity.intent(
                                context,
                                genre = slug,
                                type = mediaType,
                            ),
                        )
                    },
                )
            }
        }
    }
    } // CompositionLocalProvider
}
