package com.spywer.yoho.ui.detail

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.spywer.yoho.ui.components.DiaryStatusDropdownMenu
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.PlayerBrowserProfile
import com.spywer.yoho.data.remote.PlayerChoiceItemDto
import com.spywer.yoho.data.remote.PlayerWebViewHttp
import com.spywer.yoho.data.remote.SiteHttp
import com.spywer.yoho.data.remote.displayPlayerSourceName
import com.spywer.yoho.ui.layout.LocalHomeMetrics

@Composable
internal fun PlayerBlock(
    choices: List<PlayerChoiceItemDto>,
    selectedSource: String?,
    iframeUrl: String?,
    embedHtml: String?,
    loading: Boolean,
    needsLogin: Boolean,
    mediaType: String?,
    diaryStatusLabel: String?,
    diaryBusy: Boolean,
    diaryError: String?,
    diaryNotify: String?,
    playerReaction: String?,
    reactionBusy: Boolean,
    onSelectSource: (String) -> Unit,
    onSetDiaryStatus: (String) -> Unit,
    onRemoveFromDiary: () -> Unit,
    onConsumeDiaryNotify: () -> Unit,
    onPlayerReaction: (String) -> Unit,
    onLoginClick: () -> Unit,
    padding: androidx.compose.ui.unit.Dp,
    metaSize: androidx.compose.ui.unit.TextUnit,
    /** Holder с экрана — общий lifecycle WebView на время деталки. */
    webHolder: PlayerWebViewHolder,
    modifier: Modifier = Modifier,
) {
    val playUrl = remember(iframeUrl) { normalizePlayerUrl(iframeUrl) }
    val hasPlayer = !needsLogin && (!playUrl.isNullOrBlank() || !embedHtml.isNullOrBlank())
    // Заглушка «нет источников» — без учёта player-choice (чипы).
    val noSources = !needsLogin && !hasPlayer
    val context = LocalContext.current
    // Подхват уже открытого cinema на decor при редком remount.
    var fullscreen by remember(webHolder) { mutableStateOf(webHolder.isCinemaFullscreen) }
    var htmlFullscreen by remember(webHolder) { mutableStateOf(webHolder.isHtmlFullscreen) }
    // Toast дневника, когда панели под плеером нет (заглушка без источников).
    LaunchedEffect(diaryNotify, hasPlayer) {
        if (hasPlayer) return@LaunchedEffect
        val msg = diaryNotify ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        onConsumeDiaryNotify()
    }
    // WebView всегда в Compose; FS = reparent на decorView (Dialog ломал surface при возврате)
    DisposableEffect(webHolder) {
        webHolder.onHtmlFullscreenChanged = { htmlFullscreen = it }
        webHolder.onCinemaExit = {
            fullscreen = false
            webHolder.detachCinemaFullscreen()
            if (webHolder.isHtmlFullscreen) webHolder.requestExitHtmlFullscreen()
        }
        onDispose {
            webHolder.onHtmlFullscreenChanged = null
            webHolder.onCinemaExit = null
        }
    }
    DisposableEffect(hasPlayer) {
        if (!hasPlayer) webHolder.pausePlayback()
        onDispose { }
    }

    fun exitCinema() {
        fullscreen = false
        if (webHolder.isHtmlFullscreen) webHolder.requestExitHtmlFullscreen()
        else webHolder.detachCinemaFullscreen()
    }
    BackHandler(enabled = htmlFullscreen || fullscreen) {
        when {
            htmlFullscreen -> webHolder.requestExitHtmlFullscreen()
            else -> exitCinema()
        }
    }
    val fsActive = fullscreen || htmlFullscreen || webHolder.isCinemaFullscreen
    DetailPipEligibilityEffect(eligible = hasPlayer)
    DetailPipWebViewBindEffect(webView = webHolder.webView)
    PlayerSystemBarsEffect(hidden = fsActive)
    PlayerKeepScreenOnEffect(enabled = fsActive)
    PlayerFullscreenOrientationEffect(active = fsActive)
    // Detach только через exitCinema/крестик — не по enabled=false.
    PlayerFullscreenEffect(
        enabled = (fullscreen && hasPlayer) || webHolder.isCinemaFullscreen,
        holder = webHolder,
        onExit = { exitCinema() },
    )
    // Zoom внутри плеера — только на весь экран.
    LaunchedEffect(fullscreen, htmlFullscreen, hasPlayer) {
        val wv = webHolder.webView ?: return@LaunchedEffect
        if (!hasPlayer) return@LaunchedEffect
        applyPlayerContentZoom(wv, enabled = fullscreen || htmlFullscreen)
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = padding)
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
            if (choices.isNotEmpty() && !loading && !needsLogin) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    choices.forEach { item ->
                        val source = item.source ?: return@forEach
                        val selected = source.equals(selectedSource, ignoreCase = true)
                        PlayerSourceChip(
                            label = displayPlayerSourceName(source),
                            share = item.share ?: 0.0,
                            selected = selected,
                            onClick = { onSelectSource(source) },
                        )
                    }
                }
                Spacer(Modifier.height(LocalHomeMetrics.current.blockGap))
            }

            if (needsLogin && !loading && !hasPlayer) {
                PlayerLoginPlaceholder(
                    onLoginClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        // Не снимать WebView на время loading — иначе первый loadUrl до attach
                        // иногда даёт пустой кадр до ручного переключения источника.
                        hasPlayer -> {
                            PlayerWebView(
                                url = playUrl,
                                embedHtml = embedHtml,
                                source = selectedSource,
                                holder = webHolder,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (fullscreen) {
                                Text(
                                    text = stringResource(R.string.detail_player_fullscreen_hint),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = metaSize,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                        loading -> Unit
                        noSources -> {
                            NoSourcesPlaceholder(
                                mediaType = mediaType,
                                diaryStatusLabel = diaryStatusLabel,
                                diaryBusy = diaryBusy,
                                onSetDiaryStatus = onSetDiaryStatus,
                                onRemoveFromDiary = onRemoveFromDiary,
                                metaSize = metaSize,
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp),
                                )
                                Text(
                                    text = if (selectedSource != null) {
                                        "Источник: ${displayPlayerSourceName(selectedSource)}"
                                    } else {
                                        stringResource(R.string.detail_player_pick)
                                    },
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = metaSize,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text(
                                    text = stringResource(R.string.detail_player_no_iframe),
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = metaSize,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                    if (loading) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                            )
                        }
                    }
                }
            }

            // Как на сайте: панель под плеером (без «Запретки»)
            if (hasPlayer && !loading) {
                PlayerUnderBar(
                    diaryStatusLabel = diaryStatusLabel,
                    diaryBusy = diaryBusy,
                    diaryNotify = diaryNotify,
                    playerReaction = playerReaction,
                    reactionBusy = reactionBusy,
                    onSetDiaryStatus = onSetDiaryStatus,
                    onRemoveFromDiary = onRemoveFromDiary,
                    onConsumeDiaryNotify = onConsumeDiaryNotify,
                    onPlayerReaction = onPlayerReaction,
                    onCinema = { fullscreen = true },
                )
            }
    }
}

@Composable
private fun PlayerLoginPlaceholder(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalHomeMetrics.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A2D49),
                        Color(0xFF162338),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = primary.copy(alpha = 0.45f),
                shape = shape,
            )
            .padding(
                horizontal = if (metrics.isTablet) 20.dp else 14.dp,
                vertical = if (metrics.isTablet) 16.dp else 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (metrics.isTablet) 16.dp else 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (metrics.isTablet) 44.dp else 36.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(if (metrics.isTablet) 22.dp else 18.dp),
            )
        }

        Text(
            text = stringResource(R.string.detail_player_login_prompt),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = if (metrics.isTablet) metrics.detailBodySize else metrics.detailMetaSize,
            lineHeight = if (metrics.isTablet) metrics.detailBodyLine else metrics.detailMetaLine,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Button(
            onClick = onLoginClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor = onPrimary,
            ),
            contentPadding = PaddingValues(
                horizontal = if (metrics.isTablet) 20.dp else 14.dp,
                vertical = 0.dp,
            ),
            modifier = Modifier.height(if (metrics.isTablet) 42.dp else 36.dp),
        ) {
            Text(
                text = stringResource(R.string.detail_player_login_button),
                fontSize = if (metrics.isTablet) metrics.searchTextSize else metrics.chipTextSize,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NoSourcesPlaceholder(
    mediaType: String?,
    diaryStatusLabel: String?,
    diaryBusy: Boolean,
    onSetDiaryStatus: (String) -> Unit,
    onRemoveFromDiary: () -> Unit,
    metaSize: androidx.compose.ui.unit.TextUnit,
) {
    val wantLabel = stringResource(R.string.detail_diary_want)
    val kind = stringResource(
        when (mediaType?.lowercase()) {
            "tv" -> R.string.detail_player_no_sources_tv
            "anime" -> R.string.detail_player_no_sources_anime
            "animation" -> R.string.detail_player_no_sources_animation
            else -> R.string.detail_player_no_sources_movie
        },
    )
    val wantActive = diaryStatusLabel.equals(wantLabel, ignoreCase = true)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_player_no_sources, kind),
            color = Color.White.copy(alpha = 0.88f),
            fontSize = metaSize * 1.05f,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.detail_player_no_sources_hint),
            color = Color(0xFF8D9EB0),
            fontSize = metaSize,
            textAlign = TextAlign.Center,
        )
        val pillBg = if (wantActive) Color(0xFF3A4A5A) else Color(0xFF242D36)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(pillBg)
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(percent = 50))
                .clickable(enabled = !diaryBusy) {
                    if (wantActive) onRemoveFromDiary() else onSetDiaryStatus(wantLabel)
                }
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                tint = Color(0xFFE8E8E8),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = wantLabel,
                color = Color(0xFFE8E8E8),
                fontSize = metaSize,
            )
        }
    }
}

