package com.spywer.yoho

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.spywer.yoho.ui.detail.DetailPipController
import com.spywer.yoho.ui.detail.LocalDetailPip
import com.spywer.yoho.ui.detail.buildDetailPipParams
import com.spywer.yoho.ui.detail.onDetailPipModeChanged
import com.spywer.yoho.ui.detail.onDetailPipUserLeave
import com.spywer.yoho.ui.detail.supportsPip
import com.spywer.yoho.ui.main.MainShell
import com.spywer.yoho.ui.main.MainTab
import com.spywer.yoho.ui.theme.YohoTheme

/** Хост табов + in-app деталка; PiP при загруженном плеере (Home). */
class MainActivity : ComponentActivity() {
    val pipController = DetailPipController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (supportsPip()) {
            setPictureInPictureParams(buildDetailPipParams(autoEnter = false))
        }
        val initialTab = tabFromIntent(intent)
        setContent {
            CompositionLocalProvider(LocalDetailPip provides pipController) {
                YohoTheme(darkTheme = true) {
                    MainShell(initialTab = initialTab)
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val TAB_ACCOUNT = "account"

        fun intentOpenAccount(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_ACCOUNT)
            }

        fun tabFromIntent(intent: Intent?): MainTab? =
            when (intent?.getStringExtra(EXTRA_OPEN_TAB)) {
                TAB_ACCOUNT -> MainTab.Account
                else -> null
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Пересоздание content при возврате на аккаунт из полок коллекции
        if (tabFromIntent(intent) != null) {
            recreate()
        }
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
