package com.spywer.yoho.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.PersonDto
import com.spywer.yoho.data.remote.absMediaUrl
import com.spywer.yoho.ui.layout.LocalHomeMetrics

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonSheet(
    personId: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val metrics = LocalHomeMetrics.current
    var loading by remember(personId) { mutableStateOf(true) }
    var error by remember(personId) { mutableStateOf<String?>(null) }
    var person by remember(personId) { mutableStateOf<PersonDto?>(null) }

    LaunchedEffect(personId) {
        loading = true
        error = null
        runCatching {
            val raw = NetworkModule.api.person(personId)
            raw.copy(photoUrl = raw.photoUrl.absMediaUrl())
        }.onSuccess {
            person = it
            loading = false
        }.onFailure { e ->
            error = e.message ?: "Не удалось загрузить"
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        when {
            loading -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            error != null && person == null -> {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = metrics.cardMetaSize,
                    modifier = Modifier.padding(metrics.screenPadding),
                )
            }

            person != null -> {
                val p = person!!
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = metrics.screenPadding)
                        .padding(bottom = metrics.sectionGap),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                    ) {
                        Box(
                            Modifier
                                .width(if (metrics.isTablet) 110.dp else 88.dp)
                                .height(if (metrics.isTablet) 146.dp else 116.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (!p.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = p.photoUrl,
                                    contentDescription = p.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(metrics.blockGap),
                        ) {
                            Text(
                                text = p.name.orEmpty(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = metrics.sectionTitleSize,
                                lineHeight = metrics.lineOf(metrics.sectionTitleSize),
                                fontWeight = FontWeight.Bold,
                            )
                            p.profession?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = metrics.cardMetaSize,
                                    lineHeight = metrics.lineOf(metrics.cardMetaSize),
                                )
                            }
                            p.birthInfo?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = metrics.cardMetaSize,
                                    lineHeight = metrics.lineOf(metrics.cardMetaSize),
                                )
                            }
                            p.birthplace?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = "${stringResource(R.string.person_birthplace)}: $it",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = metrics.cardMetaSize,
                                    lineHeight = metrics.lineOf(metrics.cardMetaSize),
                                )
                            }
                        }
                    }

                    if (p.knownFor.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.person_known_for),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = metrics.searchTextSize,
                            lineHeight = metrics.lineOf(metrics.searchTextSize),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                top = metrics.sectionGap,
                                bottom = metrics.blockGap,
                            ),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(metrics.blockGap),
                            verticalArrangement = Arrangement.spacedBy(metrics.blockGap),
                        ) {
                            p.knownFor.forEach { title ->
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = metrics.cardMetaSize,
                                    lineHeight = metrics.lineOf(metrics.cardMetaSize),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(
                                            horizontal = 10.dp,
                                            vertical = if (metrics.isTablet) 6.dp else 4.dp,
                                        ),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(metrics.blockGap))
                }
            }
        }
    }
}
