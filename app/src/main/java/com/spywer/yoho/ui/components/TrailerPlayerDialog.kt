package com.spywer.yoho.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.spywer.yoho.data.remote.SiteHttp
import com.spywer.yoho.ui.layout.LocalHomeMetrics

private val YoutubeIdRegex =
    Regex(
        """(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/|youtube-nocookie\.com/embed/)([A-Za-z0-9_-]{6,})|^([A-Za-z0-9_-]{6,})$""",
    )

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
    val videoId = remember(urlOrKey) { youtubeVideoId(urlOrKey) }
    LaunchedEffect(videoId) {
        if (videoId == null) onDismiss()
    }
    if (videoId == null) return

    val metrics = LocalHomeMetrics.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

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
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title?.takeIf { it.isNotBlank() } ?: "Трейлер",
                    color = Color.White,
                    fontSize = metrics.searchTextSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            enableAutomaticInitialization = false
                            lifecycleOwner.lifecycle.addObserver(this)
                            // origin обязан быть валидным https-доменом (не packageName).
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
                        lifecycleOwner.lifecycle.removeObserver(view)
                        view.release()
                    },
                )

                when {
                    errorText != null -> {
                        Text(
                            text = errorText.orEmpty(),
                            color = Color.White,
                            fontSize = metrics.searchTextSize,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                    loading -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}
