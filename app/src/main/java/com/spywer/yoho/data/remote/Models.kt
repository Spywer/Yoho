package com.spywer.yoho.data.remote

import com.google.gson.annotations.SerializedName
import com.spywer.yoho.BuildConfig
import kotlin.jvm.Transient

data class TrendingResponse(
    val items: List<TrendingItemDto> = emptyList(),
    val ratings: Map<String, TrendingRatingDto> = emptyMap(),
    val builtAt: Long? = null,
)

/** GET /api/anime — блоки как на reyoho.com/anime. */
data class AnimeHomeResponse(
    val trending: AnimeListPageDto? = null,
    val season: AnimeListPageDto? = null,
    val popular: AnimeListPageDto? = null,
    val top: List<AnimeItemDto> = emptyList(),
    val seasonTop: List<AnimeItemDto> = emptyList(),
    val seasonLabel: AnimeSeasonLabelDto? = null,
)

data class AnimeListResponse(
    val items: List<AnimeItemDto> = emptyList(),
    val nextPage: Int? = null,
)

data class AnimeListPageDto(
    val items: List<AnimeItemDto> = emptyList(),
    val nextPage: Int? = null,
)

data class AnimeSeasonLabelDto(
    val season: String? = null,
    val year: Int? = null,
)

data class AnimeItemDto(
    val anilistId: Int? = null,
    val idMal: Int? = null,
    val title: String? = null,
    val romaji: String? = null,
    val russian: String? = null,
    val ruTitle: String? = null,
    val year: Int? = null,
    val score: Double? = null,
    val coverImage: String? = null,
    val ruPosterUrl: String? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val mediaType: String? = null,
    val tmdbId: Int? = null,
    val kpId: Int? = null,
    val kpRating: Double? = null,
    val imdbRating: Double? = null,
    val ruOverview: String? = null,
)

data class TrendingItemDto(
    val id: Int? = null,
    val title: String? = null,
    val originalTitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: String? = null,
    val mediaType: String? = null,
    val kinopoiskFilmId: Int? = null,
)

data class TrendingRatingDto(
    val kp: Double? = null,
    val imdb: Double? = null,
    val kinopoiskFilmId: Int? = null,
)

data class LatestTrailersResponse(
    val items: List<TrailerItemDto> = emptyList(),
    val nextStart: Int? = null,
    val hasMore: Boolean? = null,
    val feedVersion: Int? = null,
)

data class TrailerItemDto(
    val mediaType: String? = null,
    val tmdbId: Int? = null,
    val title: String? = null,
    val releaseYear: Int? = null,
    val backdropUrl: String? = null,
    val youtubeKey: String? = null,
    val trailerTitle: String? = null,
    val publishedAt: String? = null,
)

data class RecentListResponse(
    val items: List<RecentItemDto> = emptyList(),
    val total: Int? = null,
)

data class RecentItemDto(
    val id: Int? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val year: Int? = null,
    val poster: String? = null,
)

data class TitleDto(
    @SerializedName("id_kp") val idKp: Int? = null,
    @SerializedName("id_tmdb") val idTmdb: Int? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("title") val title: TitleNameDto? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("year_start") val yearStart: Int? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("ratings") val ratings: RatingsDto? = null,
    @SerializedName("trailer") val trailer: TrailerDto? = null,
    /** Подпись для полки рекомендаций («По вашим жанрам» и т.п.). */
    val reason: String? = null,
)

/** GET /api/recommendations/latest */
data class RecommendationsLatestResponse(
    val items: List<RecommendationItemDto> = emptyList(),
    val cachedAt: String? = null,
    val profileHash: String? = null,
)

data class RecommendationItemDto(
    val title: String? = null,
    val year: String? = null,
    val mediaType: String? = null,
    val posterUrl: String? = null,
    val href: String? = null,
    val itemId: String? = null,
    val tmdbId: Int? = null,
    val rating: Double? = null,
    val reason: String? = null,
    val confidence: Double? = null,
    val sourceTitle: String? = null,
    val sourceTmdbId: Int? = null,
    val genreIds: List<Int> = emptyList(),
)

data class TitleNameDto(
    @SerializedName("ru") val ru: String? = null,
    @SerializedName("original") val original: String? = null,
)

data class RatingsDto(
    @SerializedName("kinopoisk") val kinopoisk: ScoreDto? = null,
    @SerializedName("imdb") val imdb: ScoreDto? = null,
)

data class ScoreDto(
    @SerializedName("score") val score: Double? = null,
    @SerializedName("votes") val votes: Int? = null,
)

/** Один ролик из trailer.clips (YouTube и/или VK). */
data class TrailerClipDto(
    val title: String? = null,
    val youtubeKey: String? = null,
    val vkEmbedUrl: String? = null,
    val durationSec: Int? = null,
) {
    val youtubeWatchUrl: String?
        get() = youtubeKey?.takeIf { it.isNotBlank() }?.let { "https://www.youtube.com/watch?v=$it" }

    val hasYoutube: Boolean get() = !youtubeKey.isNullOrBlank()
    val hasVk: Boolean get() = !vkEmbedUrl.isNullOrBlank()
    val isPlayable: Boolean get() = hasYoutube || hasVk
}

data class TrailerDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    val vkEmbedUrl: String? = null,
    val clips: List<TrailerClipDto> = emptyList(),
) {
    /** Клипы для плеера; если clips пуст — собираем из корневых youtube/vk/url. */
    fun playableClips(): List<TrailerClipDto> {
        val fromApi = clips.filter { it.isPlayable }
        if (fromApi.isNotEmpty()) return fromApi
        val single = TrailerClipDto(
            title = title,
            youtubeKey = youtubeKeyFromWatchUrl(url),
            vkEmbedUrl = vkEmbedUrl,
        )
        return if (single.isPlayable) listOf(single) else emptyList()
    }
}

private fun youtubeKeyFromWatchUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val m = Regex("""(?:v=|/embed/|youtu\.be/)([A-Za-z0-9_-]{6,})""").find(url)
    return m?.groupValues?.getOrNull(1)
}

