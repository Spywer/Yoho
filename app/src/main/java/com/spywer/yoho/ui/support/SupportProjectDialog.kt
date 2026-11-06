package com.spywer.yoho.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spywer.yoho.R
import com.spywer.yoho.ui.layout.rememberHomeMetrics

/** Диалог «Поддержать разработку»: QR + ссылка CloudTips (Compose, тема YoHo). */
@Composable
fun SupportProjectDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val url = stringResource(R.string.support_project_url)
    val metrics = rememberHomeMetrics()
    val titleSize = if (metrics.isTablet) 26.sp else (metrics.sectionTitleSize.value + 2f).sp
    val bodySize = if (metrics.isTablet) 22.sp else metrics.detailBodySize
    val linkSize = if (metrics.isTablet) 24.sp else (metrics.detailBodySize.value + 3f).sp
    val buttonSize = if (metrics.isTablet) 20.sp else (metrics.searchTextSize.value + 3f).sp
    val buttonHeight = if (metrics.isTablet) 52.dp else metrics.searchHeight
    val qrOuter = if (metrics.isTablet) 232.dp else 196.dp
    val qrInner = if (metrics.isTablet) 208.dp else 172.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(if (metrics.isTablet) 0.72f else 0.92f)
                .widthIn(max = if (metrics.isTablet) 480.dp else 400.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = metrics.screenPadding + 4.dp,
                    vertical = metrics.sectionGap * 2 + 6.dp,
                )
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.support_project_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(metrics.sectionGap + 4.dp))
            Text(
                text = stringResource(R.string.support_project_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = bodySize,
                lineHeight = metrics.lineOf(bodySize),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(metrics.sectionGap * 2))
            Text(
                text = stringResource(R.string.support_project_scan_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = bodySize,
                lineHeight = metrics.lineOf(bodySize),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(metrics.sectionGap + 6.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(qrOuter)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.qr_donate_cloudtips),
                    contentDescription = stringResource(R.string.support_qr_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(qrInner),
                )
            }
            Spacer(Modifier.height(metrics.sectionGap + 6.dp))
            Text(
                text = url,
                color = MaterialTheme.colorScheme.primary,
                fontSize = linkSize,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                    .padding(vertical = 4.dp),
            )
            Spacer(Modifier.height(metrics.sectionGap * 2))
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
            ) {
                Text(
                    stringResource(R.string.support_open_payment),
                    fontSize = buttonSize,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(metrics.sectionGap))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
            ) {
                Text(
                    stringResource(R.string.support_close),
                    fontSize = buttonSize,
                )
            }
        }
    }
}