@Composable
private fun PlayerSourceChip(
    label: String,
    share: Double,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val metrics = LocalHomeMetrics.current
    val fraction = share.toFloat().coerceIn(0f, 1f)
    val shape = RoundedCornerShape(percent = 50)
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .height(metrics.playerChipHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) {
                    Modifier.border(2.dp, primary, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(
                    if (selected) primary else primary.copy(alpha = 0.45f),
                ),
        )
        Text(
            text = label,
            fontSize = metrics.playerChipText,
            fontWeight = FontWeight.Medium,
            color = when {
                selected -> onPrimary
                fraction >= 0.55f -> onPrimary
                else -> onSurface
            },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (metrics.isTablet) 18.dp else 14.dp),
        )
    }
}

@Composable
private fun PlayerUnderBar(
    diaryStatusLabel: String?,
    diaryBusy: Boolean,
    diaryNotify: String?,
    playerReaction: String?,
    reactionBusy: Boolean,
    onSetDiaryStatus: (String) -> Unit,
    onRemoveFromDiary: () -> Unit,
    onConsumeDiaryNotify: () -> Unit,
    onPlayerReaction: (String) -> Unit,
    onCinema: () -> Unit,
) {
    val context = LocalContext.current
    val soon = stringResource(R.string.detail_player_action_soon)
    fun toastSoon() {
        Toast.makeText(context, soon, Toast.LENGTH_SHORT).show()
    }
    val liked = playerReaction == "like"
    val disliked = playerReaction == "dislike"
    var diaryMenuOpen by remember { mutableStateOf(false) }
    val inDiary = diaryStatusLabel != null

    LaunchedEffect(diaryNotify) {
        val msg = diaryNotify ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        onConsumeDiaryNotify()
    }

    val metrics = LocalHomeMetrics.current
    val iconMod = Modifier.size(metrics.playerBarBtn)
    val iconSize = metrics.playerBarIcon

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { toastSoon() }, modifier = iconMod) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = stringResource(R.string.detail_player_joint),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (!reactionBusy) onPlayerReaction("like") },
                enabled = !reactionBusy,
                modifier = iconMod,
            ) {
                Icon(
                    Icons.Filled.ThumbUp,
                    contentDescription = stringResource(R.string.detail_player_like),
                    tint = if (liked) {
                        Color(0xFF43A047)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(iconSize),
                )
            }
            // DropdownMenu, не AlertDialog: Dialog ломает surface WebView-плеера.
            Box {
                IconButton(
                    onClick = { if (!diaryBusy) diaryMenuOpen = true },
                    enabled = !diaryBusy,
                    modifier = iconMod
                        .then(
                            if (inDiary) {
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFE67E22))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = stringResource(R.string.detail_player_diary),
                        tint = if (inDiary) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(iconSize),
                    )
                }
                DiaryStatusDropdownMenu(
                    expanded = diaryMenuOpen,
                    currentStatus = diaryStatusLabel,
                    busy = diaryBusy,
                    metrics = metrics,
                    onDismiss = { diaryMenuOpen = false },
                    onSelectStatus = { label ->
                        diaryMenuOpen = false
                        onSetDiaryStatus(label)
                    },
                    onRemove = {
                        diaryMenuOpen = false
                        onRemoveFromDiary()
                    },
                )
            }
            IconButton(
                onClick = { if (!reactionBusy) onPlayerReaction("dislike") },
                enabled = !reactionBusy,
                modifier = iconMod,
            ) {
                Icon(
                    Icons.Filled.ThumbDown,
                    contentDescription = stringResource(R.string.detail_player_dislike),
                    tint = if (disliked) {
                        Color(0xFFE53935)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(iconSize),
                )
            }
            IconButton(onClick = onCinema, modifier = iconMod) {
                Icon(
                    Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.detail_player_fullscreen),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

@Composable
private fun PlayerKeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = context.findActivity()?.window
        if (window == null || !enabled) {
            return@DisposableEffect onDispose { }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
private fun PlayerSystemBarsEffect(hidden: Boolean) {
    val context = LocalContext.current
    val orientation = LocalConfiguration.current.orientation
    // Не ключ DisposableEffect на orientation: иначе onDispose → show → мигание.
    DisposableEffect(hidden) {
        val activity = context.findActivity()
        val window = activity?.window
        val decor = window?.decorView
        val controller = if (window != null && decor != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, decor)
        } else {
            null
        }
        if (hidden && controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    // После поворота система может вернуть bars — только hide, без show.
    LaunchedEffect(hidden, orientation) {
        if (!hidden) return@LaunchedEffect
        val activity = context.findActivity() ?: return@LaunchedEffect
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

/** Держит WebView + inline-контейнер для reparent в fullscreen overlay. */
internal class PlayerWebViewHolder {
    var webView: WebView? = null
    var inlineContainer: FrameLayout? = null
    var fullscreenOverlay: FrameLayout? = null
    /** HTML5 / встроенный FS плеера — отдельный слой на decor, не Compose-слот. */
    var htmlFsContainer: FrameLayout? = null
    var htmlFsCallback: WebChromeClient.CustomViewCallback? = null
    var onHtmlFullscreenChanged: ((Boolean) -> Unit)? = null
    /** Выход из cinema-FS (крестик) → синхрон Compose-state. */
    var onCinemaExit: (() -> Unit)? = null
    /** Orphan popup WebView cleanup (onCreateWindow). */
    private val popupCleanups = mutableListOf<Pair<WebView, Runnable>>()

    val isHtmlFullscreen: Boolean get() = htmlFsContainer != null
    val isCinemaFullscreen: Boolean get() = fullscreenOverlay != null

    fun trackPopupCleanup(popup: WebView, cleanup: Runnable) {
        popupCleanups += popup to cleanup
        popup.tag = cleanup
        popup.postDelayed(cleanup, 5_000L)
    }

    fun cancelPopupCleanup(popup: WebView) {
        val cleanup = popup.tag as? Runnable
        if (cleanup != null) popup.removeCallbacks(cleanup)
        popupCleanups.removeAll { it.first === popup }
        popup.tag = null
    }

    private fun cancelPopupCleanups() {
        popupCleanups.toList().forEach { (popup, cleanup) ->
            runCatching {
                popup.removeCallbacks(cleanup)
                popup.stopLoading()
                (popup.parent as? ViewGroup)?.removeView(popup)
                popup.destroy()
            }
        }
        popupCleanups.clear()
    }

    /** Снять cinema overlay; WebView вернуть в inline, если он ещё жив. */
    fun detachCinemaFullscreen() {
        val wv = webView
        val overlay = fullscreenOverlay ?: return
        val inline = inlineContainer
        if (wv != null) {
            (wv.parent as? ViewGroup)?.removeView(wv)
            if (inline != null && wv.parent == null) {
                inline.addView(
                    wv,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                refreshWebViewSurface(wv)
            }
        }
        clearFullscreenLayoutBinding(overlay)
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        fullscreenOverlay = null
    }

    fun showHtmlFullscreen(view: View, callback: WebChromeClient.CustomViewCallback?) {
        if (htmlFsContainer != null) {
            // Уже в HTML FS — отклоняем повторный
            callback?.onCustomViewHidden()
            return
        }
        val activity = view.context.findActivity()
            ?: webView?.context?.findActivity()
            ?: return
        val decor = activity.window.decorView as ViewGroup
        val density = activity.resources.displayMetrics.density
        val root = FrameLayout(activity).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            isClickable = true
            isFocusable = true
            elevation = 64f
        }
        val videoHost = FrameLayout(activity)
        (view.parent as? ViewGroup)?.removeView(view)
        videoHost.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        root.addView(
            videoHost,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val close = android.widget.ImageButton(activity).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            contentDescription =
                activity.getString(R.string.detail_player_fullscreen_exit)
            setOnClickListener { requestExitHtmlFullscreen() }
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            tag = FULLSCREEN_CLOSE_TAG
        }
        root.addView(
            close,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.END or android.view.Gravity.TOP,
            ).also {
                val m = (8 * density).toInt()
                it.setMargins(m, fullscreenCloseTopMargin(decor, density), m, m)
            },
        )
        bindFullscreenLayoutListener(root, videoHost)
        decor.addView(
            root,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        htmlFsContainer = root
        htmlFsCallback = callback
        // WebView остаётся в app-overlay / inline; прячем, пока играет custom view
        webView?.visibility = View.INVISIBLE
        onHtmlFullscreenChanged?.invoke(true)
    }

    fun hideHtmlFullscreen() {
        val container = htmlFsContainer ?: return
        htmlFsContainer = null
        htmlFsCallback = null
        clearFullscreenLayoutBinding(container)
        runCatching {
            container.removeAllViews()
            (container.parent as? ViewGroup)?.removeView(container)
        }
        webView?.visibility = View.VISIBLE
        webView?.let { refreshWebViewSurface(it) }
        onHtmlFullscreenChanged?.invoke(false)
    }

    /** Back / кнопка закрытия: просим страницу выйти из HTML FS → придёт onHideCustomView. */
    fun requestExitHtmlFullscreen() {
        val cb = htmlFsCallback
        if (cb != null) {
            runCatching { cb.onCustomViewHidden() }
        } else {
            hideHtmlFullscreen()
        }
    }

    /** Пауза при скрытии плеера (нет URL), без полного destroy. */
    fun pausePlayback() {
        // Не глушить, если Activity уже в PiP (гайд Google для WebView).
        val act = webView?.context?.findActivity()
        if (act?.isInPictureInPictureMode == true) return
        val wv = webView ?: return
        runCatching {
            wv.onPause()
            wv.pauseTimers()
            wv.stopLoading()
            wv.loadUrl("about:blank")
        }
    }

    fun destroy() {
        runCatching {
            cancelPopupCleanups()
            val cb = htmlFsCallback
            htmlFsCallback = null
            if (htmlFsContainer != null) hideHtmlFullscreen()
            // Если HTML FS ещё активен с точки зрения Chromium — отпускаем callback.
            if (cb != null) runCatching { cb.onCustomViewHidden() }
            if (fullscreenOverlay != null) detachCinemaFullscreen()
            val wv = webView
            if (wv != null) {
                wv.stopLoading()
                runCatching { wv.loadUrl("about:blank") }
                runCatching { wv.webChromeClient = WebChromeClient() }
                runCatching { wv.webViewClient = WebViewClient() }
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.destroy()
            }
        }
        webView = null
        inlineContainer = null
        fullscreenOverlay = null
        htmlFsContainer = null
        htmlFsCallback = null
        onHtmlFullscreenChanged = null
        onCinemaExit = null
    }
}

internal tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun refreshWebViewSurface(webView: WebView) {
    webView.post {
        runCatching {
            webView.onResume()
            webView.resumeTimers()
            webView.requestLayout()
            webView.invalidate()
        }
    }
}

/**
 * Поверхность плеера в системном PiP без cinema FS:
 * WebView в слот Activity; если уже cinema/HTML FS — видео на decor, тут чёрный фон.
 */
@Composable
internal fun PlayerPipSurface(
    holder: PlayerWebViewHolder,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(holder) {
        val wv = holder.webView ?: return@LaunchedEffect
        refreshWebViewSurface(wv)
    }
    val onDecor = holder.isCinemaFullscreen || holder.isHtmlFullscreen
    if (onDecor) {
        Box(modifier.background(Color.Black))
        return
    }
    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                holder.inlineContainer = this
                val webView = holder.webView
                if (webView != null) {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    addView(
                        webView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    refreshWebViewSurface(webView)
                }
            }
        },
        update = { frame ->
            holder.inlineContainer = frame
            val webView = holder.webView ?: return@AndroidView
            if (!holder.isCinemaFullscreen &&
                !holder.isHtmlFullscreen &&
                webView.parent !== frame
            ) {
                (webView.parent as? ViewGroup)?.removeView(webView)
                frame.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                refreshWebViewSurface(webView)
            }
        },
        onRelease = { frame ->
            if (holder.inlineContainer === frame) {
                holder.inlineContainer = null
            }
        },
        modifier = modifier,
    )
}

/** Отступ крестика от верха: status bar (и при immersive) + 10dp. */
private fun fullscreenCloseTopMargin(host: View, density: Float): Int {
    val inset = ViewCompat.getRootWindowInsets(host)
        ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
        ?.top
        ?: 0
    val status = if (inset > 0) {
        inset
    } else {
        val id = host.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) host.resources.getDimensionPixelSize(id) else (24 * density).toInt()
    }
    return status + (10 * density).toInt()
}

private const val FULLSCREEN_CLOSE_TAG = "yoho_fs_close"

/** Альбом — без крестика; портрет — показать. */
private fun syncFullscreenCloseVisibility(container: ViewGroup, landscape: Boolean) {
    // val visibility = if (landscape) View.GONE else View.VISIBLE
    for (i in 0 until container.childCount) {
        val child = container.getChildAt(i)
        //if (child.tag == FULLSCREEN_CLOSE_TAG && child.visibility != visibility) {
        //    child.visibility = visibility
        //}
        // Временно скрываем крестик закрытия
        if (child.tag == FULLSCREEN_CLOSE_TAG) {
            child.visibility = View.GONE
        }
    }
}

/**
 * Только крестик при смене orientation.
 * Letterbox-padding убран: при уходе в альбом setPadding переживал WebView → чёрный кадр/мигание.
 * Слот всегда edge-to-edge MATCH_PARENT.
 */
/** Cleanup postDelayed + OnLayoutChangeListener при detach overlay. */
private class FullscreenLayoutBinding(
    private val overlay: FrameLayout,
) {
    private var pending: Runnable? = null
    private var appliedOrientation: Int? = null
    private val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        schedule(force = false)
    }

    init {
        overlay.addOnLayoutChangeListener(listener)
        overlay.post { schedule(force = true) }
    }

    fun schedule(force: Boolean) {
        val orient = overlay.resources.configuration.orientation
        if (!force && orient == appliedOrientation) return
        pending?.let { overlay.removeCallbacks(it) }
        val task = Runnable {
            pending = null
            val o = overlay.resources.configuration.orientation
            appliedOrientation = o
            syncFullscreenCloseVisibility(
                overlay,
                landscape = o == Configuration.ORIENTATION_LANDSCAPE,
            )
        }
        pending = task
        overlay.postDelayed(task, 120L)
    }

    fun clear() {
        pending?.let { overlay.removeCallbacks(it) }
        pending = null
        overlay.removeOnLayoutChangeListener(listener)
        if (overlay.tag === this) overlay.tag = null
    }
}

private fun bindFullscreenLayoutListener(
    overlay: FrameLayout,
    videoHost: FrameLayout,
) {
    videoHost.clipChildren = true
    videoHost.clipToPadding = true
    // На всякий случай сбросить letterbox от прошлой сессии.
    if (videoHost.paddingLeft != 0 || videoHost.paddingTop != 0 ||
        videoHost.paddingRight != 0 || videoHost.paddingBottom != 0
    ) {
        videoHost.setPadding(0, 0, 0, 0)
    }
    (overlay.tag as? FullscreenLayoutBinding)?.clear()
    overlay.tag = FullscreenLayoutBinding(overlay)
}

private fun clearFullscreenLayoutBinding(overlay: FrameLayout?) {
    val binding = overlay?.tag as? FullscreenLayoutBinding ?: return
    binding.clear()
}

/** В FS — следовать датчику; иначе вернуть прежнюю ориентацию Activity. */
@Composable
private fun PlayerFullscreenOrientationEffect(active: Boolean) {
    val context = LocalContext.current
    DisposableEffect(active) {
        val activity = context.findActivity()
        if (activity == null) {
            return@DisposableEffect onDispose { }
        }
        val previous = activity.requestedOrientation
        if (active) {
            // SENSOR без reverse — меньше дёрганий при почти горизонтальном наклоне.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        onDispose {
            activity.requestedOrientation = previous
        }
    }
}

@Composable
private fun PlayerFullscreenEffect(
    enabled: Boolean,
    holder: PlayerWebViewHolder,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val onExitUpdated by rememberUpdatedState(onExit)

    // Только attach. Detach — явный onExit/крестик (иначе remount item срывает FS).
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val activity = context.findActivity()
        val webView = holder.webView
        if (activity == null || webView == null) return@LaunchedEffect
        // Уже на decor (item пересоздали) — только обновить крестик.
        holder.fullscreenOverlay?.let { overlay ->
            for (i in 0 until overlay.childCount) {
                val child = overlay.getChildAt(i)
                if (child.tag == FULLSCREEN_CLOSE_TAG) {
                    child.setOnClickListener {
                        if (holder.isHtmlFullscreen) holder.requestExitHtmlFullscreen()
                        else {
                            holder.onCinemaExit?.invoke() ?: onExitUpdated()
                        }
                    }
                }
            }
            return@LaunchedEffect
        }
        val decor = activity.window.decorView as ViewGroup
        val density = context.resources.displayMetrics.density
        val overlay = FrameLayout(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            isClickable = true
            isFocusable = true
        }
        val videoHost = FrameLayout(context)
        (webView.parent as? ViewGroup)?.removeView(webView)
        videoHost.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        overlay.addView(
            videoHost,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val close = android.widget.ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            contentDescription =
                context.getString(R.string.detail_player_fullscreen_exit)
            setOnClickListener {
                if (holder.isHtmlFullscreen) holder.requestExitHtmlFullscreen()
                else holder.onCinemaExit?.invoke() ?: onExitUpdated()
            }
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            tag = FULLSCREEN_CLOSE_TAG
        }
        // Временно скрываем крестик закрытия
        close.visibility = View.GONE
        overlay.addView(
            close,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.END or android.view.Gravity.TOP,
            ).also {
                val m = (8 * density).toInt()
                it.setMargins(m, fullscreenCloseTopMargin(decor, density), m, m)
            },
        )
        bindFullscreenLayoutListener(overlay, videoHost)
        decor.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        holder.fullscreenOverlay = overlay
        refreshWebViewSurface(webView)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PlayerWebView(
    url: String?,
    embedHtml: String?,
    source: String?,
    holder: PlayerWebViewHolder,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val context = LocalContext.current
    val loadKey = listOf(source.orEmpty(), embedHtml?.hashCode(), url.orEmpty()).joinToString("|")
    val sourceState = remember { mutableStateOf(source) }
    LaunchedEffect(source) { sourceState.value = source }
    val mediaPerms = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    LaunchedEffect(source) {
        if (!source.equals("turbo", ignoreCase = true)) return@LaunchedEffect
        val need = mediaPerms.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need) permissionLauncher.launch(mediaPerms)
    }

    AndroidView(
        factory = { ctx ->
            val frame = FrameLayout(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            holder.inlineContainer = frame
            val webView = holder.webView ?: createPlayerWebView(
                context = ctx,
                holder = holder,
                sourceState = sourceState,
            ).also { holder.webView = it }
            attachPlayerWebViewIfNeeded(holder, frame, webView)
            schedulePlayerContentLoad(webView, loadKey, url, embedHtml)
            frame
        },
        update = { frame ->
            holder.inlineContainer = frame
            val webView = holder.webView ?: return@AndroidView
            attachPlayerWebViewIfNeeded(holder, frame, webView)
            schedulePlayerContentLoad(webView, loadKey, url, embedHtml)
        },
        onRelease = { frame ->
            if (holder.inlineContainer === frame) {
                holder.inlineContainer = null
            }
        },
        modifier = modifier,
    )
}

/** Сначала в иерархию, потом load — иначе WebView иногда остаётся пустым. */
private fun attachPlayerWebViewIfNeeded(
    holder: PlayerWebViewHolder,
    frame: FrameLayout,
    webView: WebView,
) {
    if (holder.fullscreenOverlay != null || holder.isHtmlFullscreen) return
    if (webView.parent === frame) {
        refreshWebViewSurface(webView)
        return
    }
    (webView.parent as? ViewGroup)?.removeView(webView)
    frame.addView(
        webView,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
    refreshWebViewSurface(webView)
}

private fun schedulePlayerContentLoad(
    webView: WebView,
    loadKey: String,
    url: String?,
    embedHtml: String?,
) {
    if (webView.tag == loadKey) return
    webView.tag = loadKey
    val key = loadKey
    webView.post {
        if (webView.tag != key) return@post
        loadPlayerContent(webView, url, embedHtml)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createPlayerWebView(
    context: android.content.Context,
    holder: PlayerWebViewHolder,
    sourceState: MutableState<String?>,
): WebView = WebView(context).apply {
    setBackgroundColor(android.graphics.Color.BLACK)
    PlayerBrowserProfile.apply(this)
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    installPlayerJsPolyfills(this)
    webChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            val req = request ?: return
            post { req.grant(req.resources) }
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?,
        ): Boolean {
            val parent = view ?: return false
            val popup = WebView(parent.context).apply {
                PlayerBrowserProfile.apply(this)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        v: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        parent.loadUrl(
                            request.url.toString(),
                            PlayerBrowserProfile.browserHeaders(parent.context),
                        )
                        // Popup нужен только для transport — сразу освобождаем.
                        v.post {
                            runCatching {
                                holder.cancelPopupCleanup(v)
                                v.stopLoading()
                                (v.parent as? ViewGroup)?.removeView(v)
                                v.destroy()
                            }
                        }
                        return true
                    }
                }
            }
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            transport.webView = popup
            resultMsg.sendToTarget()
            // Если URL так и не пришёл — не держим orphan WebView.
            val cleanup = Runnable {
                runCatching {
                    holder.cancelPopupCleanup(popup)
                    if (popup.parent == null) {
                        popup.stopLoading()
                        popup.destroy()
                    }
                }
            }
            holder.trackPopupCleanup(popup, cleanup)
            return true
        }

        override fun onCloseWindow(window: WebView?) {
            val w = window ?: return
            runCatching {
                w.stopLoading()
                (w.parent as? ViewGroup)?.removeView(w)
                w.destroy()
            }
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            val v = view ?: run {
                callback?.onCustomViewHidden()
                return
            }
            // Всегда на decor (поверх app-FS), не в Compose-слот 16:9
            holder.showHtmlFullscreen(v, callback)
        }

        override fun onHideCustomView() {
            holder.hideHtmlFullscreen()
        }
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean = false

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            val src = sourceState.value.orEmpty()
            val host = request.url?.host
            val useProxy = src.equals("turbo", true) ||
                src.equals("videoseed", true) ||
                PlayerWebViewHttp.isVideoSeedHost(host)
            if (useProxy) {
                PlayerWebViewHttp.intercept(
                    request,
                    forceHttp1 = src.equals("videoseed", true) ||
                        PlayerWebViewHttp.isVideoSeedHost(host),
                )?.let { return it }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: android.webkit.WebResourceError,
        ) {
            if (request.isForMainFrame) {
                android.util.Log.e(
                    "PlayerWebView",
                    "main error ${request.url} ${error.errorCode} ${error.description}",
                )
            }
        }

        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
            val fs = holder.fullscreenOverlay != null || holder.isHtmlFullscreen
            view?.let { applyPlayerContentZoom(it, enabled = fs) }
        }
    }
}