data class CastMemberDto(
    val id: Int? = null,
    val name: String? = null,
    val role: String? = null,
    val poster: String? = null,
)

/** Ответ /api/person/{id} */
data class PersonDto(
    val id: Int? = null,
    val name: String? = null,
    val profession: String? = null,
    val photoUrl: String? = null,
    val birthInfo: String? = null,
    val birthplace: String? = null,
    val knownFor: List<String> = emptyList(),
)

/** POST /api/diary → 201 { entry }; like/dislike без statusLabel (409 = уже есть). */
data class DiaryRequestDto(
    val itemId: String,
    val href: String,
    val title: String,
    val year: String? = null,
    val mediaType: String,
    val sourceReaction: String = "manual",
    val statusLabel: String? = null,
    val updateStatusIfItemExists: Boolean? = null,
)

data class DiaryResponseDto(
    val entry: DiaryEntryDto? = null,
)

data class DiaryEntryDto(
    val id: String? = null,
    val itemId: String? = null,
    val href: String? = null,
    val title: String? = null,
    val year: String? = null,
    val mediaType: String? = null,
    val statusLabel: String? = null,
)

/** GET /api/diary/contains?itemId=kp:… → { inDiary, statusLabel } */
data class DiaryContainsResponseDto(
    val inDiary: Boolean? = null,
    val statusLabel: String? = null,
)

/** DELETE /api/diary?itemId=kp:… → { ok, removed } */
data class DiaryRemoveResponseDto(
    val ok: Boolean? = null,
    val removed: Boolean? = null,
)

/**
 * POST /api/me/activity — формы body зависят от op:
 * - addFavorite/removeFavorite/recordWatch: `item`
 * - setRating: `input`
 * - clearRating/removeFavorite(alt)/removeWatch: `itemId`
 */
data class MeActivityRequest(
    val op: String,
    val item: MeActivityItemDto? = null,
    val input: MeActivityRatingInputDto? = null,
    val itemId: String? = null,
)

data class MeActivityItemDto(
    val id: String,
    val href: String,
    val title: String,
    val year: String? = null,
    val posterUrl: String? = null,
    val mediaType: String? = null,
    val ts: Long = System.currentTimeMillis(),
)

/** input для op=setRating (лайк → value=10, дизлайк → isNotInterested). */
data class MeActivityRatingInputDto(
    val itemId: String,
    val href: String,
    val title: String,
    val mediaType: String,
    val value: Int? = null,
    val isNotInterested: Boolean = false,
)

/** Элемент полки: favorites / watchHistory из GET /api/sync */
data class FavoriteItemDto(
    val id: String? = null,
    @SerializedName("content_id") val contentId: Int? = null,
    @SerializedName("content_type") val contentType: String? = null,
    val href: String? = null,
    val title: String? = null,
    val year: String? = null,
    val posterUrl: String? = null,
    val mediaType: String? = null,
    val ts: Long? = null,
    val tmdbTvId: Int? = null,
)

/** GET /api/sync — избранное и история просмотров */
data class SyncResponseDto(
    val ok: Boolean = false,
    val favorites: List<FavoriteItemDto> = emptyList(),
    val watchHistory: List<FavoriteItemDto> = emptyList(),
)

/** GET /api/ratings — реакции like/dislike. */
data class UserRatingsResponse(
    val ratings: List<UserReactionDto> = emptyList(),
)

data class UserReactionDto(
    val id: String? = null,
    val href: String? = null,
    val title: String? = null,
    val year: String? = null,
    val posterUrl: String? = null,
    val mediaType: String? = null,
    val reaction: String? = null,
    val aliases: List<String> = emptyList(),
    val ts: Long? = null,
)

/** POST /api/auth/sign-in/email */
data class SignInEmailRequest(
    val email: String,
    val password: String,
    val callbackURL: String = "/",
)

/** POST /api/auth/sign-up/email */
data class SignUpEmailRequest(
    val email: String,
    val password: String,
    val username: String,
    val name: String,
    val callbackURL: String = "/",
)

/** GET /api/auth/get-session (Better Auth) / sign-in */
data class AuthSessionResponse(
    val user: AuthUserDto? = null,
    val session: AuthSessionDto? = null,
    val redirectTo: String? = null,
)

data class AuthUserDto(
    val id: String? = null,
    val username: String? = null,
    @SerializedName("user_code") val userCode: String? = null,
    val email: String? = null,
    /** Better Auth */
    val name: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val role: String? = null,
    /** Better Auth */
    val image: String? = null,
    val avatar: String? = null,
    @SerializedName("avatar_source") val avatarSource: String? = null,
    @SerializedName("registered_at") val registeredAt: String? = null,
    @SerializedName("is_private") val isPrivate: Boolean? = null,
) {
    fun resolvedDisplayName(): String? =
        displayName?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: username

    fun resolvedAvatar(): String? =
        avatar?.takeIf { it.isNotBlank() } ?: image?.takeIf { it.isNotBlank() }
}

data class AuthSessionDto(
    val id: String? = null,
    val token: String? = null,
    val expiresAt: String? = null,
    val userId: String? = null,
)

/** GET /api/tv-last-air?tmdbId= */
data class TvLastAirDto(
    val fp: String? = null,
    val badge: String? = null,
    val meta: TvLastAirMetaDto? = null,
)

data class TvLastAirMetaDto(
    val tmdbId: Int? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val airDate: String? = null,
    val fingerprint: String? = null,
    val showStatus: String? = null,
    val inProduction: Boolean? = null,
    val airAt: Long? = null,
    val airPrecision: String? = null,
    val nextSeasonNumber: Int? = null,
    val nextEpisodeNumber: Int? = null,
    val nextAirDate: String? = null,
    val nextAirAt: Long? = null,
    val nextAirPrecision: String? = null,
)

/** GET /api/tv-schedule?tmdbId=&kinopoiskId=&release=1 */
data class TvScheduleDto(
    val tmdbId: Int? = null,
    val seasons: List<TvSeasonDto> = emptyList(),
    val release: TvReleaseDto? = null,
)

