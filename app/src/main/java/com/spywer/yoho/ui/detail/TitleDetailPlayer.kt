package com.spywer.yoho.ui.detail

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Message
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
    padding: androidx.compose.ui.unit.Dp,
    metaSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
) {
    val playUrl = remember(iframeUrl) { normalizePlayerUrl(iframeUrl) }
    val hasPlayer = !playUrl.isNullOrBlank() || !embedHtml.isNullOrBlank()
    val noSources = !needsLogin && choices.isEmpty() && !hasPlayer
    val context = LocalContext.current
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var htmlFullscreen by remember { mutableStateOf(false) }
    // Toast дневника, когда панели под плеером нет (заглушка без источников).
    LaunchedEffect(diaryNotify, hasPlayer) {
        if (hasPlayer) return@LaunchedEffect
        val msg = diaryNotify ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        onConsumeDiaryNotify()
    }
    // WebView всегда в Compose; FS = reparent на decorView (Dialog ломал surface при возврате)
    val webHolder = remember { PlayerWebViewHolder() }
    DisposableEffect(Unit) {
        webHolder.onHtmlFullscreenChanged = { htmlFullscreen = it }
        onDispose { webHolder.destroy() }
    }
    DisposableEffect(hasPlayer) {
        if (!hasPlayer) webHolder.pausePlayback()
        onDispose { }
    }

    BackHandler(enabled = htmlFullscreen || fullscreen) {
        when {
            htmlFullscreen -> webHolder.requestExitHtmlFullscreen()
            else -> fullscreen = false
        }
    }
    val fsActive = (fullscreen || htmlFullscreen) && hasPlayer
    PlayerSystemBarsEffect(hidden = fsActive)
    PlayerFullscreenOrientationEffect(active = fsActive)
    PlayerFullscreenEffect(
        enabled = fullscreen && hasPlayer,
        holder = webHolder,
        onExit = {
            if (webHolder.isHtmlFullscreen) webHolder.requestExitHtmlFullscreen()
            fullscreen = false
        },
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
        if (loading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                )
            }
        } else {
            if (choices.isNotEmpty()) {
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

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                when {
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
                                text = when {
                                    needsLogin -> stringResource(R.string.detail_player_needs_login)
                                    selectedSource != null ->
                                        "Источник: ${displayPlayerSourceName(selectedSource)}"
                                    else -> stringResource(R.string.detail_player_pick)
                                },
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = metaSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            if (!needsLogin) {
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
                }
            }

            // Как на сайте: панель под плеером (без «Запретки»)
            if (hasPlayer) {
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

private val diaryStatusLabelRes = listOf(
    R.string.detail_diary_watched,
    R.string.detail_diary_watching,
    R.string.detail_diary_want,
    R.string.detail_diary_rewatching,
    R.string.detail_diary_on_hold,
    R.string.detail_diary_dropped,
)

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
                DropdownMenu(
                    expanded = diaryMenuOpen,
                    onDismissRequest = { diaryMenuOpen = false },
                    modifier = Modifier.widthIn(min = if (metrics.isTablet) 340.dp else 180.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (inDiary) {
                                R.string.detail_diary_menu_title_active
                            } else {
                                R.string.detail_diary_menu_title
                            },
                        ).uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = metrics.diaryMenuTitle,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(
                            horizontal = if (metrics.isTablet) 20.dp else 10.dp,
                            vertical = if (metrics.isTablet) 14.dp else 4.dp,
                        ),
                    )
                    diaryStatusLabelRes.forEach { labelRes ->
                        val label = stringResource(labelRes)
                        val selected = diaryStatusLabel == label
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontSize = metrics.diaryMenuItem,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selected) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                            onClick = {
                                diaryMenuOpen = false
                                if (!selected) onSetDiaryStatus(label)
                            },
                            modifier = Modifier
                                .then(
                                    if (metrics.isTablet) {
                                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    } else {
                                        Modifier
                                            .padding(horizontal = 4.dp)
                                            .height(36.dp)
                                    },
                                )
                                .clip(RoundedCornerShape(if (metrics.isTablet) 10.dp else 6.dp))
                                .then(
                                    if (selected) {
                                        Modifier.background(MaterialTheme.colorScheme.primary)
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentPadding = PaddingValues(
                                horizontal = if (metrics.isTablet) 16.dp else 10.dp,
                                vertical = if (metrics.isTablet) 12.dp else 0.dp,
                            ),
                        )
                    }
                    if (inDiary) {
                        HorizontalDivider(
                            Modifier.padding(vertical = if (metrics.isTablet) 8.dp else 2.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.detail_diary_remove),
                                    fontSize = metrics.diaryMenuItem,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF87171),
                                )
                            },
                            onClick = {
                                diaryMenuOpen = false
                                onRemoveFromDiary()
                            },
                            modifier = if (metrics.isTablet) {
                                Modifier
                            } else {
                                Modifier.height(36.dp)
                            },
                            contentPadding = PaddingValues(
                                horizontal = if (metrics.isTablet) 20.dp else 10.dp,
                                vertical = if (metrics.isTablet) 14.dp else 0.dp,
                            ),
                        )
                    }
                }
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
private fun PlayerSystemBarsEffect(hidden: Boolean) {
    val context = LocalContext.current
    DisposableEffect(hidden) {
        val activity = context as? Activity
        val controller = activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
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
}

/** Держит WebView + inline-контейнер для reparent в fullscreen overlay. */
private class PlayerWebViewHolder {
    var webView: WebView? = null
    var inlineContainer: FrameLayout? = null
    var fullscreenOverlay: FrameLayout? = null
    /** HTML5 / встроенный FS плеера — отдельный слой на decor, не Compose-слот. */
    var htmlFsContainer: FrameLayout? = null
    var htmlFsCallback: WebChromeClient.CustomViewCallback? = null
    var onHtmlFullscreenChanged: ((Boolean) -> Unit)? = null

    val isHtmlFullscreen: Boolean get() = htmlFsContainer != null

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
        (view.parent as? ViewGroup)?.removeView(view)
        root.addView(view, fullscreenPlayerLayoutParams(activity))
        bindFullscreenLayoutListener(root, view)
        val close = android.widget.ImageButton(activity).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            contentDescription =
                activity.getString(R.string.detail_player_fullscreen_exit)
            setOnClickListener { requestExitHtmlFullscreen() }
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        root.addView(
            close,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.END or android.view.Gravity.TOP,
            ).also {
                val m = (8 * density).toInt()
                it.setMargins(m, m, m, m)
            },
        )
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
            val cb = htmlFsCallback
            htmlFsCallback = null
            if (htmlFsContainer != null) hideHtmlFullscreen()
            // Если HTML FS ещё активен с точки зрения Chromium — отпускаем callback.
            if (cb != null) runCatching { cb.onCustomViewHidden() }
            fullscreenOverlay?.let { overlay ->
                (overlay.parent as? ViewGroup)?.removeView(overlay)
            }
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
 * Альбом — на весь экран; портрет — 16:9 по центру (чёрные поля сверху/снизу).
 */
private fun fullscreenPlayerLayoutParams(
    context: android.content.Context,
): FrameLayout.LayoutParams {
    val dm = context.resources.displayMetrics
    return fullscreenPlayerLayoutParams(dm.widthPixels, dm.heightPixels)
}

private fun fullscreenPlayerLayoutParams(
    containerW: Int,
    containerH: Int,
): FrameLayout.LayoutParams {
    if (containerW <= 0 || containerH <= 0) {
        return FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }
    // Альбом / квадрат → edge-to-edge
    if (containerW >= containerH) {
        return FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }
    val byWidthH = (containerW * 9f / 16f).toInt()
    val (w, h) = if (byWidthH <= containerH) {
        containerW to byWidthH
    } else {
        (containerH * 16f / 9f).toInt() to containerH
    }
    return FrameLayout.LayoutParams(w, h, android.view.Gravity.CENTER)
}

/** Мягко подгоняет child при смене размера контейнера (поворот), без мигания surface. */
private fun bindFullscreenLayoutListener(
    container: FrameLayout,
    playerView: View,
) {
    container.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        val w = right - left
        val h = bottom - top
        val oldW = oldRight - oldLeft
        val oldH = oldBottom - oldTop
        if (w == oldW && h == oldH) return@addOnLayoutChangeListener
        if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
        val next = fullscreenPlayerLayoutParams(w, h)
        val cur = playerView.layoutParams as? FrameLayout.LayoutParams
        if (cur != null &&
            cur.width == next.width &&
            cur.height == next.height &&
            cur.gravity == next.gravity
        ) {
            return@addOnLayoutChangeListener
        }
        playerView.layoutParams = next
        v.requestLayout()
    }
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
    DisposableEffect(enabled) {
        val activity = context as? Activity
        val webView = holder.webView
        val container = holder.inlineContainer
        if (enabled && activity != null && webView != null && container != null) {
            val decor = activity.window.decorView as ViewGroup
            val density = context.resources.displayMetrics.density
            val overlay = FrameLayout(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                isClickable = true
                isFocusable = true
            }
            (webView.parent as? ViewGroup)?.removeView(webView)
            overlay.addView(webView, fullscreenPlayerLayoutParams(context))
            bindFullscreenLayoutListener(overlay, webView)
            val close = android.widget.ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                contentDescription =
                    context.getString(R.string.detail_player_fullscreen_exit)
                setOnClickListener {
                    if (holder.isHtmlFullscreen) holder.requestExitHtmlFullscreen()
                    else onExit()
                }
                val pad = (8 * density).toInt()
                setPadding(pad, pad, pad, pad)
            }
            overlay.addView(
                close,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.END or android.view.Gravity.TOP,
                ).also {
                    val m = (8 * density).toInt()
                    it.setMargins(m, m, m, m)
                },
            )
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
        onDispose {
            if (holder.isHtmlFullscreen) {
                holder.hideHtmlFullscreen()
            }
            val wv = holder.webView
            val overlay = holder.fullscreenOverlay
            val inline = holder.inlineContainer
            if (overlay != null) {
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
                (overlay.parent as? ViewGroup)?.removeView(overlay)
                holder.fullscreenOverlay = null
            }
        }
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
            ).also { created ->
                holder.webView = created
                created.tag = loadKey
                loadPlayerContent(created, url, embedHtml)
            }
            if (holder.fullscreenOverlay == null) {
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
            frame
        },
        update = { frame ->
            holder.inlineContainer = frame
            val webView = holder.webView ?: return@AndroidView
            if (webView.tag != loadKey) {
                webView.tag = loadKey
                loadPlayerContent(webView, url, embedHtml)
            }
            if (holder.fullscreenOverlay == null &&
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
            return true
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
            android.util.Log.i("PlayerWebView", "finished $finishedUrl")
            val fs = holder.fullscreenOverlay != null || holder.isHtmlFullscreen
            view?.let { applyPlayerContentZoom(it, enabled = fs) }
        }
    }
}

/** true только Expanded (≥840dp); Compact/Medium — без zoom. */
private fun playerZoomAllowed(context: android.content.Context): Boolean =
    context.resources.configuration.screenWidthDp >= 840

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


