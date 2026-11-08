package com.spywer.yoho.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.spywer.yoho.MainActivity
import com.spywer.yoho.ui.theme.YohoTheme

class SearchActivity : ComponentActivity() {
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialQuery = intent.getStringExtra(EXTRA_QUERY)
        val initialGenre = intent.getStringExtra(EXTRA_GENRE)
        val initialType = intent.getStringExtra(EXTRA_TYPE)
        val initialActor = intent.getStringExtra(EXTRA_ACTOR)
        val initialActorKp = intent.getStringExtra(EXTRA_ACTOR_KP)
        setContent {
            YohoTheme(darkTheme = true) {
                LaunchedEffect(initialQuery, initialGenre, initialType, initialActor, initialActorKp) {
                    viewModel.setInitial(
                        query = initialQuery,
                        genre = initialGenre,
                        type = initialType,
                        actor = initialActor,
                        actorKp = initialActorKp,
                    )
                }
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onOpenTitle = { idKp, idTmdb, mediaType ->
                        startActivity(MainActivity.intentOpenTitle(this, idKp, idTmdb, mediaType, "catalog"))
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_QUERY = "extra_query"
        const val EXTRA_GENRE = "extra_genre"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_ACTOR = "extra_actor"
        const val EXTRA_ACTOR_KP = "extra_actor_kp"

        fun intent(
            context: Context,
            query: String? = null,
            genre: String? = null,
            type: String? = null,
            actor: String? = null,
            actorKp: String? = null,
        ): Intent =
            Intent(context, SearchActivity::class.java).apply {
                if (!query.isNullOrBlank()) putExtra(EXTRA_QUERY, query)
                if (!genre.isNullOrBlank()) putExtra(EXTRA_GENRE, genre)
                if (!type.isNullOrBlank()) putExtra(EXTRA_TYPE, type)
                if (!actor.isNullOrBlank()) putExtra(EXTRA_ACTOR, actor)
                if (!actorKp.isNullOrBlank()) putExtra(EXTRA_ACTOR_KP, actorKp)
            }
    }
}