data class TvSeasonDto(
    val seasonNumber: Int? = null,
    val episodes: List<TvEpisodeDto> = emptyList(),
)

data class TvEpisodeDto(
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val title: String? = null,
    val airDate: String? = null,
)

data class TvReleaseDto(
    val context: TvReleaseContextDto? = null,
    /** Ключ "season:episode" → список озвучек */
    val dubs: Map<String, List<TvDubDto>> = emptyMap(),
)

data class TvReleaseContextDto(
    val networks: List<String> = emptyList(),
    val originCountry: List<String> = emptyList(),
)

data class TvDubDto(
    val name: String? = null,
    val at: Long? = null,
)

fun TitleDetailDto.isSeriesLike(): Boolean =
    mediaType.equals("tv", ignoreCase = true) ||
        mediaType.equals("anime", ignoreCase = true)

fun TitleDetailDto.playerItemId(): String? {
    idKp?.takeIf { it > 0 }?.let { return "kp:$it" }
    idTmdb?.takeIf { it > 0 }?.let { return "tmdb:$it" }
    return null
}

/** GET /api/player-choice?itemId=kp:123 */
data class PlayerChoiceDto(
    val total: Int? = null,
    val items: List<PlayerChoiceItemDto> = emptyList(),
)

data class PlayerChoiceItemDto(
    val source: String? = null,
    val count: Int? = null,
    val share: Double? = null,
)

/** GET /api/film/watchers?... */
data class FilmWatchersDto(
    val watchers: List<FilmWatcherDto> = emptyList(),
)

data class FilmWatcherDto(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val isStreamer: Boolean? = null,
    val isLive: Boolean? = null,
    val score: Double? = null,
    val rating: String? = null,
    val subscribed: Boolean? = null,
)

/** GET /api/player/{type}/{id} */
data class PlayerResponseDto(
    val players: List<PlayerSourceDto> = emptyList(),
    @SerializedName("quality_tags") val qualityTags: List<String> = emptyList(),
)

data class PlayerSourceDto(
    val name: String? = null,
    val quality: String? = null,
    val active: Boolean? = null,
    val share: Int? = null,
    val dub: String? = null,
    @SerializedName("iframe_url") val iframeUrl: String? = null,
    /** Локальный HTML-embed (Vibix/CVH SDK), не с сервера. */
    @Transient val embedHtml: String? = null,
) {
    fun hasPlayableContent(): Boolean =
        !iframeUrl.isNullOrBlank() || !embedHtml.isNullOrBlank()
}

/**
 * GET /api/player-config — формат как у сайта (`initialPlayerConfig` в RSC).
 * Список чипов = все ненулевые провайдеры конфига (не только player-choice).
 */
data class PlayerConfigDto(
    val available: Boolean? = null,
    val primaryPlayer: String? = null,
    val qualities: List<String>? = null,
    val dubs: List<String>? = null,
    @SerializedName("iframe_url") val iframeUrl: String? = null,
    val isDemo: Boolean? = null,
    val collapsUrl: String? = null,
    val resolvedKinopoiskId: String? = null,
    val alloha: PlayerAllohaDto? = null,
    val turbo: PlayerTurboDto? = null,
    val kodik: PlayerKodikDto? = null,
    val vibix: PlayerVibixDto? = null,
    val videoseed: PlayerVideoseedDto? = null,
    val cvh: PlayerCvhDto? = null,
    val veoveo: PlayerVeoveoDto? = null,
    val capabilities: Map<String, Boolean>? = null,
)

data class PlayerAllohaDto(
    val baseUrl: String? = null,
    val token: String? = null,
)

data class PlayerTurboDto(
    val baseUrl: String? = null,
    val siteId: Int? = null,
    val titleId: Int? = null,
    val matchedBy: String? = null,
)

data class PlayerKodikDto(
    val baseUrl: String? = null,
    val link: String? = null,
)

data class PlayerVibixDto(
    val publisherId: String? = null,
    val embedId: String? = null,
    val embedType: String? = null,
)

data class PlayerVideoseedDto(
    val baseUrl: String? = null,
    val type: String? = null,
)

data class PlayerCvhDto(
    val publisherId: String? = null,
)

data class PlayerVeoveoDto(
    val baseUrl: String? = null,
)

/** Порядок чипов как на сайте. */
val PLAYER_SOURCE_ORDER = listOf(
    "turbo", "collaps", "alloha", "vibix", "videoseed", "cvh", "veoveo", "kodik",
)

