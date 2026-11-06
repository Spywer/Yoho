package com.spywer.yoho.ui.legal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.spywer.yoho.ui.components.YohoBackButton
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.theme.YohoTheme

class LegalDocumentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val doc = LegalDocument.fromExtra(intent.getStringExtra(EXTRA_DOC))
        setContent {
            YohoTheme(darkTheme = true) {
                BackHandler { finish() }
                LegalDocumentScreen(
                    document = doc,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_DOC = "extra_legal_doc"

        fun intent(context: Context, document: LegalDocument): Intent =
            Intent(context, LegalDocumentActivity::class.java)
                .putExtra(EXTRA_DOC, document.extraValue)
    }
}

@Composable
private fun LegalDocumentScreen(
    document: LegalDocument,
    onBack: () -> Unit,
) {
    val metrics = rememberHomeMetrics()
    val context = LocalContext.current
    val body = remember(document) {
        context.resources.openRawResource(document.bodyRes)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }
    val titleSize = (metrics.sectionTitleSize.value + 4f).sp
    val bodySize = (metrics.searchTextSize.value + 1f).sp

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = metrics.screenPadding),
    ) {
        Spacer(Modifier.height(metrics.sectionGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(document.titleRes),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = metrics.sectionGap),
            )
            YohoBackButton(onClick = onBack)
        }
        Spacer(Modifier.height(metrics.sectionGap))
        Text(
            text = body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = bodySize,
            lineHeight = metrics.lineOf(bodySize),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = metrics.sectionGap * 2),
        )
    }
}
