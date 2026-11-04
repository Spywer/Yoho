package com.spywer.yoho.ui.detail

import com.spywer.yoho.data.remote.TvSeasonDto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val RuMonthsGenitive = listOf(
    "", "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private val RuMonthsNominative = listOf(
    "", "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
)

private val MskZone: ZoneId = ZoneId.of("Europe/Moscow")

/** «март 2025 г. – июнь 2025 г.» по min/max airDate эпизодов. */
internal fun formatSeasonPeriod(season: TvSeasonDto): String? {
    val dates = season.episodes.mapNotNull { parseAirLocalDate(it.airDate) }.sorted()
    if (dates.isEmpty()) return null
    val start = dates.first()
    val end = dates.last()
    fun monthYear(d: LocalDate): String =
        "${RuMonthsNominative[d.monthValue]} ${d.year} г."
    return if (start.year == end.year && start.month == end.month) {
        monthYear(start)
    } else {
        "${monthYear(start)} – ${monthYear(end)}"
    }
}

/** Дней до airDate: null — нет даты; ≤0 — уже вышла / сегодня. */
internal fun daysUntilAir(airDate: String?): Long? {
    val date = parseAirLocalDate(airDate) ?: return null
    return ChronoUnit.DAYS.between(LocalDate.now(), date)
}

/** Время суток из templateAirAtMs, навешанное на airDate (МСК). */
internal fun estimateAirAtMs(airDate: String?, templateAirAtMs: Long?): Long? {
    val date = parseAirLocalDate(airDate) ?: return null
    val template = templateAirAtMs?.takeIf { it > 0L } ?: return null
    val tod = Instant.ofEpochMilli(template).atZone(MskZone).toLocalTime()
    return date.atTime(tod).atZone(MskZone).toInstant().toEpochMilli()
}

/** «6 ноября ≈11:00 МСК» / «29 октября 18:04» / «6 ноября». */
internal fun formatAirDateTimeMsk(
    airDate: String?,
    airAtMs: Long?,
    precision: String? = null,
    withMsk: Boolean = true,
): String? {
    val estimated = precision.equals("estimated", ignoreCase = true)
    if (airAtMs != null && airAtMs > 0L) {
        val zdt = Instant.ofEpochMilli(airAtMs).atZone(MskZone)
        val nowYear = LocalDate.now(MskZone).year
        val dayMonth = buildString {
            append(zdt.dayOfMonth)
            append(' ')
            append(RuMonthsGenitive[zdt.monthValue])
            if (zdt.year != nowYear) {
                append(' ')
                append(zdt.year)
            }
        }
        val approx = if (estimated) "≈" else ""
        val time = "%s%02d:%02d".format(approx, zdt.hour, zdt.minute)
        return if (withMsk) "$dayMonth в $time МСК" else "$dayMonth $time"
    }
    return formatAirDateRu(airDate)
}

/** «8 октября» / «8 октября 19:02»; год — если не текущий. */
internal fun formatAirDateRu(
    airDate: String?,
    fallbackEpochMs: Long? = null,
): String? {
    val nowYear = LocalDate.now().year
    if (fallbackEpochMs != null && fallbackEpochMs > 0) {
        val zdt = Instant.ofEpochMilli(fallbackEpochMs)
            .atZone(ZoneId.systemDefault())
        val dayMonth = buildString {
            append(zdt.dayOfMonth)
            append(' ')
            append(RuMonthsGenitive[zdt.monthValue])
            if (zdt.year != nowYear) {
                append(' ')
                append(zdt.year)
            }
        }
        return "%s %02d:%02d".format(dayMonth, zdt.hour, zdt.minute)
    }
    val fromString = parseAirLocalDate(airDate) ?: return null
    val dayMonth = buildString {
        append(fromString.dayOfMonth)
        append(' ')
        append(RuMonthsGenitive[fromString.monthValue])
        if (fromString.year != nowYear) {
            append(' ')
            append(fromString.year)
        }
    }
    val timeFromString = parseAirLocalTime(airDate) ?: return dayMonth
    return "%s %02d:%02d".format(dayMonth, timeFromString.hour, timeFromString.minute)
}

private fun parseAirLocalDate(airDate: String?): LocalDate? {
    if (airDate.isNullOrBlank()) return null
    return try {
        val raw = airDate.trim()
        when {
            raw.length >= 10 && raw[4] == '-' -> {
                val p = raw.take(10).split('-')
                LocalDate.of(p[0].toInt(), p[1].toInt(), p[2].toInt())
            }
            else -> LocalDate.parse(raw.take(10))
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseAirLocalTime(airDate: String?): LocalTime? {
    if (airDate.isNullOrBlank()) return null
    return try {
        val raw = airDate.trim()
        val tIdx = raw.indexOf('T').takeIf { it >= 0 } ?: raw.indexOf(' ').takeIf { it >= 0 }
        if (tIdx == null || tIdx + 1 >= raw.length) return null
        val timePart = raw.substring(tIdx + 1).take(5)
        if (!timePart.contains(':')) return null
        val hm = timePart.split(':')
        val h = hm[0].toInt()
        val m = hm.getOrNull(1)?.toInt() ?: 0
        if (h == 0 && m == 0 && !raw.contains('T') && !raw.contains(' ')) return null
        LocalTime.of(h, m)
    } catch (_: Exception) {
        null
    }
}