fun displayPlayerSourceName(source: String): String = when (source.lowercase()) {
    "alloha" -> "Alloha"
    "turbo" -> "Turbo"
    "collaps" -> "Collaps"
    "vibix" -> "Vibix"
    "videoseed" -> "VideoSeed"
    "cvh" -> "CVH"
    "veoveo" -> "VeoVeo"
    "kodik" -> "Kodik"
    else -> source.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun PlayerKodikDto.toEmbedUrl(): String? =
    baseUrl?.takeIf { it.isNotBlank() } ?: link?.takeIf { it.isNotBlank() }

private fun String.ensureAbsHttpUrl(): String =
    when {
        startsWith("//") -> "https:$this"
        startsWith("http://") || startsWith("https://") -> this
        else -> "https://$this"
    }

/** Добавляет query-параметр, если его ещё нет. */
private fun String.withQueryParam(key: String, value: String): String {
    if (contains(Regex("""([?&])${Regex.escape(key)}="""))) return this
    val sep = if (contains("?")) "&" else "?"
    return "$this$sep$key=${android.net.Uri.encode(value)}"
}

private fun String.withQueryParamSet(key: String, value: String): String {
    return try {
        val uri = android.net.Uri.parse(this)
        uri.buildUpon().clearQuery().apply {
            uri.queryParameterNames
                .filter { !it.equals(key, ignoreCase = true) }
                .forEach { name ->
                    uri.getQueryParameters(name).forEach { appendQueryParameter(name, it) }
                }
            appendQueryParameter(key, value)
        }.build().toString()
    } catch (_: Exception) {
        withQueryParam(key, value)
    }
}

private fun PlayerAllohaDto.toEmbedUrl(): String? {
    val base = baseUrl?.takeIf { it.isNotBlank() }?.ensureAbsHttpUrl() ?: return null
    val tok = token?.takeIf { it.isNotBlank() } ?: return base
    return base.withQueryParamSet("token", tok)
}

private fun PlayerTurboDto.toEmbedUrl(): String? =
    baseUrl?.takeIf { it.isNotBlank() }?.ensureAbsHttpUrl()?.withQueryParam("api", "1")

private fun String.toCollapsEmbedUrl(): String =
    ensureAbsHttpUrl().withQueryParam("host", "reyoho.com")

private fun PlayerVideoseedDto.toEmbedUrl(): String? =
    baseUrl?.takeIf { it.isNotBlank() }?.ensureAbsHttpUrl()

private fun PlayerVeoveoDto.toEmbedUrl(): String? =
    baseUrl?.takeIf { it.isNotBlank() }?.ensureAbsHttpUrl()

private fun PlayerVibixDto.toEmbedHtml(): String? {
    val pub = publisherId?.takeIf { it.isNotBlank() } ?: return null
    val id = embedId?.takeIf { it.isNotBlank() } ?: return null
    val type = embedType?.takeIf { it.isNotBlank() } ?: "movie"
    return PlayerEmbedHtml.vibix(publisherId = pub, embedId = id, embedType = type)
}

private fun PlayerCvhDto.toEmbedHtml(titleId: String?): String? {
    val pub = publisherId?.takeIf { it.isNotBlank() } ?: return null
    val kp = titleId?.takeIf { it.isNotBlank() } ?: return null
    return PlayerEmbedHtml.cvh(titleId = kp, publisherId = pub, aggregator = "kp")
}

/**
 * Источники как на сайте: baseUrl + правки query; Vibix/CVH — HTML SDK.
 */
fun PlayerConfigDto.toPlayerSources(): List<PlayerSourceDto> {
    val byKey = linkedMapOf<String, PlayerSourceDto>()
    val titleId = resolvedKinopoiskId?.takeIf { it.isNotBlank() }

    /** iframe + parent origin reyoho.com (как Alloha на сайте). */
    fun putShell(name: String, url: String?) {
        val src = url?.takeIf { it.isNotBlank() } ?: return
        byKey[name] = PlayerSourceDto(
            name = name,
            iframeUrl = src,
            embedHtml = PlayerEmbedHtml.iframe(src),
        )
    }

    fun putHtml(name: String, html: String?) {
        byKey[name] = PlayerSourceDto(name = name, embedHtml = html)
    }

    if (alloha != null) putShell("alloha", alloha.toEmbedUrl())
    // Turbo как на сайте: iframe под parent reyoho.com (top-level obrut часто пустой)
    if (turbo != null) putShell("turbo", turbo.toEmbedUrl())
    // Collaps в iframe-shell (как Alloha) — стабильнее viewport при повороте, чем top-level.
    if (!collapsUrl.isNullOrBlank() || capabilities?.get("collapsCatalog") == true) {
        putShell("collaps", collapsUrl?.takeIf { it.isNotBlank() }?.toCollapsEmbedUrl())
    }
    if (vibix != null) {
        // Прямой kinescope URL → «контент ещё не добавлен»; на сайте — rendex+ins.
        putHtml("vibix", vibix.toEmbedHtml())
    }
    if (videoseed != null) putShell("videoseed", videoseed.toEmbedUrl())
    if (cvh != null) putHtml("cvh", cvh.toEmbedHtml(titleId))
    if (veoveo != null) putShell("veoveo", veoveo.toEmbedUrl())
    if (kodik != null) putShell("kodik", kodik.toEmbedUrl()?.ensureAbsHttpUrl())

    if (byKey.isEmpty()) {
        iframeUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val name = primaryPlayer?.trim()?.lowercase()?.ifBlank { null } ?: "player"
            putShell(name, url.ensureAbsHttpUrl())
        }
    }

    val orderedKeys = PLAYER_SOURCE_ORDER.filter { byKey.containsKey(it) } +
        byKey.keys.filter { it !in PLAYER_SOURCE_ORDER }

    return orderedKeys.mapIndexed { index, name ->
        byKey.getValue(name).copy(active = index == 0)
    }
}

/** Чипы = источники конфига + доли из player-choice (0%, если голосов нет). */
fun mergePlayerChoices(
    sources: List<PlayerSourceDto>,
    votes: List<PlayerChoiceItemDto>,
): List<PlayerChoiceItemDto> {
    if (sources.isEmpty() && votes.isEmpty()) return emptyList()
    val voteBy = votes.associateBy { it.source?.lowercase().orEmpty() }
    val keys = linkedSetOf<String>()
    PLAYER_SOURCE_ORDER.forEach { key ->
        if (sources.any { it.name.equals(key, ignoreCase = true) }) keys.add(key)
    }
    sources.mapNotNull { it.name?.lowercase() }.forEach { keys.add(it) }
    // если конфига нет — хотя бы то, что пришло в votes
    if (sources.isEmpty()) {
        PLAYER_SOURCE_ORDER.forEach { key ->
            if (voteBy.containsKey(key)) keys.add(key)
        }
        votes.mapNotNull { it.source?.lowercase() }.forEach { keys.add(it) }
    }
    return keys.map { key ->
        val v = voteBy[key]
        PlayerChoiceItemDto(
            source = key,
            count = v?.count,
            share = v?.share ?: 0.0,
        )
    }
}

fun TvScheduleDto.dubsFor(season: Int?, episode: Int?): List<TvDubDto> {
    if (season == null || episode == null) return emptyList()
    return release?.dubs?.get("$season:$episode").orEmpty()
}

/** «ColdFilm, Dragon Money Studio и ещё 11» */
fun List<TvDubDto>.formatDubsSummary(maxNames: Int = 2): String? {
    val names = mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }.distinct()
    if (names.isEmpty()) return null
    val shown = names.take(maxNames)
    val rest = names.size - shown.size
    return buildString {
        append(shown.joinToString(", "))
        if (rest > 0) append(" и ещё $rest")
    }
}

