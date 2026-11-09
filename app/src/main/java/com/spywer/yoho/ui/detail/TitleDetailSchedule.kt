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
import com.spywer.yoho.R
import com.spywer.yoho.data.remote.TvEpisodeDto
import com.spywer.yoho.data.remote.TvLastAirDto
import com.spywer.yoho.data.remote.TvScheduleDto
import com.spywer.yoho.data.remote.dubsFor
import com.spywer.yoho.data.remote.formatDubsSummary
import com.spywer.yoho.ui.layout.LocalHomeMetrics

@Composable
private fun NextEpisodeBlock(
    lastAir: TvLastAirDto,
    schedule: TvScheduleDto?,
    padding: androidx.compose.ui.unit.Dp,
) {
    val metrics = LocalHomeMetrics.current
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
        ?.let { formatLongDateRu(it.airDate) }
    val progress = if (totalCount > 0) airedCount.toFloat() / totalCount.toFloat() else 0f

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = padding)
            .padding(
                top = metrics.sectionGap,
                bottom = metrics.blockGap,
            ),
    ) {
        if (nextEpisode != null && nextSeason != null && meta.inProduction != false) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(if (metrics.isTablet) 16.dp else metrics.sectionGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(
                            horizontal = if (metrics.isTablet) 14.dp else 10.dp,
                            vertical = if (metrics.isTablet) 12.dp else 8.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = nextEpisode.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = metrics.detailTitleSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.detail_episode_badge_label).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = if (metrics.isTablet) {
                                metrics.scheduleBodySize
                            } else {
                                metrics.castRoleSize
                            },
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.width(if (metrics.isTablet) 14.dp else 10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.detail_next_episode).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = metrics.scheduleBodySize,
                        fontWeight = FontWeight.Bold,
                    )
                    nextTitle?.takeIf { it.isNotBlank() }?.let { title ->
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = metrics.scheduleTitleSize,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    nextAirLabel?.let { label ->
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = metrics.scheduleBodySize,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                daysLeft?.takeIf { it > 0 }?.let { days ->
                    Text(
                        text = formatDaysLeft(days),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = metrics.detailTitleSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(metrics.sectionGap))
        }

        if (totalCount > 0 && airedCount > 0) {
            val seasonComplete = airedCount >= totalCount
            Column(Modifier.fillMaxWidth()) {
                if (seasonComplete) {
                    Text(
                        text = seasonEnd?.let {
                            stringResource(R.string.detail_season_finished_on, it)
                        } ?: stringResource(R.string.detail_season_finished),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = metrics.scheduleBodySize,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
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
                        fontSize = metrics.scheduleBodySize,
                    )
                }
                Spacer(Modifier.height(metrics.blockGap))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                if (seasonComplete) {
                    Text(
                        text = stringResource(
                            R.string.detail_season_progress_complete,
                            airedCount,
                            totalCount,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = metrics.scheduleBodySize,
                        modifier = Modifier.padding(top = metrics.blockGap),
                    )
                }
            }
            Spacer(Modifier.height(metrics.sectionGap))
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
                fontSize = metrics.scheduleBodySize,
            )
        } else if (!lastAir.badge.isNullOrBlank()) {
            Text(
                text = lastAir.badge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = metrics.scheduleBodySize,
            )
        }
    }
}

private fun formatDaysLeft(days: Long): String = when {
    days % 10 == 1L && days % 100 != 11L -> "$days день"
    days % 10 in 2..4 && days % 100 !in 12..14 -> "$days дня"
    else -> "$days дн."
}

@Composable
internal fun TitleDetailScheduleSection(
    schedule: TvScheduleDto?,
    scheduleLoading: Boolean,
    selectedSeason: Int?,
    selectedSeasonEpisodes: List<TvEpisodeDto>,
    lastAir: TvLastAirDto?,
    screenPadding: Dp,
    onSelectSeason: (Int) -> Unit,
) {
    if (lastAir?.meta != null) {
        NextEpisodeBlock(
            lastAir = lastAir,
            schedule = schedule,
            padding = screenPadding,
        )
    }

    if (scheduleLoading) {
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

    if (schedule != null && schedule.seasons.isNotEmpty()) {
            val metrics = LocalHomeMetrics.current
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screenPadding)
                    .padding(top = metrics.sectionGap, bottom = metrics.sectionGap),
            ) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
                ) {
                    schedule.seasons.forEach { season ->
                        val num = season.seasonNumber ?: return@forEach
                        FilterChip(
                            selected = selectedSeason == num,
                            onClick = { onSelectSeason(num) },
                            modifier = Modifier.height(
                                if (metrics.isTablet) 56.dp else metrics.playerChipHeight,
                            ),
                            label = {
                                Text(
                                    text = stringResource(R.string.detail_season_n, num),
                                    fontSize = if (metrics.isTablet) {
                                        metrics.scheduleEpSize
                                    } else {
                                        metrics.chipTextSize
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        vertical = if (metrics.isTablet) 8.dp else 4.dp,
                                    ),
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
                            fontSize = metrics.scheduleBodySize,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = metrics.sectionGap),
                        )
                    }
            }
        selectedSeasonEpisodes.forEach { ep ->
            val metrics = LocalHomeMetrics.current
            val daysUntil = daysUntilAir(ep.airDate)
            val unknownDate = ep.airDate.isNullOrBlank()
            val aired = !unknownDate && (daysUntil == null || daysUntil <= 0)
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
                unknownDate -> "—"
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
                    .padding(top = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(
                        horizontal = if (metrics.isTablet) 12.dp else 10.dp,
                        vertical = if (metrics.isTablet) 10.dp else 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(
                    if (metrics.isTablet) 12.dp else 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (metrics.isTablet) {
                        stringResource(
                            R.string.detail_episode_n,
                            ep.episodeNumber ?: 0,
                        )
                    } else {
                        (ep.episodeNumber ?: 0).toString()
                    },
                    color = if (aired) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        statusColor
                    },
                    fontSize = metrics.scheduleEpSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = if (metrics.isTablet) TextAlign.Start else TextAlign.Center,
                    modifier = Modifier.width(if (metrics.isTablet) 120.dp else 28.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = ep.title?.takeIf { it.isNotBlank() }
                            ?: stringResource(
                                R.string.detail_episode_n,
                                ep.episodeNumber ?: 0,
                            ),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = metrics.scheduleEpSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    dubsText?.let { dubs ->
                        Text(
                            text = stringResource(R.string.detail_dubs, dubs),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = metrics.cardMetaSize,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
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
                        } else if (unknownDate) {
                            stringResource(R.string.detail_episode_unknown_date)
                        } else {
                            stringResource(R.string.detail_episode_aired)
                        },
                        color = statusColor,
                        fontSize = metrics.scheduleBodySize,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                    )
                    dateLabel?.let { date ->
                        Text(
                            text = date,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = metrics.cardMetaSize,
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


