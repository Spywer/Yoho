package com.spywer.yoho.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.SiteHttp
import com.spywer.yoho.data.remote.TrailerClipDto
import com.spywer.yoho.ui.layout.LocalHomeMetrics

private val YoutubeIdRegex =
    Regex(
        """(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/|youtube-nocookie\.com/embed/)([A-Za-z0-9_-]{6,})|^([A-Za-z0-9_-]{6,})$""",
    )

enum class TrailerSource { Youtube, Vk }

/** watch / embed / ключ → video id (пример: HF1_epZNoCg). */
fun youtubeVideoId(urlOrKey: String?): String? {
    if (urlOrKey.isNullOrBlank()) return null
    return YoutubeIdRegex.find(urlOrKey.trim())
        ?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }
        ?.takeIf { it.isNotBlank() }
}

/** Есть ли валидный youtube id (для клика по трейлеру). */
fun youtubeEmbedUrl(urlOrKey: String?): String? {
    val id = youtubeVideoId(urlOrKey) ?: return null
    return "https://www.youtube.com/embed/$id"
}

@Composable
fun TrailerPlayerDialog(
    urlOrKey: String,
    title: String? = null,
    onDismiss: () -> Unit,
) {
    val clip = remember(urlOrKey, title) {
        TrailerClipDto(
            title = title,
            youtubeKey = youtubeVideoId(urlOrKey),
            vkEmbedUrl = urlOrKey.takeIf {
                it.contains("vk.com", true) || it.contains("vkvideo.ru", true)
            },
        )
    }
    TrailerPlayerDialog(
        clips = listOfNotNull(clip.takeIf { it.isPlayable }),
        title = title,
        onDismiss = onDismiss,
    )
}

