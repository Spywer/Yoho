package com.spywer.yoho.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.spywer.yoho.MainActivity

/**
 * Совместимость: раньше полки были отдельными Activity без навбара.
 * Теперь сразу уводим в Main → вкладка Коллекция + полка.
 */
abstract class CollectionShelfActivity : ComponentActivity() {
    protected abstract val shelf: CollectionShelf

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startActivity(MainActivity.intentOpenShelf(this, shelf))
        finish()
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
