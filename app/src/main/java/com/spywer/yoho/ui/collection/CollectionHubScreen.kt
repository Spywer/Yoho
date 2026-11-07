package com.spywer.yoho.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spywer.yoho.R
import com.spywer.yoho.MainActivity
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics

@Composable
fun CollectionHubScreen(
    onOpenTitle: ((idKp: Int?, idTmdb: Int?, mediaType: String?) -> Unit)? = null,
    onOpenShelf: (CollectionShelf) -> Unit,
    viewModel: CollectionHubViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    val titleSize = (metrics.sectionTitleSize.value + 4f).sp
    val rowText = (metrics.searchTextSize.value + 2f).sp
    val iconSize = if (metrics.isTablet) 32.dp else 24.dp
    val pad = metrics.screenPadding

    CompositionLocalProvider(LocalHomeMetrics provides metrics) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = metrics.poster),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = pad,
                end = pad,
                bottom = metrics.blockGap * 2,
            ),
            horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
            verticalArrangement = Arrangement.spacedBy(metrics.cardGap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.nav_favorites),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = metrics.blockGap * 2),
                    )
                    HubRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        label = stringResource(R.string.favorites_section),
                        textSize = rowText,
                        iconSize = iconSize,
                        onClick = { onOpenShelf(CollectionShelf.Favorites) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HubRow(
                        icon = Icons.Outlined.AutoStories,
                        label = stringResource(R.string.diary_section),
                        textSize = rowText,
                        iconSize = iconSize,
                        onClick = { onOpenShelf(CollectionShelf.Diary) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HubRow(
                        icon = Icons.Outlined.History,
                        label = stringResource(R.string.history_section),
                        textSize = rowText,
                        iconSize = iconSize,
                        onClick = { onOpenShelf(CollectionShelf.History) },
                    )
                    if (!state.needsLogin) {
                        Text(
                            text = stringResource(R.string.collection_films_section),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = metrics.sectionTitleSize,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                top = metrics.blockGap * 2,
                                bottom = metrics.blockGap,
                            ),
                        )
                    }
                }
            }

            when {
                state.needsLogin -> Unit
                state.loading && state.filmsEmpty -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                state.filmsEmpty -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.collection_films_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = rowText,
                            modifier = Modifier.padding(vertical = metrics.blockGap),
                        )
                    }
                }
                else -> {
                    items(
                        items = state.cards,
                        key = { it.key },
                    ) { card ->
                        PosterCard(
                            item = card.title,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val film = card.film
                                val kp = film.resolvedKp()
                                if (kp == null || kp <= 0) return@PosterCard
                                val media = film.mediaType ?: "movie"
                                onOpenTitle?.invoke(kp, film.tmdbId, media)
                                    ?: context.startActivity(
                                        MainActivity.intentOpenTitle(
                                            context,
                                            kp,
                                            film.tmdbId,
                                            media,
                                        ),
                                    )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HubRow(
    icon: ImageVector,
    label: String,
    textSize: TextUnit,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
