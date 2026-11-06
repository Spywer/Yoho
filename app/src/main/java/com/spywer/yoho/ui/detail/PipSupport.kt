package com.spywer.yoho.ui.detail

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.view.Choreographer
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.spywer.yoho.data.AppSettings

/** PiP для кино-плеера: MainActivity / TitleDetailActivity. */
class DetailPipController {
    @Volatile
    var eligible: Boolean = false

    @Volatile
    var playerWebView: WebView? = null

    private var appliedAutoEnter: Boolean? = null

    val inPipState = mutableStateOf(false)

    fun setInPip(value: Boolean) {
        inPipState.value = value
    }

    fun setEligible(activity: Activity?, value: Boolean) {
        val changed = eligible != value
        eligible = value
        if (activity == null || !activity.supportsPip() || activity.isInPictureInPictureMode) {
            return
        }
        val autoEnter = value && AppSettings.pipEnabled(activity)
        if (!changed && appliedAutoEnter == autoEnter) return
        runCatching {
            activity.setPictureInPictureParams(buildDetailPipParams(autoEnter = autoEnter))
            appliedAutoEnter = autoEnter
        }
    }

    fun keepPlayingInPip() {
        val wv = playerWebView ?: return
        runCatching {
            wv.onResume()
            wv.resumeTimers()
        }
    }

    /** Уход с деталки / destroy holder — не держать WebView на Activity. */
    fun clearPlayer(activity: Activity?) {
        playerWebView = null
        if (!inPipState.value) {
            setEligible(activity, false)
        }
    }
}

val LocalDetailPip = staticCompositionLocalOf<DetailPipController?> { null }

fun Activity.supportsPip(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

fun buildDetailPipParams(autoEnter: Boolean = false): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
    }
    return builder.build()
}

fun Activity.enterDetailPipIfPossible(eligible: Boolean): Boolean {
    if (!eligible ||
        !AppSettings.pipEnabled(this) ||
        !supportsPip() ||
        isInPictureInPictureMode ||
        isFinishing
    ) {
        return false
    }
    return runCatching {
        enterPictureInPictureMode(buildDetailPipParams(autoEnter = false))
    }.getOrDefault(false)
}

fun Activity.onDetailPipUserLeave(controller: DetailPipController) {
    if (!controller.eligible ||
        !AppSettings.pipEnabled(this) ||
        !supportsPip() ||
        isInPictureInPictureMode ||
        isFinishing
    ) {
        return
    }
    // Сначала убрать chrome (Compose pipMode), кадр спустя — enter PiP,
    // иначе в мини-окне мелькает вся страница деталки.
    controller.setInPip(true)
    Choreographer.getInstance().postFrameCallback {
        if (isFinishing) {
            controller.setInPip(false)
            return@postFrameCallback
        }
        if (!enterDetailPipIfPossible(true)) {
            controller.setInPip(false)
        }
    }
}

fun Activity.onDetailPipModeChanged(
    controller: DetailPipController,
    isInPictureInPictureMode: Boolean,
) {
    controller.setInPip(isInPictureInPictureMode)
    if (isInPictureInPictureMode) {
        controller.keepPlayingInPip()
    } else if (supportsPip()) {
        setPictureInPictureParams(
            buildDetailPipParams(
                autoEnter = controller.eligible && AppSettings.pipEnabled(this),
            ),
        )
    }
}

@Composable
fun DetailPipEligibilityEffect(eligible: Boolean) {
    val controller = LocalDetailPip.current ?: return
    val activity = LocalContext.current.findActivity()
    DisposableEffect(eligible, activity) {
        controller.setEligible(activity, eligible)
        onDispose {
            if (controller.inPipState.value || activity?.isInPictureInPictureMode == true) {
                return@onDispose
            }
            if (controller.eligible == eligible) {
                controller.setEligible(activity, false)
            }
        }
    }
}

@Composable
fun DetailPipWebViewBindEffect(webView: WebView?) {
    val controller = LocalDetailPip.current ?: return
    DisposableEffect(webView) {
        if (webView != null) controller.playerWebView = webView
        onDispose {
            // Не обнулять при смене layout в PiP — PlayerBlock уходит, WebView жив.
            if (controller.inPipState.value) return@onDispose
            if (controller.playerWebView === webView) {
                controller.playerWebView = null
            }
        }
    }
}
