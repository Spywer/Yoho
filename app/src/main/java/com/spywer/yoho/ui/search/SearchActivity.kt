package com.spywer.yoho.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.spywer.yoho.ui.theme.YohoTheme

class SearchActivity : ComponentActivity() {
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialQuery = intent.getStringExtra(EXTRA_QUERY)
        val initialGenre = intent.getStringExtra(EXTRA_GENRE)
        val initialType = intent.getStringExtra(EXTRA_TYPE)
        setContent {
            YohoTheme(darkTheme = true) {
                LaunchedEffect(initialQuery, initialGenre, initialType) {
                    viewModel.setInitial(
                        query = initialQuery,
                        genre = initialGenre,
                        type = initialType,
                    )
                }
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_QUERY = "extra_query"
        const val EXTRA_GENRE = "extra_genre"
        const val EXTRA_TYPE = "extra_type"

        fun intent(
            context: Context,
            query: String? = null,
            genre: String? = null,
            type: String? = null,
        ): Intent =
            Intent(context, SearchActivity::class.java).apply {
                if (!query.isNullOrBlank()) putExtra(EXTRA_QUERY, query)
                if (!genre.isNullOrBlank()) putExtra(EXTRA_GENRE, genre)
                if (!type.isNullOrBlank()) putExtra(EXTRA_TYPE, type)
            }
    }
}
