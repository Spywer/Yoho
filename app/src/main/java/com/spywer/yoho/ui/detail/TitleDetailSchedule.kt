package com.spywer.yoho.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.TvEpisodeDto
import com.spywer.yoho.data.remote.TvLastAirDto
import com.spywer.yoho.data.remote.TvScheduleDto
import com.spywer.yoho.data.remote.dubsFor
import com.spywer.yoho.data.remote.formatDubsSummary

@Composable
private fun NextEpisodeBlock(
    lastAir: TvLastAirDto,
    schedule: TvScheduleDto?,
    padding: androidx.compose.ui.unit.Dp,
) {
    val meta = lastAir.meta ?: return
    val lastSeason = meta.seasonNumber
    val lastEpisode = meta.episodeNumber
    val nextSeason = meta.nextSeasonNumber
    val nextEpisode = meta.nextEpisodeNumber
    val nextTitle = schedule
        ?.seasons
        ?.firstOrNull { it.seasonNumber == nextSeason }
        ?.episodes
        ?.firstOrNull { it.episodeNumber == nextEpisode }
        ?.title
    val nextAirLabel = formatAirDateTimeMsk(
        airDate = meta.nextAirDate,
        airAtMs = meta.nextAirAt,
        precision = meta.nextAirPrecision,
        withMsk = true,
    )?.let { "Выходит $it" } ?: meta.nextAirDate
    val daysLeft = daysUntilAir(meta.nextAirDate)
        ?: meta.nextAirAt?.takeIf { it > 0L }?.let { at ->
            java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(),
                java.time.Instant.ofEpochMilli(at)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate(),
            )
        }
    val episodeDubs = schedule?.dubsFor(lastSeason, lastEpisode).orEmpty()
    val firstDubAt = episodeDubs.mapNotNull { it.at }.minOrNull()
    val airedDate = when {
        firstDubAt != null -> formatAirDateTimeMsk(
            airDate = null,
            airAtMs = firstDubAt,
            precision = null,
            withMsk = false,
        )
        else -> formatAirDateTimeMsk(
            airDate = meta.airDate,
            airAtMs = meta.airAt,
            precision = meta.airPrecision,
            withMsk = false,
        )
    }
    val dubs = episodeDubs.formatDubsSummary()
    val seasonEps = schedule
        ?.seasons
        ?.firstOrNull { it.seasonNumber == (nextSeason ?: lastSeason) }
        ?.episodes
        .orEmpty()
    val airedCount = seasonEps.count { ep ->
        val d = daysUntilAir(ep.airDate)
        d != null && d <= 0
    }.coerceAtLeast(lastEpisode ?: 0)
    val totalCount = seasonEps.size.coerceAtLeast(airedCount)
    val seasonEnd = seasonEps
        .maxByOrNull { it.episodeNumber ?: 0 }
        ?.let { formatAirDateRu(it.airDate) }
    val progress = if (totalCount > 0) airedCount.toFloat() / totalCount.toFloat() else 0f

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = padding)
            .padding(top = 20.dp, bottom = 12.dp),
    ) {
        if (nextEpisode != null && nextSeason != null && meta.inProduction != false) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = nextEpisode.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.detail_episode_badge_label).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.detail_next_episode).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    nextTitle?.takeIf { it.isNotBlank() }?.let { title ->
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    nextAirLabel?.let { label ->
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                daysLeft?.takeIf { it > 0 }?.let { days ->
                    Text(
                        text = formatDaysLeft(days),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        if (totalCount > 0 && airedCount > 0) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = buildString {
                        append(
                            stringResource(
                                R.string.detail_season_progress,
                                airedCount,
                                totalCount,
                            ),
                        )
                        seasonEnd?.let {
                            append(stringResource(R.string.detail_season_ends, it))
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (lastEpisode != null && airedDate != null) {
            Text(
                text = buildString {
                    append(
                        stringResource(
                            R.string.detail_episode_aired_line,
                            lastEpisode,
                            airedDate,
                        ),
                    )
                    if (!dubs.isNullOrBlank()) {
                        append(stringResource(R.string.detail_episode_aired_dubs, dubs))
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 22.sp,
            )
        } else if (!lastAir.badge.isNullOrBlank()) {
            Text(
                text = lastAir.badge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 22.sp,
            )
        }
    }
}

private fun formatDaysLeft(days: Long): String = when {
    days % 10 == 1L && days % 100 != 11L -> "$days день"
    days % 10 in 2..4 && days % 100 !in 12..14 -> "$days дня"
    else -> "$days дн."
}

internal fun LazyListScope.titleDetailScheduleSection(
    schedule: TvScheduleDto?,
    scheduleLoading: Boolean,
    selectedSeason: Int?,
    selectedSeasonEpisodes: List<TvEpisodeDto>,
    lastAir: TvLastAirDto?,
    screenPadding: Dp,
    onSelectSeason: (Int) -> Unit,
) {
    if (lastAir?.meta != null) {
        item {
            NextEpisodeBlock(
                lastAir = lastAir,
                schedule = schedule,
                padding = screenPadding,
            )
        }
    }

    if (scheduleLoading) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(screenPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                )
            }
        }
    }

    if (schedule != null && schedule.seasons.isNotEmpty()) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screenPadding)
                    .padding(top = 20.dp),
            ) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    schedule.seasons.forEach { season ->
                        val num = season.seasonNumber ?: return@forEach
                        FilterChip(
                            selected = selectedSeason == num,
                            onClick = { onSelectSeason(num) },
                            modifier = Modifier.height(56.dp),
                            label = {
                                Text(
                                    text = stringResource(R.string.detail_season_n, num),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                schedule.seasons
                    .firstOrNull { it.seasonNumber == selectedSeason }
                    ?.let { season ->
                        val count = season.episodes.size
                        val period = formatSeasonPeriod(season) ?: return@let
                        if (count <= 0) return@let
                        Text(
                            text = stringResource(
                                R.string.detail_season_meta,
                                count,
                                period,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
            }
        }
        items(
            items = selectedSeasonEpisodes,
            key = { "ep-${it.seasonNumber}-${it.episodeNumber}" },
        ) { ep ->
            val daysUntil = daysUntilAir(ep.airDate)
            val aired = daysUntil == null || daysUntil <= 0
            val statusColor = if (aired) {
                Color(0xFF43A047)
            } else {
                Color(0xFFFFA000)
            }
            val episodeDubs = schedule.dubsFor(
                ep.seasonNumber ?: selectedSeason,
                ep.episodeNumber,
            )
            val dubsText = episodeDubs.formatDubsSummary()
            // Как на сайте: вышедшие — время первой озвучки; ожидаемые — ≈слот nextAirAt.
            val firstDubAt = episodeDubs.mapNotNull { it.at }.minOrNull()
            val lastMeta = lastAir?.meta
            val dateLabel = when {
                firstDubAt != null -> formatAirDateTimeMsk(
                    airDate = null,
                    airAtMs = firstDubAt,
                    precision = null,
                    withMsk = false,
                )
                !aired -> formatAirDateTimeMsk(
                    airDate = ep.airDate,
                    airAtMs = when {
                        ep.seasonNumber == lastMeta?.nextSeasonNumber &&
                            ep.episodeNumber == lastMeta?.nextEpisodeNumber ->
                            lastMeta?.nextAirAt
                        else -> estimateAirAtMs(
                            ep.airDate,
                            lastMeta?.nextAirAt ?: lastMeta?.airAt,
                        )
                    },
                    precision = lastMeta?.nextAirPrecision ?: "estimated",
                    withMsk = false,
                )
                else -> formatAirDateRu(ep.airDate)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screenPadding)
                    .padding(top = 5.dp, bottom = 5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (aired) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color(0xFFFFA000).copy(alpha = 0.12f)
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = statusColor.copy(alpha = if (aired) 0.25f else 0.45f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.detail_episode_n,
                        ep.episodeNumber ?: 0,
                    ),
                    color = if (aired) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        statusColor
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(120.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = ep.title?.takeIf { it.isNotBlank() }
                            ?: stringResource(
                                R.string.detail_episode_n,
                                ep.episodeNumber ?: 0,
                            ),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    dubsText?.let { dubs ->
                        Text(
                            text = stringResource(R.string.detail_dubs, dubs),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Column(
                    Modifier.wrapContentWidth(Alignment.End),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = if (daysUntil != null && daysUntil > 0) {
                            stringResource(
                                R.string.detail_episode_upcoming_in,
                                daysUntil.toInt(),
                            )
                        } else {
                            stringResource(R.string.detail_episode_aired)
                        },
                        color = statusColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                    )
                    dateLabel?.let { date ->
                        Text(
                            text = date,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 20.sp,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}