/** true только Expanded (≥840dp sw); Compact/Medium — без zoom. */
private fun playerZoomAllowed(context: android.content.Context): Boolean =
    context.resources.configuration.smallestScreenWidthDp >= 840

/** Масштаб оболочки плеера; только fullscreen и только Expanded. */
private fun applyPlayerContentZoom(webView: WebView, enabled: Boolean) {
    val on = enabled && playerZoomAllowed(webView.context)
    val z = if (on) PlayerBrowserProfile.PLAYER_CSS_ZOOM else "1"
    webView.settings.textZoom =
        if (on) PlayerBrowserProfile.PLAYER_TEXT_ZOOM else 100
    val js =
        """
        (function(){
          try {
            var z = '$z';
            document.documentElement.style.zoom = z;
            if (document.body) document.body.style.zoom = z;
            var ifr = document.querySelectorAll('iframe');
            for (var i = 0; i < ifr.length; i++) {
              ifr[i].style.zoom = z;
            }
          } catch (e) {}
        })();
        """.trimIndent()
    webView.evaluateJavascript(js, null)
}

/** Polyfill до скриптов во всех фреймах (в т.ч. player.cdnvideohub.com). */
private val PLAYER_JS_POLYFILLS = """
(function(){
  if (typeof URL !== 'undefined' && typeof URL.canParse !== 'function') {
    URL.canParse = function(url, base) {
      try { new URL(url, base); return true; } catch (e) { return false; }
    };
  }
})();
""".trimIndent()

