package com.spywer.yoho

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
        setContent {
            CompositionLocalProvider(LocalDetailPip provides pipController) {
                YohoTheme(darkTheme = true) {
                    MainShell()
                }
            }
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