data class DurationDto(
    val minutes: Int? = null,
    val formatted: String? = null,
)

data class ProductionDto(
    val distributor: String? = null,
    @SerializedName("rights_holder") val rightsHolder: String? = null,
    val budget: String? = null,
)

data class SeriesTotalsDto(
    val seasons: Int? = null,
    val episodes: Int? = null,
)

data class AboutFilmRowDto(
    val id: String? = null,
    val label: String? = null,
    val value: String? = null,
    /** Имя → KP person id; на сайте /name/{id} */
    val personIds: Map<String, Int>? = null,
)

/** UI-модель деталей тайтла (мапится из /api/film-info). */
data class TitleDetailDto(
    val idKp: Int? = null,
    val idTmdb: Int? = null,
    val mediaType: String? = null,
    val title: TitleNameDto? = null,
    val year: Int? = null,
    val yearStart: Int? = null,
    val yearEnd: Int? = null,
    val description: String? = null,
    val tagline: String? = null,
    val duration: DurationDto? = null,
    val ageRating: String? = null,
    val ratings: RatingsDto? = null,
    val rtScore: Int? = null,
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val director: String? = null,
    val directorPersonIds: Map<String, Int> = emptyMap(),
    val directors: List<String> = emptyList(),
    val cast: List<CastMemberDto> = emptyList(),
    val castTotal: Int? = null,
    /** Ссылка на полный каст (KP): https://www.kinopoisk.ru/film/{id}/cast/ */
    val castPageHref: String? = null,
    val awardsCount: Int? = null,
    val production: ProductionDto? = null,
    val aboutFilm: List<AboutFilmRowDto> = emptyList(),
    val poster: String? = null,
    val backdrop: String? = null,
    val trailer: TrailerDto? = null,
    val totals: SeriesTotalsDto? = null,
    val status: String? = null,
    val similar: List<TitleDto> = emptyList(),
    val sequels: List<TitleDto> = emptyList(),
    val prequels: List<TitleDto> = emptyList(),
    val remakes: List<TitleDto> = emptyList(),
)

/** Ответ https://reyoho.com/api/film-info */
data class FilmInfoResponseDto(
    val details: FilmInfoDetailsDto? = null,
    val staff: List<Any> = emptyList(),
    val related: List<FilmInfoRelatedDto> = emptyList(),
    val recommendations: List<FilmInfoRecommendationDto> = emptyList(),
    val backdropUrl: String? = null,
    val resolvedTmdbId: Int? = null,
    val resolvedMediaType: String? = null,
    val distributors: List<String> = emptyList(),
    val rightsHolders: List<String> = emptyList(),
    val aboutFilm: List<AboutFilmRowDto> = emptyList(),
    val mainCast: FilmInfoMainCastDto? = null,
    val awards: Any? = null,
    val trailer: FilmInfoTrailerDto? = null,
    val rtRating: FilmInfoRtRatingDto? = null,
    val rtUrl: String? = null,
)

data class FilmInfoDetailsDto(
    val kinopoiskId: Int? = null,
    val imdbId: String? = null,
    val nameRu: String? = null,
    val nameEn: String? = null,
    val nameOriginal: String? = null,
    val posterUrl: String? = null,
    val posterUrlPreview: String? = null,
    val year: Int? = null,
    val filmLength: Int? = null,
    val slogan: String? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val ratingKinopoisk: Double? = null,
    val ratingKinopoiskVoteCount: Int? = null,
    val ratingImdb: Double? = null,
    val ratingImdbVoteCount: Int? = null,
    val ratingAgeLimits: String? = null,
    val countries: List<FilmInfoCountryDto> = emptyList(),
    val genres: List<FilmInfoGenreDto> = emptyList(),
    val startYear: Int? = null,
    val endYear: Int? = null,
    val type: String? = null,
    val webUrl: String? = null,
)

data class FilmInfoCountryDto(val country: String? = null)
data class FilmInfoGenreDto(val genre: String? = null)

data class FilmInfoRelatedDto(
    val filmId: Int? = null,
    val nameEn: String? = null,
    val nameRu: String? = null,
    val posterUrl: String? = null,
    val nameOriginal: String? = null,
    val relationType: String? = null,
    val mediaType: String? = null,
)

data class FilmInfoRecommendationDto(
    val id: Int? = null,
    val year: String? = null,
    val title: String? = null,
    val rating: Double? = null,
    val mediaType: String? = null,
    val posterUrl: String? = null,
    val voteCount: Int? = null,
    val kpId: Int? = null,
)

data class FilmInfoMainCastDto(
    val preview: List<FilmInfoCastPreviewDto> = emptyList(),
    val total: Int? = null,
    val castPageHref: String? = null,
)

data class FilmInfoCastPreviewDto(
    val id: Int? = null,
    val name: String? = null,
    val href: String? = null,
)

data class FilmInfoTrailerClipDto(
    val title: String? = null,
    val youtubeKey: String? = null,
    val vkEmbedUrl: String? = null,
    val durationSec: Int? = null,
)

data class FilmInfoTrailerDto(
    val title: String? = null,
    val youtubeKey: String? = null,
    val publishedAt: String? = null,
    val thumbnailUrl: String? = null,
    val vkEmbedUrl: String? = null,
    val durationSec: Int? = null,
    val clips: List<FilmInfoTrailerClipDto> = emptyList(),
)

data class FilmInfoRtRatingDto(
    val score: Int? = null,
    val votes: Int? = null,
)