private fun installPlayerJsPolyfills(webView: WebView) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
        WebViewCompat.addDocumentStartJavaScript(
            webView,
            PLAYER_JS_POLYFILLS,
            setOf("*"),
        )
    }
}

/**
 * HTML (shell / Vibix / CVH) → loadDataWithBaseURL(reyoho.com).
 * Collaps / VideoSeed и прочие direct → loadUrl.
 */
private fun loadPlayerContent(
    webView: WebView,
    url: String?,
    embedHtml: String?,
) {
    val ctx = webView.context
    PlayerBrowserProfile.apply(webView)
    if (!embedHtml.isNullOrBlank()) {
        webView.loadDataWithBaseURL(
            SiteHttp.ORIGIN + "/",
            embedHtml,
            "text/html",
            Charsets.UTF_8.name(),
            null,
        )
        return
    }
    val normalized = normalizePlayerUrl(url) ?: return
    webView.loadUrl(normalized, PlayerBrowserProfile.browserHeaders(ctx))
}

private val YoutubeWatchRegex =
    Regex("""(?:youtube\.com/watch\?v=|youtu\.be/)([A-Za-z0-9_-]{6,})""")

private fun normalizePlayerUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val yt = YoutubeWatchRegex.find(url) ?: return url
    return "https://www.youtube.com/embed/${yt.groupValues[1]}?autoplay=1&rel=0"
}


