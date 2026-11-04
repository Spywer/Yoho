package com.spywer.yoho.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.ui.components.PosterCard
import com.spywer.yoho.ui.search.genreSlugOf
@Composable
internal fun AgeRatingBadge(age: String) {
    val label = age.trim().let { if (it.endsWith("+")) it else "$it+" }
    Text(
        text = label,
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun RatingBadge(
    label: String,
    value: Double,
    votes: Int?,
    color: Color,
    suffix: String = "",
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = if (suffix == "%") "${value.toInt()}$suffix" else "%.1f".format(value),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        votes?.let {
            Text(
                text = formatVotes(it),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
            )
        }
    }
}

private fun formatVotes(votes: Int): String = when {
    votes >= 1_000_000 -> "%.1f млн".format(votes / 1_000_000.0)
    votes >= 1_000 -> "%,d".format(votes).replace(',', ' ')
    else -> votes.toString()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MetaRow(
    label: String,
    value: String?,
    personIds: Map<String, Int> = emptyMap(),
    onOpenPerson: (Int) -> Unit = {},
    linkPartsAsGenres: Boolean = false,
    onOpenGenre: (String) -> Unit = {},
) {
    if (value.isNullOrBlank()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        val metaSize = 22.sp
        val metaLine = 32.sp
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = metaSize,
            lineHeight = metaLine,
            modifier = Modifier.width(260.dp),
        )
        if (personIds.isEmpty() && !linkPartsAsGenres) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = metaSize,
                lineHeight = metaLine,
                modifier = Modifier.weight(1f),
            )
        } else {
            val parts = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            FlowRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                parts.forEachIndexed { index, name ->
                    val personId = personIds[name]?.takeIf { it > 0 }
                    val asGenre = linkPartsAsGenres && genreSlugOf(name) != null
                    Text(
                        text = name,
                        color = if (personId != null || asGenre) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        fontSize = metaSize,
                        lineHeight = metaLine,
                        modifier = when {
                            personId != null -> Modifier.clickable { onOpenPerson(personId) }
                            asGenre -> Modifier.clickable { onOpenGenre(name) }
                            else -> Modifier
                        },
                    )
                    if (index < parts.lastIndex) {
                        Text(
                            text = ", ",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = metaSize,
                            lineHeight = metaLine,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String, size: androidx.compose.ui.unit.TextUnit, padding: androidx.compose.ui.unit.Dp) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = padding,
            end = padding,
            top = 24.dp,
            bottom = 12.dp,
        ),
    )
}

internal fun androidx.compose.foundation.lazy.LazyListScope.relatedSection(
    title: String,
    items: List<TitleDto>,
    onOpenTitle: (Int?, Int?, String?) -> Unit,
    padding: androidx.compose.ui.unit.Dp,
    titleSize: androidx.compose.ui.unit.TextUnit,
    cardWidth: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
) {
    if (items.isEmpty()) return
    item { SectionHeader(title, titleSize, padding) }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(items, key = { it.idKp ?: it.idTmdb ?: it.hashCode() }) { item ->
                PosterCard(
                    item = item,
                    modifier = Modifier.width(cardWidth),
                    onClick = {
                        if (item.idKp == null && item.idTmdb == null) return@PosterCard
                        onOpenTitle(item.idKp, item.idTmdb, item.mediaType)
                    },
                )
            }
        }
    }
}