fun FilmInfoResponseDto.toTitleDetail(): TitleDetailDto {
    val d = details
    val aboutMap = aboutFilm.associate { (it.id ?: "") to (it.value ?: "") }
    fun relatedOf(type: String) = related
        .filter { it.relationType.equals(type, ignoreCase = true) }
        .map {
            TitleDto(
                idKp = it.filmId,
                mediaType = it.mediaType,
                title = TitleNameDto(ru = it.nameRu, original = it.nameOriginal ?: it.nameEn),
                poster = it.posterUrl,
            )
        }
    return TitleDetailDto(
        idKp = d?.kinopoiskId,
        idTmdb = resolvedTmdbId,
        mediaType = resolvedMediaType,
        title = TitleNameDto(
            ru = d?.nameRu,
            original = d?.nameOriginal ?: d?.nameEn,
        ),
        year = d?.year,
        yearStart = d?.startYear,
        yearEnd = d?.endYear,
        description = d?.description ?: d?.shortDescription,
        tagline = d?.slogan,
        duration = DurationDto(
            minutes = d?.filmLength,
            formatted = aboutMap["runtime"],
        ),
        ageRating = d?.ratingAgeLimits,
        ratings = RatingsDto(
            kinopoisk = d?.ratingKinopoisk?.let {
                ScoreDto(score = it, votes = d.ratingKinopoiskVoteCount)
            },
            imdb = d?.ratingImdb?.let {
                ScoreDto(score = it, votes = d.ratingImdbVoteCount)
            },
        ),
        rtScore = rtRating?.score,
        genres = d?.genres?.mapNotNull { it.genre }.orEmpty(),
        countries = d?.countries?.mapNotNull { it.country }.orEmpty(),
        director = aboutMap["director"],
        directorPersonIds = aboutFilm.find { it.id == "director" }?.personIds.orEmpty(),
        cast = mainCast?.preview?.map {
            CastMemberDto(
                id = it.id,
                name = it.name,
                poster = it.id?.takeIf { id -> id > 0 }
                    ?.let { id -> "/api/img/kp/actor_posters/kp/$id.jpg" },
            )
        }.orEmpty(),
        castTotal = mainCast?.total,
        castPageHref = mainCast?.castPageHref?.takeIf { it.isNotBlank() }
            ?: d?.kinopoiskId?.takeIf { it > 0 }?.let { "https://www.kinopoisk.ru/film/$it/cast/" },
        production = ProductionDto(
            distributor = aboutMap["distributor"] ?: distributors.firstOrNull(),
            rightsHolder = aboutMap["rightsHolder"]
                ?: rightsHolders.takeIf { it.isNotEmpty() }?.joinToString(", "),
            budget = aboutMap["budget"],
        ),
        aboutFilm = aboutFilm.filter { it.id != "dataSource" },
        poster = d?.posterUrl,
        backdrop = backdropUrl ?: d?.posterUrl,
        trailer = trailer?.let { t ->
            val mappedClips = t.clips.map { c ->
                TrailerClipDto(
                    title = c.title,
                    youtubeKey = c.youtubeKey,
                    vkEmbedUrl = c.vkEmbedUrl,
                    durationSec = c.durationSec,
                )
            }
            TrailerDto(
                url = t.youtubeKey?.let { "https://www.youtube.com/watch?v=$it" }
                    ?: mappedClips.firstOrNull { it.hasYoutube }?.youtubeWatchUrl,
                title = t.title,
                date = t.publishedAt?.take(10),
                thumbnail = t.thumbnailUrl,
                vkEmbedUrl = t.vkEmbedUrl
                    ?: mappedClips.firstOrNull { it.hasVk }?.vkEmbedUrl,
                clips = mappedClips.ifEmpty {
                    listOfNotNull(
                        TrailerClipDto(
                            title = t.title,
                            youtubeKey = t.youtubeKey,
                            vkEmbedUrl = t.vkEmbedUrl,
                            durationSec = t.durationSec,
                        ).takeIf { it.isPlayable },
                    )
                },
            )
        },
        similar = recommendations.map {
            TitleDto(
                idKp = it.kpId,
                idTmdb = it.id,
                mediaType = it.mediaType,
                title = TitleNameDto(ru = it.title),
                year = it.year?.toIntOrNull(),
                poster = it.posterUrl,
                ratings = it.rating?.let { r ->
                    RatingsDto(kinopoisk = ScoreDto(score = r, votes = it.voteCount))
                },
            )
        },
        sequels = relatedOf("SEQUEL"),
        prequels = relatedOf("PREQUEL"),
        remakes = relatedOf("REMAKE"),
    )
}

data class SuggestItemDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("poster") val poster: String? = null,
)

/** GET /api/search/persons?source=kinodata&q= */
data class SearchPersonDto(
    val id: Int? = null,
    val name: String? = null,
    val originalName: String? = null,
    val photoUrl: String? = null,
)

data class SearchPersonsResponse(
    val persons: List<SearchPersonDto> = emptyList(),
)

data class SearchResponseDto(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val results: List<TitleDto> = emptyList(),
    val items: List<TitleDto> = emptyList(),
)

/** Ответ /api/search/discover — сайт: results + page/totalPages/hasMore. */
data class DiscoverResponseDto(
    val results: List<DiscoverItemDto> = emptyList(),
    val items: List<DiscoverItemDto> = emptyList(),
    val total: Int? = null,
    val page: Int? = null,
    val totalPages: Int? = null,
    val hasMore: Boolean? = null,
    val source: String? = null,
) {
    fun list(): List<DiscoverItemDto> = results.ifEmpty { items }

    fun resolvedTotal(): Int {
        total?.takeIf { it > 0 }?.let { return it }
        val size = list().size
        val pages = totalPages ?: return size
        if (pages <= 1 || size == 0) return size
        return if (hasMore == true) pages * size else (pages - 1) * size + size
    }
}

data class DiscoverItemDto(
    val id: Int? = null,
    val tmdbId: Int? = null,
    val kinopoiskId: Int? = null,
    @SerializedName("id_kp") val idKp: Int? = null,
    @SerializedName("id_tmdb") val idTmdb: Int? = null,
    val mediaType: String? = null,
    @SerializedName("media_type") val mediaTypeSnake: String? = null,
    val title: com.google.gson.JsonElement? = null,
    val originalTitle: String? = null,
    val year: com.google.gson.JsonElement? = null,
    val posterUrl: String? = null,
    val poster: String? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val ratings: DiscoverRatingsDto? = null,
)

