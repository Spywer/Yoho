package com.spywer.yoho.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.MeActivityRequest
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SessionCookieJar
import com.spywer.yoho.data.remote.PlayerChoiceItemDto
import com.spywer.yoho.data.remote.PlayerConfigDto
import com.spywer.yoho.data.remote.PlayerConfigPageParser
import com.spywer.yoho.data.remote.PlayerSourceDto
import com.spywer.yoho.data.remote.TitleDetailDto
import com.spywer.yoho.data.remote.TvLastAirDto
import com.spywer.yoho.data.remote.TvScheduleDto
import com.spywer.yoho.data.remote.fetchTitlePageHtml
import com.spywer.yoho.data.remote.hasPlayableSource
import com.spywer.yoho.data.remote.isSeriesLike
import com.spywer.yoho.data.remote.mergePlayerChoices
import com.spywer.yoho.data.remote.playerItemId
import com.spywer.yoho.data.remote.toDiaryRequest
import com.spywer.yoho.data.remote.toFavoriteActivityItem
import com.spywer.yoho.data.remote.toPlayerSources
import com.spywer.yoho.data.remote.toTitleDetail
import com.spywer.yoho.data.remote.withResolvedMediaUrls
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TitleDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: TitleDetailDto? = null,
    val isFavorite: Boolean = false,
    val favoriteBusy: Boolean = false,
    val favoriteError: String? = null,
    val schedule: TvScheduleDto? = null,
    val scheduleLoading: Boolean = false,
    val selectedSeason: Int? = null,
    val lastAir: TvLastAirDto? = null,
    val playerChoices: List<PlayerChoiceItemDto> = emptyList(),
    val playerSources: List<PlayerSourceDto> = emptyList(),
    val selectedPlayerSource: String? = null,
    val playerIframeUrl: String? = null,
    val playerEmbedHtml: String? = null,
    val playerLoading: Boolean = false,
    /** Сайт отдаёт конфиг только с yoho_session (takenDownReason=login). */
    val playerNeedsLogin: Boolean = false,
    val diaryStatusLabel: String? = null,
    val diaryBusy: Boolean = false,
    val diaryError: String? = null,
    /** Одноразовый toast после действия пользователя. */
    val diaryNotify: String? = null,
)

class TitleDetailViewModel : ViewModel() {
    private val api = NetworkModule.api
    private val _state = MutableStateFlow(TitleDetailUiState())
    val state: StateFlow<TitleDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var playerJob: Job? = null
    private var scheduleJob: Job? = null
    private var lastAirJob: Job? = null