@Composable
fun TrailerPlayerDialog(
    clips: List<TrailerClipDto>,
    title: String? = null,
    onDismiss: () -> Unit,
) {
    val playable = remember(clips) { clips.filter { it.isPlayable } }
    LaunchedEffect(playable) {
        if (playable.isEmpty()) onDismiss()
    }
    if (playable.isEmpty()) return

    val hasYoutube = playable.any { it.hasYoutube }
    val hasVk = playable.any { it.hasVk }
    var source by remember(hasYoutube, hasVk) {
        mutableStateOf(
            when {
                hasYoutube -> TrailerSource.Youtube
                else -> TrailerSource.Vk
            },
        )
    }
    val sourceClips = remember(playable, source) {
        when (source) {
            TrailerSource.Youtube -> playable.filter { it.hasYoutube }
            TrailerSource.Vk -> playable.filter { it.hasVk }
        }
    }
    LaunchedEffect(sourceClips) {
        if (sourceClips.isEmpty()) {
            source = if (source == TrailerSource.Youtube && hasVk) {
                TrailerSource.Vk
            } else if (hasYoutube) {
                TrailerSource.Youtube
            } else {
                source
            }
        }
    }
    var selectedIndex by remember(sourceClips) { mutableIntStateOf(0) }
    val selected = sourceClips.getOrNull(selectedIndex.coerceIn(0, (sourceClips.size - 1).coerceAtLeast(0)))

    val metrics = LocalHomeMetrics.current
    val showSourceTabs = hasYoutube && hasVk
    val showClipPicker = sourceClips.size > 1
    val landscape = rememberTrailerLandscape()

    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        // Dialog при Activity.configChanges часто не меняет размер окна — форсим MATCH_PARENT.
        TrailerDialogWindowSync(landscape = landscape)
        TrailerSystemBarsEffect(hidden = landscape)
        Box(
            Modifier
                .fillMaxSize()
                .background(if (landscape) Color.Black else Color.Black.copy(alpha = 0.92f))
                .then(
                    if (landscape) {
                        Modifier
                    } else {
                        Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    },
                )
                .clickable(onClick = onDismiss),
        ) {
            if (landscape) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                ) {
                    TrailerPlayerSlot(
                        source = source,
                        selected = selected,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                val padH = if (metrics.isTablet) 24.dp else 8.dp
                val cardPad = if (metrics.isTablet) 12.dp else 8.dp
                val gapAfterHeader = if (metrics.isTablet) 10.dp else 6.dp
                Column(
                    Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .align(Alignment.Center)
                        .padding(horizontal = padH),
                ) {
                    Spacer(Modifier.weight(1f))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(if (metrics.isTablet) 16.dp else 12.dp))
                            .background(Color(0xFF1A1A1A))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {},
                            )
                            .padding(cardPad),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                if (metrics.isTablet) 8.dp else 6.dp,
                            ),
                        ) {
                            if (showSourceTabs) {
                                SourceChip(
                                    label = stringResource(R.string.detail_trailer_source_youtube),
                                    selected = source == TrailerSource.Youtube,
                                    onClick = {
                                        source = TrailerSource.Youtube
                                        selectedIndex = 0
                                    },
                                )
                                SourceChip(
                                    label = stringResource(R.string.detail_trailer_source_vk),
                                    selected = source == TrailerSource.Vk,
                                    onClick = {
                                        source = TrailerSource.Vk
                                        selectedIndex = 0
                                    },
                                )
                                Spacer(Modifier.weight(1f))
                            } else {
                                Text(
                                    text = title?.takeIf { it.isNotBlank() }
                                        ?: selected?.title
                                        ?: stringResource(R.string.detail_trailer),
                                    color = Color.White,
                                    fontSize = metrics.searchTextSize,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            TrailerCloseButton(
                                onClick = onDismiss,
                                compact = !metrics.isTablet,
                            )
                        }

                        Spacer(Modifier.padding(top = gapAfterHeader))

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(if (metrics.isTablet) 12.dp else 8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            TrailerPlayerSlot(
                                source = source,
                                selected = selected,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = if (metrics.isTablet) 12.dp else 8.dp),
                    ) {
                        if (showClipPicker) {
                            ClipGrid(
                                clips = sourceClips,
                                selectedIndex = selectedIndex,
                                onSelect = { selectedIndex = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = maxHeight)
                                    .clip(RoundedCornerShape(if (metrics.isTablet) 12.dp else 8.dp))
                                    .background(Color(0xFF1A1A1A))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {},
                                    )
                                    .padding(if (metrics.isTablet) 10.dp else 6.dp)
                                    .verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Альбом по Activity.configuration + FULL_SENSOR (LocalConfiguration у Dialog врёт/запаздывает). */
@Composable
private fun rememberTrailerLandscape(): Boolean {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    fun Configuration.isLandscape(): Boolean =
        orientation == Configuration.ORIENTATION_LANDSCAPE || screenWidthDp > screenHeightDp

    var landscape by remember(activity) {
        val cfg = activity?.resources?.configuration ?: context.resources.configuration
        mutableStateOf(cfg.isLandscape())
    }

    DisposableEffect(activity) {
        if (activity == null) {
            return@DisposableEffect onDispose { }
        }
        val previous = activity.requestedOrientation
        // FULL_SENSOR — и «обычный», и перевёрнутый альбом/портрет.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        val callbacks = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                landscape = newConfig.isLandscape()
            }

            override fun onLowMemory() = Unit
        }
        activity.registerComponentCallbacks(callbacks)
        // Синхронизация на случай, если колбэк уже прошёл.
        landscape = activity.resources.configuration.isLandscape()
        onDispose {
            activity.unregisterComponentCallbacks(callbacks)
            activity.requestedOrientation = previous
        }
    }
    return landscape
}

@Composable
private fun TrailerDialogWindowSync(landscape: Boolean) {
    val view = LocalView.current
    // Только при смене ориентации — постоянный requestLayout в SideEffect гасит WebView (VK).
    LaunchedEffect(landscape) {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        window.decorView.requestLayout()
    }
}

@Composable
private fun TrailerPlayerSlot(
    source: TrailerSource,
    selected: TrailerClipDto?,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            selected == null -> {
                Text(
                    text = stringResource(R.string.detail_trailer),
                    color = Color.White,
                    modifier = Modifier.padding(24.dp),
                )
            }
            source == TrailerSource.Youtube && selected.hasYoutube -> {
                YoutubeTrailerPlayer(
                    videoId = selected.youtubeKey.orEmpty(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            source == TrailerSource.Vk && selected.hasVk -> {
                VkTrailerPlayer(
                    embedUrl = selected.vkEmbedUrl.orEmpty(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TrailerCloseButton(
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(if (compact) 28.dp else 40.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 16.dp else 22.dp),
        )
    }
}

@Composable
private fun TrailerSystemBarsEffect(hidden: Boolean) {
    val view = LocalView.current
    DisposableEffect(hidden, view) {
        val window = (view.parent as? DialogWindowProvider)?.window
            ?: view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (hidden) {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val metrics = LocalHomeMetrics.current
    Text(
        text = label,
        color = if (selected) Color.White else Color(0xFFBDBDBD),
        fontSize = metrics.searchTextSize,
        lineHeight = metrics.lineOf(metrics.searchTextSize),
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(if (metrics.isTablet) 20.dp else 14.dp))
            .background(if (selected) Color(0xFF333333) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (metrics.isTablet) 16.dp else 10.dp,
                vertical = if (metrics.isTablet) 10.dp else 6.dp,
            ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClipGrid(
    clips: List<TrailerClipDto>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalHomeMetrics.current
    val compact = !metrics.isTablet
    val gap = if (compact) 4.dp else 8.dp
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
        maxItemsInEachRow = if (compact) 1 else 2,
    ) {
        clips.forEachIndexed { index, clip ->
            val active = index == selectedIndex
            Text(
                text = clip.title?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.detail_trailer),
                color = if (active) Color.White else Color(0xFFB0B0B0),
                fontSize = metrics.cardMetaSize,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = if (compact) TextAlign.Start else TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .then(if (compact) Modifier.fillMaxWidth() else Modifier.weight(1f))
                    .clip(RoundedCornerShape(if (compact) 8.dp else 10.dp))
                    .background(if (active) Color(0xFF2F2F2F) else Color(0xFF242424))
                    .clickable { onSelect(index) }
                    .padding(
                        horizontal = if (compact) 8.dp else 10.dp,
                        vertical = if (compact) 7.dp else 12.dp,
                    ),
            )
        }
    }
}

@Composable
private fun YoutubeTrailerPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localView = LocalView.current
    val activity = LocalContext.current.findActivity()
    var loading by remember(videoId) { mutableStateOf(true) }
    var errorText by remember(videoId) { mutableStateOf<String?>(null) }
    // FS-слой держим сами: Dialog — отдельное Window, Activity.decorView под ним → «видео пропало».
    var fsOverlay by remember { mutableStateOf<View?>(null) }
    var exitFs by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun clearFullscreenOverlay() {
        fsOverlay?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        fsOverlay = null
        exitFs = null
    }

    DisposableEffect(Unit) {
        onDispose { clearFullscreenOverlay() }
    }

    // Back в FS YouTube → выходим из FS, не закрываем диалог.
    BackHandler(enabled = fsOverlay != null) {
        exitFs?.invoke() ?: clearFullscreenOverlay()
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        key(videoId) {
            AndroidView(
                factory = { ctx ->
                    YouTubePlayerView(ctx).apply {
                        enableAutomaticInitialization = false
                        lifecycleOwner.lifecycle.addObserver(this)
                        addFullscreenListener(object : FullscreenListener {
                            override fun onEnterFullscreen(
                                fullscreenView: View,
                                exitFullscreen: () -> Unit,
                            ) {
                                val host = dialogFullscreenHost(localView, activity) ?: return
                                clearFullscreenOverlay()
                                fullscreenView.setBackgroundColor(AndroidColor.BLACK)
                                fullscreenView.elevation = 64f
                                host.addView(
                                    fullscreenView,
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    ),
                                )
                                fsOverlay = fullscreenView
                                exitFs = exitFullscreen
                            }

                            override fun onExitFullscreen() {
                                clearFullscreenOverlay()
                            }
                        })
                        val options = IFramePlayerOptions.Builder(ctx)
                            .controls(1)
                            .fullscreen(1)
                            .autoplay(1)
                            .rel(0)
                            .origin(SiteHttp.ORIGIN.trimEnd('/'))
                            .build()
                        initialize(
                            object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.loadVideo(videoId, 0f)
                                    loading = false
                                    errorText = null
                                }

                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: PlayerConstants.PlayerError,
                                ) {
                                    loading = false
                                    errorText = "Не удалось воспроизвести ($error)"
                                }

                                override fun onStateChange(
                                    youTubePlayer: YouTubePlayer,
                                    state: PlayerConstants.PlayerState,
                                ) {
                                    if (state == PlayerConstants.PlayerState.PLAYING ||
                                        state == PlayerConstants.PlayerState.PAUSED ||
                                        state == PlayerConstants.PlayerState.BUFFERING
                                    ) {
                                        loading = false
                                    }
                                }
                            },
                            handleNetworkEvents = true,
                            playerOptions = options,
                            videoId = videoId,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view ->
                    clearFullscreenOverlay()
                    lifecycleOwner.lifecycle.removeObserver(view)
                    view.release()
                },
            )
        }
        when {
            errorText != null -> {
                Text(
                    text = errorText.orEmpty(),
                    color = Color.White,
                    modifier = Modifier.padding(24.dp),
                )
            }
            loading -> CircularProgressIndicator(color = Color.White)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Host для FS: окно Compose Dialog, иначе Activity (иначе слой уходит «под» Dialog). */
private fun dialogFullscreenHost(localView: View, activity: Activity?): ViewGroup? {
    var p: Any? = localView.parent
    while (p != null) {
        if (p is DialogWindowProvider) {
            return p.window.decorView as? ViewGroup
        }
        p = (p as? View)?.parent
    }
    return activity?.window?.decorView as? ViewGroup
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VkTrailerPlayer(
    embedUrl: String,
    modifier: Modifier = Modifier,
) {
    val localView = LocalView.current
    val activity = LocalContext.current.findActivity()
    var loading by remember(embedUrl) { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var inlineHost by remember { mutableStateOf<FrameLayout?>(null) }
    var fsOverlay by remember { mutableStateOf<View?>(null) }
    val isFullscreen = fsOverlay != null

    fun exitVkFullscreen() {
        val overlay = fsOverlay ?: return
        val web = webViewRef
        val inline = inlineHost
        fsOverlay = null
        // Снять FS-слой (наш FrameLayout или HTML custom view от VK).
        if (web != null && web.parent === overlay) {
            (overlay as? ViewGroup)?.removeView(web)
        }
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        // Вернуть WebView в инлайн, если он был вынут.
        if (web != null && inline != null && web.parent !== inline) {
            (web.parent as? ViewGroup)?.removeView(web)
            inline.addView(
                web,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exitVkFullscreen()
        }
    }

    BackHandler(enabled = isFullscreen) {
        exitVkFullscreen()
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        key(embedUrl) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).also { host ->
                        inlineHost = host
                        val web = WebView(ctx).apply {
                            setBackgroundColor(AndroidColor.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.userAgentString = SiteHttp.userAgent(ctx)
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(
                                    view: View?,
                                    callback: CustomViewCallback?,
                                ) {
                                    // VK/HTML FS — тоже на окно диалога.
                                    if (view == null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    val fsHost = dialogFullscreenHost(localView, activity)
                                    if (fsHost == null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    exitVkFullscreen()
                                    view.setBackgroundColor(AndroidColor.BLACK)
                                    view.elevation = 64f
                                    fsHost.addView(
                                        view,
                                        ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                        ),
                                    )
                                    fsOverlay = view
                                }

                                override fun onHideCustomView() {
                                    exitVkFullscreen()
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                }
                            }
                            onResume()
                            resumeTimers()
                            loadUrl(embedUrl, mapOf("Referer" to SiteHttp.REFERER))
                        }
                        webViewRef = web
                        host.addView(
                            web,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                },
                update = { host ->
                    inlineHost = host
                    webViewRef?.let { web ->
                        runCatching {
                            web.onResume()
                            web.resumeTimers()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { host ->
                    exitVkFullscreen()
                    webViewRef?.apply {
                        stopLoading()
                        loadUrl("about:blank")
                        (parent as? ViewGroup)?.removeView(this)
                        destroy()
                    }
                    webViewRef = null
                    inlineHost = null
                    (host.parent as? ViewGroup)?.removeView(host)
                },
            )
        }
        if (loading) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
