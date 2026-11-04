package com.spywer.yoho.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.spywer.yoho.ui.search.SearchActivity
import com.spywer.yoho.ui.search.genreSlugOf
import com.spywer.yoho.ui.theme.YohoTheme

class TitleDetailActivity : ComponentActivity() {
    private val viewModel: TitleDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val idKp = intent.getIntExtra(EXTRA_ID_KP, -1).takeIf { it > 0 }
        val idTmdb = intent.getIntExtra(EXTRA_ID_TMDB, -1).takeIf { it > 0 }
        val mediaType = intent.getStringExtra(EXTRA_MEDIA_TYPE)
        setContent {
            YohoTheme(darkTheme = true) {
                LaunchedEffect(idKp, idTmdb, mediaType) {
                    viewModel.load(idKp, idTmdb, mediaType)
                }
                TitleDetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onOpenTitle = { kp, tmdb, type ->
                        startActivity(intent(this@TitleDetailActivity, kp, tmdb, type))
                    },
                    onOpenGenre = { genreLabel, type ->
                        val slug = genreSlugOf(genreLabel) ?: return@TitleDetailScreen
                        startActivity(
                            SearchActivity.intent(
                                this@TitleDetailActivity,
                                genre = slug,
                                type = type,
                            ),
                        )
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ID_KP = "extra_id_kp"
        const val EXTRA_ID_TMDB = "extra_id_tmdb"
        const val EXTRA_MEDIA_TYPE = "extra_media_type"

        fun intent(
            context: Context,
            idKp: Int?,
            idTmdb: Int? = null,
            mediaType: String? = null,
        ): Intent = Intent(context, TitleDetailActivity::class.java).apply {
            putExtra(EXTRA_ID_KP, idKp ?: -1)
            putExtra(EXTRA_ID_TMDB, idTmdb ?: -1)
            putExtra(EXTRA_MEDIA_TYPE, mediaType)
        }
    }
}