    fun load(idKp: Int?, idTmdb: Int?, mediaType: String?) {
        if ((idKp == null || idKp <= 0) && (idTmdb == null || idTmdb <= 0)) {
            _state.update { it.copy(loading = false, error = "Нет id тайтла", detail = null) }
            return
        }
        loadJob?.cancel()
        playerJob?.cancel()
        scheduleJob?.cancel()
        lastAirJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    favoriteError = null,
                    diaryStatusLabel = null,
                    diaryError = null,
                    diaryNotify = null,
                    schedule = null,
                    selectedSeason = null,
                    lastAir = null,
                    playerChoices = emptyList(),
                    playerSources = emptyList(),
                    selectedPlayerSource = null,
                    playerIframeUrl = null,
                    playerEmbedHtml = null,
                    playerNeedsLogin = false,
                )
            }
            runCatching {
                val raw = api.filmInfo(
                    kinopoiskId = idKp?.takeIf { it > 0 },
                    tmdbId = idTmdb?.takeIf { it > 0 },
                    mediaType = mediaType?.takeIf { it.isNotBlank() } ?: "movie",
                ).toTitleDetail()
                withContext(Dispatchers.Default) { raw.withResolvedMediaUrls() }
            }.onSuccess { detail ->
                _state.update {
                    it.copy(loading = false, detail = detail, error = null, isFavorite = false)
                }
                refreshFavoriteState(detail.idKp)
                refreshDiaryState(detail.playerItemId())
                loadPlayerBlock(detail)
                if (detail.isSeriesLike()) {
                    loadSchedule(detail.idTmdb, detail.idKp)
                    loadLastAir(detail.idTmdb)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Не удалось загрузить", detail = null)
                }
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        _state.update { it.copy(selectedSeason = seasonNumber) }
    }

    fun selectPlayerSource(source: String) {
        val matched = _state.value.playerSources
            .firstOrNull { it.name.equals(source, ignoreCase = true) }
        _state.update {
            it.copy(
                selectedPlayerSource = source,
                playerIframeUrl = matched?.iframeUrl?.takeIf { url -> url.isNotBlank() },
                playerEmbedHtml = matched?.embedHtml?.takeIf { html -> html.isNotBlank() },
            )
        }
    }

    private fun pickSource(
        sources: List<PlayerSourceDto>,
        preferred: String?,
    ): PlayerSourceDto? {
        if (preferred != null) {
            sources.firstOrNull { it.name.equals(preferred, ignoreCase = true) }
                ?.takeIf { it.hasPlayableContent() }
                ?.let { return it }
        }
        return sources.firstOrNull { it.hasPlayableContent() } ?: sources.firstOrNull()
    }

    fun toggleFavorite() {
        val detail = _state.value.detail ?: return
        val item = detail.toFavoriteActivityItem() ?: return
        if (_state.value.favoriteBusy) return
        if (!SessionRepository.state.value.isLoggedIn &&
            !SessionCookieJar.get().hasSessionCookie()
        ) {
            _state.update {
                it.copy(favoriteError = "Войдите, чтобы добавить в избранное")
            }
            return
        }
        val adding = !_state.value.isFavorite
        viewModelScope.launch {
            _state.update { it.copy(favoriteBusy = true, favoriteError = null) }
            runCatching {
                val resp = api.meActivity(
                    MeActivityRequest(
                        op = if (adding) "addFavorite" else "removeFavorite",
                        item = item,
                    ),
                )
                when {
                    resp.isSuccessful || resp.code() == 204 -> Unit
                    resp.code() == 401 || resp.code() == 403 ->
                        error("Войдите, чтобы добавить в избранное")
                    else -> error("HTTP ${resp.code()}")
                }
            }.onSuccess {
                _state.update {
                    it.copy(favoriteBusy = false, isFavorite = adding, favoriteError = null)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        favoriteBusy = false,
                        favoriteError = e.message ?: "Не удалось обновить избранное",
                    )
                }
            }
        }
    }

    fun consumeDiaryNotify() {
        _state.update { it.copy(diaryNotify = null) }
    }

    /** POST /api/diary — statusLabel как на сайте («Хочу посмотреть» и т.п.). */
    fun setDiaryStatus(statusLabel: String) {
        val detail = _state.value.detail ?: return
        val body = detail.toDiaryRequest(statusLabel) ?: return
        if (_state.value.diaryBusy) return
        if (!SessionRepository.state.value.isLoggedIn &&
            !SessionCookieJar.get().hasSessionCookie()
        ) {
            _state.update {
                it.copy(diaryError = "Войдите, чтобы добавить в дневник", diaryNotify = "Войдите, чтобы добавить в дневник")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(diaryBusy = true, diaryError = null) }
            runCatching {
                val resp = api.diary(body)
                when {
                    resp.isSuccessful -> resp.body()?.entry?.statusLabel ?: statusLabel
                    resp.code() == 401 || resp.code() == 403 ->
                        error("Войдите, чтобы добавить в дневник")
                    else -> error("HTTP ${resp.code()}")
                }
            }.onSuccess { label ->
                _state.update {
                    it.copy(
                        diaryBusy = false,
                        diaryStatusLabel = label,
                        diaryError = null,
                        diaryNotify = "Добавлено: $label",
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        diaryBusy = false,
                        diaryError = e.message ?: "Не удалось добавить в дневник",
                        diaryNotify = e.message ?: "Не удалось добавить в дневник",
                    )
                }
            }
        }
    }

    /** DELETE /api/diary?itemId=kp:… */
    fun removeFromDiary() {
        val itemId = _state.value.detail?.playerItemId() ?: return
        if (_state.value.diaryBusy) return
        if (!SessionRepository.state.value.isLoggedIn &&
            !SessionCookieJar.get().hasSessionCookie()
        ) {
            _state.update {
                it.copy(diaryNotify = "Войдите, чтобы изменить дневник")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(diaryBusy = true, diaryError = null) }
            runCatching {
                val resp = api.diaryRemove(itemId)
                when {
                    resp.isSuccessful && resp.body()?.removed != false -> Unit
                    resp.code() == 401 || resp.code() == 403 ->
                        error("Войдите, чтобы изменить дневник")
                    else -> error("HTTP ${resp.code()}")
                }
            }.onSuccess {
                _state.update {
                    it.copy(
                        diaryBusy = false,
                        diaryStatusLabel = null,
                        diaryError = null,
                        diaryNotify = "Убрано из дневника",
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        diaryBusy = false,
                        diaryError = e.message ?: "Не удалось убрать из дневника",
                        diaryNotify = e.message ?: "Не удалось убрать из дневника",
                    )
                }
            }
        }
    }

    private fun refreshDiaryState(itemId: String?) {
        val id = itemId?.takeIf { it.isNotBlank() } ?: return
        if (!SessionRepository.state.value.isLoggedIn &&
            !SessionCookieJar.get().hasSessionCookie()
        ) {
            return
        }
        viewModelScope.launch {
            runCatching { api.diaryContains(id) }
                .onSuccess { resp ->
                    if (!resp.isSuccessful) return@onSuccess
                    val body = resp.body() ?: return@onSuccess
                    // Как на сайте: { inDiary: true, statusLabel: "Хочу посмотреть" }
                    val inDiary = body.inDiary == true
                    _state.update {
                        it.copy(
                            diaryStatusLabel = when {
                                inDiary -> body.statusLabel?.takeIf { s -> s.isNotBlank() }
                                    ?: ""
                                else -> null
                            },
                        )
                    }
                }
        }
    }

    private fun loadPlayerBlock(detail: TitleDetailDto) {
        playerJob?.cancel()
        val itemId = detail.playerItemId()
        val media = detail.mediaType?.takeIf { it.isNotBlank() } ?: "movie"
        val playerType = when {
            media.equals("tv", true) || media.equals("anime", true) -> "tv"
            else -> "movie"
        }
        val playerId = (detail.idTmdb?.takeIf { it > 0 } ?: detail.idKp)?.toString()
        if (itemId == null || playerId == null) {
            _state.update {
                it.copy(
                    playerLoading = false,
                    playerChoices = emptyList(),
                    playerSources = emptyList(),
                    selectedPlayerSource = null,
                    playerIframeUrl = null,
                    playerEmbedHtml = null,
                    playerNeedsLogin = false,
                )
            }
            return
        }
        playerJob = viewModelScope.launch {
            _state.update { it.copy(playerLoading = true) }
            coroutineScope {
                val choiceD = async {
                    runCatching { api.playerChoice(itemId) }.getOrNull()
                }
                val votes = choiceD.await()?.items.orEmpty()
                val (config, needsLogin) = resolvePlayerConfig(detail, media)
                var sources = config?.toPlayerSources().orEmpty()
                if (sources.none { it.hasPlayableContent() }) {
                    val legacy = runCatching { api.player(playerType, playerId) }.getOrNull()
                        ?.players
                        .orEmpty()
                        .ifEmpty {
                            detail.idKp?.takeIf { it > 0 }?.let { kp ->
                                runCatching { api.player("film", kp.toString()) }
                                    .getOrNull()
                                    ?.players
                            }.orEmpty()
                        }
                    if (legacy.any { it.hasPlayableContent() }) {
                        sources = legacy
                    }
                }
                val choices = mergePlayerChoices(sources, votes)
                val preferred = votes
                    .sortedByDescending { it.share ?: 0.0 }
                    .mapNotNull { it.source }
                    .firstOrNull { key ->
                        sources.any { s ->
                            s.name.equals(key, ignoreCase = true) && s.hasPlayableContent()
                        }
                    }
                    ?: sources.firstOrNull { it.hasPlayableContent() }?.name
                    ?: sources.firstOrNull()?.name
                    ?: choices.firstOrNull()?.source
                val picked = pickSource(sources, preferred)
                _state.update {
                    it.copy(
                        playerLoading = false,
                        playerChoices = choices,
                        playerSources = sources,
                        selectedPlayerSource = preferred,
                        playerIframeUrl = picked?.iframeUrl?.takeIf { url -> url.isNotBlank() },
                        playerEmbedHtml = picked?.embedHtml?.takeIf { html -> html.isNotBlank() },
                        playerNeedsLogin = needsLogin && picked?.hasPlayableContent() != true,
                    )
                }
            }
        }
    }

    private fun loadSchedule(tmdbId: Int?, kinopoiskId: Int?) {
        if ((tmdbId == null || tmdbId <= 0) && (kinopoiskId == null || kinopoiskId <= 0)) return
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            _state.update { it.copy(scheduleLoading = true) }
            runCatching {
                api.tvSchedule(
                    tmdbId = tmdbId?.takeIf { it > 0 },
                    kinopoiskId = kinopoiskId?.takeIf { it > 0 },
                    release = 1,
                )
            }.onSuccess { schedule ->
                val firstSeason = schedule.seasons.firstOrNull()?.seasonNumber
                _state.update {
                    it.copy(
                        scheduleLoading = false,
                        schedule = schedule,
                        selectedSeason = firstSeason,
                    )
                }
            }.onFailure {
                _state.update { it.copy(scheduleLoading = false, schedule = null) }
            }
        }
    }

    private fun loadLastAir(tmdbId: Int?) {
        val id = tmdbId?.takeIf { it > 0 } ?: return
        lastAirJob?.cancel()
        lastAirJob = viewModelScope.launch {
            runCatching { api.tvLastAir(id) }
                .onSuccess { last -> _state.update { it.copy(lastAir = last) } }
                .onFailure { _state.update { it.copy(lastAir = null) } }
        }
    }

    private fun refreshFavoriteState(idKp: Int?) {
        val kp = idKp?.takeIf { it > 0 } ?: return
        val key = "kp:$kp"
        viewModelScope.launch {
            runCatching { api.sync() }
                .onSuccess { sync ->
                    val found = sync.favorites.any { fav ->
                        fav.id == key || fav.contentId == kp
                    }
                    _state.update { it.copy(isFavorite = found) }
                }
        }
    }

    /**
     * Как на сайте: сначала /api/player-config (с cookie),
     * иначе SSR `initialPlayerConfig` из HTML карточки.
     */
    private suspend fun resolvePlayerConfig(
        detail: TitleDetailDto,
        media: String,
    ): Pair<PlayerConfigDto?, Boolean> {
        val fromApi = runCatching {
            api.playerConfig(
                kinopoiskId = detail.idKp?.takeIf { it > 0 },
                tmdbId = detail.idTmdb?.takeIf { it > 0 },
                mediaType = media,
            )
        }.getOrNull()
        val body = fromApi?.body()
        if (fromApi?.isSuccessful == true && body != null && body.hasPlayableSource()) {
            return body to false
        }

        val kp = detail.idKp?.takeIf { it > 0 } ?: return null to true
        val html = withContext(Dispatchers.IO) {
            fetchTitlePageHtml(kp, media)
        } ?: return null to (fromApi?.code() == 451)
        val fromPage = PlayerConfigPageParser.parse(html)
        if (fromPage != null && fromPage.hasPlayableSource()) {
            return fromPage to false
        }
        return null to PlayerConfigPageParser.needsLogin(html)
    }
}
