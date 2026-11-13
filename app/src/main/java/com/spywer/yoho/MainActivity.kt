package com.spywer.yoho

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spywer.yoho.ui.detail.DetailPipController
import com.spywer.yoho.ui.detail.LocalDetailPip
import com.spywer.yoho.ui.detail.buildDetailPipParams
import com.spywer.yoho.ui.detail.onDetailPipModeChanged
import com.spywer.yoho.ui.detail.onDetailPipUserLeave
import com.spywer.yoho.ui.detail.supportsPip
import com.spywer.yoho.ui.collection.CollectionShelf
import com.spywer.yoho.ui.main.MainShell
import com.spywer.yoho.ui.main.MainTab
import com.spywer.yoho.ui.main.TitleOpenRequest
import com.spywer.yoho.ui.theme.YohoTheme
import kotlinx.coroutines.flow.MutableStateFlow

/** Хост табов + in-app деталка; PiP при загруженном плеере (Home). */
class MainActivity : ComponentActivity() {
    val pipController = DetailPipController()
    private val openTab = MutableStateFlow<MainTab?>(null)
    private val openTitle = MutableStateFlow<TitleOpenRequest?>(null)
    private val openShelf = MutableStateFlow<CollectionShelf?>(null)
    private val openActor = MutableStateFlow<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (supportsPip()) {
            setPictureInPictureParams(buildDetailPipParams(autoEnter = false))
        }
        applyIntent(intent)
        setContent {
            val initialTab by openTab.collectAsStateWithLifecycle()
            val titleRequest by openTitle.collectAsStateWithLifecycle()
            val shelfRequest by openShelf.collectAsStateWithLifecycle()
            val actorRequest by openActor.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalDetailPip provides pipController) {
                YohoTheme(darkTheme = true) {
                    MainShell(
                        initialTab = initialTab,
                        openTabRequest = openTab.value, 
                        onOpenTabConsumed = { openTab.value = null },
                        openTitleRequest = titleRequest,
                        onOpenTitleConsumed = { openTitle.value = null },
                        openShelfRequest = shelfRequest,
                        onOpenShelfConsumed = { openShelf.value = null },
                        openActorRequest = actorRequest,
                        onOpenActorConsumed = { openActor.value = null },
                    )
                }
            }
        }
    }

    private fun applyIntent(intent: Intent?) {
        openTab.value = tabFromIntent(intent)
        openTitle.value = titleFromIntent(intent)
        openShelf.value = shelfFromIntent(intent)
        val actor = intent?.getStringExtra(EXTRA_OPEN_ACTOR)
        val actorKp = intent?.getStringExtra(EXTRA_OPEN_ACTOR_KP)
        if (!actor.isNullOrBlank() && !actorKp.isNullOrBlank()) {
            openActor.value = actor to actorKp
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val TAB_ACCOUNT = "account"
        const val TAB_FAVORITES = "favorites"
        const val TAB_CATALOG = "catalog"
        const val TAB_HOME = "home"
        const val EXTRA_OPEN_TITLE_KP = "extra_open_title_kp"
        const val EXTRA_OPEN_TITLE_TMDB = "extra_open_title_tmdb"
        const val EXTRA_OPEN_TITLE_MEDIA = "extra_open_title_media"
        const val EXTRA_OPEN_SHELF = "extra_open_shelf"
        const val EXTRA_OPEN_ACTOR = "extra_open_actor"
        const val EXTRA_OPEN_ACTOR_KP = "extra_open_actor_kp"

        fun intentOpenAccount(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_ACCOUNT)
            }

        /** Деталка внутри Main (с нижней навигацией), не TitleDetailActivity. */
        fun intentOpenTitle(
            context: Context,
            idKp: Int?,
            idTmdb: Int? = null,
            mediaType: String? = null,
            tab: String = TAB_FAVORITES,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_TAB, tab)
                idKp?.takeIf { it > 0 }?.let { putExtra(EXTRA_OPEN_TITLE_KP, it) }
                idTmdb?.takeIf { it > 0 }?.let { putExtra(EXTRA_OPEN_TITLE_TMDB, it) }
                mediaType?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_OPEN_TITLE_MEDIA, it) }
            }

        fun tabFromIntent(intent: Intent?): MainTab? =
            when (intent?.getStringExtra(EXTRA_OPEN_TAB)) {
                TAB_ACCOUNT -> MainTab.Account
                TAB_FAVORITES -> MainTab.Favorites
                TAB_CATALOG -> MainTab.Catalog
                else -> null
            }

        fun titleFromIntent(intent: Intent?): TitleOpenRequest? {
            if (intent == null) return null
            val kp = intent.getIntExtra(EXTRA_OPEN_TITLE_KP, -1).takeIf { it > 0 }
            val tmdb = intent.getIntExtra(EXTRA_OPEN_TITLE_TMDB, -1).takeIf { it > 0 }
            if (kp == null && tmdb == null) return null
            return TitleOpenRequest(
                idKp = kp,
                idTmdb = tmdb,
                mediaType = intent.getStringExtra(EXTRA_OPEN_TITLE_MEDIA),
            )
        }

        fun intentOpenShelf(context: Context, shelf: CollectionShelf): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_FAVORITES)
                putExtra(EXTRA_OPEN_SHELF, shelf.name)
            }

        fun intentOpenCatalogWithActor(
            context: Context,
            actor: String,
            actorKp: String,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_CATALOG)
                putExtra(EXTRA_OPEN_ACTOR, actor)
                putExtra(EXTRA_OPEN_ACTOR_KP, actorKp)
            }

        fun shelfFromIntent(intent: Intent?): CollectionShelf? {
            val raw = intent?.getStringExtra(EXTRA_OPEN_SHELF) ?: return null
            return runCatching { CollectionShelf.valueOf(raw) }.getOrNull()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        onDetailPipUserLeave(pipController)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        onDetailPipModeChanged(pipController, isInPictureInPictureMode)
    }
}