data class DiscoverRatingsDto(
    val kp: Double? = null,
    val imdb: Double? = null,
    @SerializedName("kinopoisk") val kinopoisk: ScoreDto? = null,
)

data class SearchFacetsDto(
    val types: List<FacetItemDto> = emptyList(),
    val genres: List<FacetItemDto> = emptyList(),
    val countries: List<FacetItemDto> = emptyList(),
    val regions: List<FacetItemDto> = emptyList(),
    val years: YearRangeDto? = null,
)

data class FacetItemDto(
    val value: String? = null,
    val label: String? = null,
    val count: Int? = null,
    val available: Boolean? = true,
)

data class YearRangeDto(
    val min: Int? = null,
    val max: Int? = null,
)

data class RegionMappingDto(
    val regions: List<RegionItemDto> = emptyList(),
)

data class RegionItemDto(
    val slug: String? = null,
    val label: String? = null,
    val countries: List<String> = emptyList(),
    @SerializedName("countries_count") val countriesCount: Int? = null,
)

data class SearchFiltersCatalogDto(
    val kinds: Map<String, Int> = emptyMap(),
    val tags: List<FilterTagDto> = emptyList(),
    val platforms: List<FilterPlatformDto> = emptyList(),
    val companies: List<String> = emptyList(),
)

data class FilterTagDto(
    val tag: String? = null,
    val kind: String? = null,
)

data class FilterPlatformDto(
    val slug: String? = null,
    val name: String? = null,
)

fun TitleDto.displayTitle(): String = title?.ru ?: title?.original ?: "Без названия"

fun TitleDto.displayYear(): Int? = year ?: yearStart

fun TitleDto.typeLabel(): String = when (mediaType) {
    "tv" -> "СЕРИАЛ"
    "anime" -> "АНИМЕ"
    "animation" -> "МУЛЬТ"
    else -> "ФИЛЬМ"
}

fun String?.absMediaUrl(): String? {
    if (isNullOrBlank()) return null
    if (startsWith("http://") || startsWith("https://")) return this
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return base + if (startsWith("/")) this else "/$this"
}

/** Абсолютные URL постеров/бэкдропа/каста — один раз после film-info. */
fun TitleDetailDto.withResolvedMediaUrls(): TitleDetailDto = copy(
    poster = poster.absMediaUrl(),
    backdrop = backdrop.absMediaUrl() ?: poster.absMediaUrl(),
    trailer = trailer?.copy(thumbnail = trailer.thumbnail.absMediaUrl()),
    cast = cast.map { it.copy(poster = it.poster.absMediaUrl()) },
    similar = similar.map { it.copy(poster = it.poster.absMediaUrl()) },
    sequels = sequels.map { it.copy(poster = it.poster.absMediaUrl()) },
    prequels = prequels.map { it.copy(poster = it.poster.absMediaUrl()) },
    remakes = remakes.map { it.copy(poster = it.poster.absMediaUrl()) },
)

fun String?.toRelativeMediaPath(): String? {
    if (isNullOrBlank()) return null
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return when {
        startsWith(base) -> removePrefix(base).ifBlank { "/" }
        startsWith("http://") || startsWith("https://") -> {
            // оставить path после host
            try {
                val path = java.net.URI(this).path
                if (path.isNullOrBlank()) this else path
            } catch (_: Exception) {
                this
            }
        }
        else -> this
    }
}

fun TitleDetailDto.normalizedMediaType(): String = when {
    mediaType.equals("tv", ignoreCase = true) -> "tv"
    mediaType.equals("anime", ignoreCase = true) -> "anime"
    else -> "movie"
}

/** Как на сайте: /series/{kp} для сериалов, /film/{kp} для фильмов. */
fun TitleDetailDto.siteHref(): String? {
    val kp = idKp?.takeIf { it > 0 } ?: return null
    return when (normalizedMediaType()) {
        "tv", "anime" -> "/series/$kp"
        else -> "/film/$kp"
    }
}

fun TitleDetailDto.toFavoriteActivityItem(): MeActivityItemDto? {
    val kp = idKp?.takeIf { it > 0 } ?: return null
    val type = normalizedMediaType()
    val titleText = title?.ru ?: title?.original ?: return null
    val href = siteHref() ?: return null
    return MeActivityItemDto(
        id = "kp:$kp",
        href = href,
        title = titleText,
        year = (year ?: yearStart)?.toString(),
        posterUrl = poster.toFavoritePosterPath(),
        mediaType = type,
        ts = System.currentTimeMillis(),
    )
}

fun TitleDetailDto.toDiaryRequest(statusLabel: String): DiaryRequestDto? {
    val kp = idKp?.takeIf { it > 0 } ?: return null
    val href = siteHref() ?: return null
    val titleText = title?.ru ?: title?.original ?: return null
    return DiaryRequestDto(
        itemId = "kp:$kp",
        href = href,
        title = titleText,
        year = (year ?: yearStart)?.toString(),
        mediaType = normalizedMediaType(),
        sourceReaction = "manual",
        statusLabel = statusLabel,
        updateStatusIfItemExists = true,
    )
}

/** Реакция с плеера: sourceReaction=like|dislike, без statusLabel. */
fun TitleDetailDto.toDiaryReactionRequest(reaction: String): DiaryRequestDto? {
    val kp = idKp?.takeIf { it > 0 } ?: return null
    val href = siteHref() ?: return null
    val titleText = title?.ru ?: title?.original ?: return null
    return DiaryRequestDto(
        itemId = "kp:$kp",
        href = href,
        title = titleText,
        year = (year ?: yearStart)?.toString(),
        mediaType = normalizedMediaType(),
        sourceReaction = reaction,
    )
}

/**
 * Body setRating как на сайте:
 * like → value:10, isNotInterested:false
 * dislike → value:null, isNotInterested:true (null обязан быть в JSON).
 */
