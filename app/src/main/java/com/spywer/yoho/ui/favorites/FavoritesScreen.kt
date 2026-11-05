package com.spywer.yoho.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.TitleNameDto
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.detail.TitleDetailActivity
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics

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

    fun openItem(item: FavoriteItemDto) {
        val kp = item.contentId
            ?: item.id?.removePrefix("kp:")?.toIntOrNull()
        if (kp == null || kp <= 0) return
        val media = item.mediaType ?: item.contentType ?: "movie"
        onOpenTitle?.invoke(kp, null, media)
            ?: context.startActivity(
                TitleDetailActivity.intent(context, kp, null, media),
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

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Text(
                text = stringResource(R.string.nav_favorites),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = metrics.sectionTitleSize,
                lineHeight = metrics.lineOf(metrics.sectionTitleSize),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = metrics.screenPadding,
                    vertical = metrics.sectionGap,
                ),
            )

            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                state.needsLogin -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.favorites_need_login),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onNeedLogin,
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text(stringResource(R.string.account_sign_in))
                        }
                    }
                }

                state.error != null && state.favorites.isEmpty() && state.history.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.load() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.refreshing,
                        onRefresh = { viewModel.load(refresh = true) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.favorites.isEmpty() && state.history.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.favorites_empty),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SectionHeader(stringResource(R.string.favorites_section))
                                }
                                if (state.favorites.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyHint(stringResource(R.string.favorites_empty))
                                    }
                                } else {
                                    items(
                                        state.favorites,
                                        key = { "fav-${it.id ?: it.contentId}" },
                                    ) { item ->
                                        PosterCard(
                                            item = item.toTitleDto(),
                                            onClick = { openItem(item) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SectionHeader(
                                        stringResource(R.string.history_section),
                                        modifier = Modifier.padding(top = metrics.sectionGap),
                                    )
                                }
                                if (state.history.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyHint(stringResource(R.string.history_empty))
                                    }
                                } else {
                                    items(
                                        state.history,
                                        key = { "hist-${it.id ?: it.contentId}-${it.ts}" },
                                    ) { item ->
                                        PosterCard(
                                            item = item.toTitleDto(),
                                            onClick = { openItem(item) },
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

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val metrics = LocalHomeMetrics.current
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = metrics.sectionTitleSize,
        lineHeight = metrics.lineOf(metrics.sectionTitleSize),
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(bottom = metrics.blockGap),
    )
}

@Composable
private fun EmptyHint(text: String) {
    val metrics = LocalHomeMetrics.current
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = metrics.cardMetaSize,
        lineHeight = metrics.lineOf(metrics.cardMetaSize),
        modifier = Modifier.padding(bottom = metrics.blockGap),
    )
}
