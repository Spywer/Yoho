package com.spywer.yoho.ui.trailer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.spywer.yoho.data.remote.TrailerClipDto
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.theme.YohoTheme

/**
 * Отдельная Activity для трейлеров: не трогает Main/Detail и кино-плееры.
 * Ориентацию не форсим — решает система (автоповорот пользователя).
 */
class TrailerPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val title = intent.getStringExtra(EXTRA_TITLE)
        val clips = readClips(intent)
        if (clips.isEmpty()) {
            finish()
            return
        }
        setContent {
            YohoTheme(darkTheme = true) {
                val metrics = rememberHomeMetrics()
                CompositionLocalProvider(LocalHomeMetrics provides metrics) {
                    TrailerPlayerScreen(
                        clips = clips,
                        title = title,
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_trailer_title"
        private const val EXTRA_CLIP_TITLES = "extra_clip_titles"
        private const val EXTRA_CLIP_YT = "extra_clip_yt"
        private const val EXTRA_CLIP_VK = "extra_clip_vk"

        fun intent(
            context: Context,
            urlOrKey: String,
            title: String? = null,
        ): Intent {
            val clip = TrailerClipDto(
                title = title,
                youtubeKey = youtubeVideoId(urlOrKey),
                vkEmbedUrl = urlOrKey.takeIf {
                    it.contains("vk.com", true) || it.contains("vkvideo.ru", true)
                },
            )
            return intent(context, listOfNotNull(clip.takeIf { it.isPlayable }), title)
        }

        fun intent(
            context: Context,
            clips: List<TrailerClipDto>,
            title: String? = null,
        ): Intent {
            val playable = clips.filter { it.isPlayable }
            return Intent(context, TrailerPlayerActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(EXTRA_TITLE, title)
                putStringArrayListExtra(
                    EXTRA_CLIP_TITLES,
                    ArrayList(playable.map { it.title.orEmpty() }),
                )
                putStringArrayListExtra(
                    EXTRA_CLIP_YT,
                    ArrayList(playable.map { it.youtubeKey.orEmpty() }),
                )
                putStringArrayListExtra(
                    EXTRA_CLIP_VK,
                    ArrayList(playable.map { it.vkEmbedUrl.orEmpty() }),
                )
            }
        }

        private fun readClips(intent: Intent): List<TrailerClipDto> {
            val titles = intent.getStringArrayListExtra(EXTRA_CLIP_TITLES).orEmpty()
            val yt = intent.getStringArrayListExtra(EXTRA_CLIP_YT).orEmpty()
            val vk = intent.getStringArrayListExtra(EXTRA_CLIP_VK).orEmpty()
            val n = maxOf(titles.size, yt.size, vk.size)
            if (n == 0) return emptyList()
            return List(n) { i ->
                TrailerClipDto(
                    title = titles.getOrNull(i)?.takeIf { it.isNotBlank() },
                    youtubeKey = yt.getOrNull(i)?.takeIf { it.isNotBlank() },
                    vkEmbedUrl = vk.getOrNull(i)?.takeIf { it.isNotBlank() },
                )
            }.filter { it.isPlayable }
        }
    }
}