fun TitleDetailDto.toSetRatingBody(reaction: String): com.google.gson.JsonObject? {
    val kp = idKp?.takeIf { it > 0 } ?: return null
    val href = siteHref() ?: return null
    val titleText = title?.ru ?: title?.original ?: return null
    val input = com.google.gson.JsonObject().apply {
        addProperty("itemId", "kp:$kp")
        addProperty("href", href)
        addProperty("title", titleText)
        addProperty("mediaType", normalizedMediaType())
        if (reaction == "like") {
            addProperty("value", 10)
        } else {
            add("value", com.google.gson.JsonNull.INSTANCE)
        }
        addProperty("isNotInterested", reaction == "dislike")
    }
    return com.google.gson.JsonObject().apply {
        addProperty("op", "setRating")
        add("input", input)
    }
}

/** Постер для /api/me/activity — относительный path с w185, как на сайте. */
fun String?.toFavoritePosterPath(): String? {
    val path = toRelativeMediaPath() ?: return null
    return path
        .replace(Regex("/p/w\\d+/"), "/p/w185/")
        .replace("/p/original/", "/p/w185/")
}

fun RecommendationItemDto.toTitle(): TitleDto {
    val tmdb = tmdbId ?: itemId
        ?.takeIf { it.startsWith("tmdb:") }
        ?.substringAfter("tmdb:")
        ?.toIntOrNull()
    val kp = itemId
        ?.takeIf { it.startsWith("kp:") }
        ?.substringAfter("kp:")
        ?.toIntOrNull()
        ?: href
            ?.substringAfterLast('/')
            ?.substringBefore('?')
            ?.toIntOrNull()
    return TitleDto(
        idKp = kp,
        idTmdb = tmdb,
        mediaType = mediaType ?: "movie",
        title = TitleNameDto(ru = title),
        year = year?.toIntOrNull(),
        poster = posterUrl.absMediaUrl(),
        ratings = rating?.let { RatingsDto(kinopoisk = ScoreDto(it)) },
        reason = sourceTitle?.takeIf { it.isNotBlank() }
            ?: reason?.takeIf { it.isNotBlank() },
    )
}

fun AnimeItemDto.toTitle(): TitleDto {
    val display = ruTitle?.takeIf { it.isNotBlank() }
        ?: russian?.takeIf { it.isNotBlank() }
        ?: title
    return TitleDto(
        idKp = kpId,
        idTmdb = tmdbId,
        mediaType = mediaType ?: "anime",
        title = TitleNameDto(ru = display, original = romaji ?: title),
        year = year,
        poster = (ruPosterUrl ?: coverImage).absMediaUrl(),
        ratings = RatingsDto(
            kinopoisk = (kpRating ?: score)?.let { ScoreDto(it) },
            imdb = imdbRating?.let { ScoreDto(it) },
        ),
    )
}

fun AnimeSeasonLabelDto?.displayRu(): String {
    if (this == null) return ""
    val seasonRu = when (season?.uppercase()) {
        "WINTER" -> "зима"
        "SPRING" -> "весна"
        "SUMMER" -> "лето"
        "FALL" -> "осень"
        else -> season?.lowercase().orEmpty()
    }
    return listOfNotNull(
        seasonRu.takeIf { it.isNotBlank() },
        year?.toString(),
    ).joinToString(" ")
}

fun TrendingItemDto.toTitle(ratings: Map<String, TrendingRatingDto>): TitleDto {
    val media = mediaType ?: "movie"
    val tmdb = id
    val key = if (tmdb != null) "$media:$tmdb" else null
    val r = key?.let { ratings[it] }
    return TitleDto(
        idKp = kinopoiskFilmId ?: r?.kinopoiskFilmId,
        idTmdb = tmdb,
        mediaType = media,
        title = TitleNameDto(ru = title, original = originalTitle),
        year = year?.toIntOrNull(),
        poster = posterUrl.absMediaUrl(),
        backdrop = backdropUrl.absMediaUrl(),
        ratings = RatingsDto(
            kinopoisk = r?.kp?.let { ScoreDto(it) },
            imdb = r?.imdb?.let { ScoreDto(it) },
        ),
    )
}

fun TrailerItemDto.toTitle(): TitleDto {
    val yt = youtubeKey
    return TitleDto(
        idTmdb = tmdbId,
        mediaType = mediaType,
        title = TitleNameDto(ru = title),
        year = releaseYear,
        poster = backdropUrl.absMediaUrl(),
        backdrop = backdropUrl.absMediaUrl(),
        trailer = TrailerDto(
            url = yt?.let { "https://www.youtube.com/watch?v=$it" },
            title = trailerTitle,
            thumbnail = backdropUrl.absMediaUrl(),
        ),
    )
}

fun RecentItemDto.toTitle(): TitleDto = TitleDto(
    idKp = id,
    mediaType = mediaType,
    title = TitleNameDto(ru = title),
    year = year,
    poster = poster.absMediaUrl(),
)

fun DiscoverItemDto.toTitle(): TitleDto {
    val ru = when {
        title == null || title.isJsonNull -> null
        title.isJsonPrimitive -> title.asString
        title.isJsonObject -> title.asJsonObject.get("ru")?.asString
            ?: title.asJsonObject.get("original")?.asString
        else -> null
    }
    val original = originalTitle
        ?: title?.takeIf { it.isJsonObject }?.asJsonObject?.get("original")?.asString
    val yearInt = when {
        year == null || year.isJsonNull -> null
        year.isJsonPrimitive && year.asJsonPrimitive.isNumber -> year.asInt
        year.isJsonPrimitive -> year.asString.toIntOrNull()
        else -> null
    }
    val media = mediaType ?: mediaTypeSnake
    return TitleDto(
        idKp = kinopoiskId ?: idKp ?: id,
        idTmdb = tmdbId ?: idTmdb,
        mediaType = media,
        title = TitleNameDto(ru = ru, original = original),
        year = yearInt,
        poster = (posterUrl ?: poster).absMediaUrl(),
        ratings = RatingsDto(
            kinopoisk = (ratings?.kp ?: rating)?.let { ScoreDto(score = it) }
                ?: ratings?.kinopoisk,
            imdb = ratings?.imdb?.let { ScoreDto(score = it) },
        ),
    )
}
