package com.spywer.yoho.ui.favorites

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.spywer.yoho.MainActivity
import com.spywer.yoho.ui.theme.YohoTheme

/** Общий хост полки коллекции; конкретные Activity задают [shelf]. */
abstract class CollectionShelfActivity : ComponentActivity() {
    protected abstract val shelf: CollectionShelf

    private val viewModel: FavoritesViewModel by viewModels {
        FavoritesViewModel.factory(shelf)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YohoTheme(darkTheme = true) {
                FavoritesScreen(
                    viewModel = viewModel,
                    fixedShelf = shelf,
                    onBack = { finish() },
                    onNeedLogin = {
                        startActivity(MainActivity.intentOpenAccount(this@CollectionShelfActivity))
                        finish()
                    },
                )
            }
        }
    }
}

class FavoritesListActivity : CollectionShelfActivity() {
    override val shelf: CollectionShelf = CollectionShelf.Favorites

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, FavoritesListActivity::class.java)
    }
}

class DiaryActivity : CollectionShelfActivity() {
    override val shelf: CollectionShelf = CollectionShelf.Diary

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, DiaryActivity::class.java)
    }
}

class HistoryActivity : CollectionShelfActivity() {
    override val shelf: CollectionShelf = CollectionShelf.History

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, HistoryActivity::class.java)
    }
}
