package com.spywer.yoho.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.DiaryEntryDto
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.PostersResolveItem
import com.spywer.yoho.data.remote.PostersResolveRequest
import com.spywer.yoho.data.remote.ProfileFilmDto
import com.spywer.yoho.data.remote.ProfileFilmsResponseDto
import com.spywer.yoho.data.remote.RatingsDto
import com.spywer.yoho.data.remote.ScoreDto
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.TitleNameDto
import com.spywer.yoho.data.remote.YohoGson
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Карточка хаба: film + кэш TitleDto (без toTitleDto на каждом рекомпозите). */
data class HubFilmCard(
    val key: String,
    val film: ProfileFilmDto,
    val title: TitleDto,
)

data class CollectionHubUiState(
    val loading: Boolean = true,
    val cards: List<HubFilmCard> = emptyList(),
    val needsLogin: Boolean = false,
) {
    val filmsEmpty: Boolean get() = cards.isEmpty()
}

/**
 * Сетка «Мои фильмы и сериалы» как /{username}/films:
 * дневник ∪ избранное, свежие сверху.
 */
class CollectionHubViewModel : ViewModel() {
    private val api = NetworkModule.api
    private val gson = YohoGson.instance
    private val _state = MutableStateFlow(CollectionHubUiState())
    val state: StateFlow<CollectionHubUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            var wasLoggedIn = SessionRepository.state.value.isLoggedIn
            SessionRepository.state.map { it.isLoggedIn }.collect { isLoggedIn ->
                if (isLoggedIn && !wasLoggedIn) {
                    // Вошли — подгружаем данные
                    load()
                } else if (!isLoggedIn && wasLoggedIn) {
                    // Выйшли — показываем «нужен логин»
                    _state.update {
                        it.copy(
                            loading = false,
                            cards = emptyList(),
                            needsLogin = true,
                        )
                    }
                }
                wasLoggedIn = isLoggedIn
            }
        }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            if (!SessionRepository.state.value.isLoggedIn) {
                SessionRepository.refresh()
            }
            val user = SessionRepository.state.value.user
            if (user == null) {
                _state.update {
                    it.copy(loading = false, cards = emptyList(), needsLogin = true)
                }
                return@launch
            }

            // Сначала профильный API; при пустом/404 — diary∪favorites
            val username = user.username?.takeIf { it.isNotBlank() }
            val fromProfile = username?.let { loadProfileFilms(it) }.orEmpty()
            val films = if (fromProfile.isNotEmpty()) {
                fromProfile
            } else {
                coroutineScope {
                    val diaryJob = async {
                        runCatching { api.diaryList(page = 1, limit = 100).entries }
                            .getOrDefault(emptyList())
                    }
                    val syncJob = async {
                        runCatching { api.sync() }.getOrNull()
                    }
                    val merged = mergeFilmsLibrary(
                        diary = diaryJob.await(),
                        favorites = syncJob.await()?.favorites.orEmpty(),
                    )
                    enrichPosters(merged)
                }
            }

            _state.update {
                it.copy(
                    loading = false,
                    cards = films.toHubCards(),
                    needsLogin = false,
                )
            }
        }
    }

    private suspend fun loadProfileFilms(username: String): List<ProfileFilmDto> {
        val resp = runCatching { api.profileFilms(username) }.getOrNull() ?: return emptyList()
        val body = resp.body() ?: return emptyList()
        if (!resp.isSuccessful) {
            body.close()
            return emptyList()
        }
        val raw = runCatching { body.string() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        val list = runCatching {
            val trimmed = raw.trimStart()
            if (trimmed.startsWith("[")) {
                val type = object : TypeToken<List<ProfileFilmDto>>() {}.type
                gson.fromJson<List<ProfileFilmDto>>(raw, type).orEmpty()
            } else {
                gson.fromJson(raw, ProfileFilmsResponseDto::class.java)?.resolved().orEmpty()
            }
        }.getOrDefault(emptyList())
        return list.sortedByDescending { it.sortMillis() }
    }

    /** Как на сайте: дневник ∪ избранное. */
    private fun mergeFilmsLibrary(
        diary: List<DiaryEntryDto>,
        favorites: List<FavoriteItemDto>,
    ): List<ProfileFilmDto> {
        val byId = linkedMapOf<String, ProfileFilmDto>()
        for (entry in diary) {
            val id = entry.itemId?.takeIf { it.isNotBlank() }
                ?: entry.resolvedKp()?.let { "kp:$it" }
                ?: continue
            byId[id] = entry.toProfileFilm(id)
        }
        for (fav in favorites) {
            val id = fav.id?.takeIf { it.isNotBlank() }
                ?: fav.contentId?.let { "kp:$it" }
                ?: continue
            val existing = byId[id]
            if (existing == null) {
                byId[id] = fav.toProfileFilm(id)
            } else if (existing.posterUrl.isNullOrBlank() && !fav.posterUrl.isNullOrBlank()) {
                byId[id] = existing.copy(posterUrl = fav.posterUrl)
            }
        }
        return byId.values.sortedByDescending { it.sortMillis() }
    }

    private suspend fun enrichPosters(films: List<ProfileFilmDto>): List<ProfileFilmDto> {
        val need = films.filter { it.posterUrl.isNullOrBlank() && !it.itemId.isNullOrBlank() }
        if (need.isEmpty()) return films
        val resp = runCatching {
            api.resolvePosters(
                PostersResolveRequest(
                    items = need.mapNotNull { film ->
                        val id = film.itemId ?: return@mapNotNull null
                        PostersResolveItem(
                            id = id,
                            href = film.href,
                            title = film.title,
                            year = film.year,
                            mediaType = film.mediaType,
                            tmdbId = film.tmdbId,
                        )
                    },
                ),
            )
        }.getOrNull() ?: return films
        if (!resp.isSuccessful) return films
        val posters = resp.body()?.posters.orEmpty()
        if (posters.isEmpty()) return films
        return films.map { film ->
            val id = film.itemId ?: return@map film
            if (!film.posterUrl.isNullOrBlank()) return@map film
            val resolved = posters[id] ?: return@map film
            film.copy(posterUrl = resolved)
        }
    }
}

private fun DiaryEntryDto.toProfileFilm(id: String): ProfileFilmDto =
    ProfileFilmDto(
        itemId = id,
        href = href,
        title = title,
        year = year,
        posterUrl = posterUrl,
        mediaType = mediaType,
        tmdbId = tmdbId,
        kp = kpRating,
        imdb = imdbRating,
        status = statusLabel,
        watchedAt = watchedAt ?: updatedAt ?: createdAt,
    )

private fun FavoriteItemDto.toProfileFilm(id: String): ProfileFilmDto =
    ProfileFilmDto(
        itemId = id,
        href = href,
        title = title,
        year = year,
        posterUrl = posterUrl,
        mediaType = mediaType ?: contentType,
        tmdbId = tmdbTvId,
        watchedAt = ts?.let { runCatching { Instant.ofEpochMilli(it).toString() }.getOrNull() },
    )

private fun ProfileFilmDto.sortMillis(): Long {
    val iso = watchedAt?.takeIf { it.isNotBlank() } ?: return 0L
    return runCatching { Instant.parse(iso).toEpochMilli() }.getOrDefault(0L)
}

private fun List<ProfileFilmDto>.toHubCards(): List<HubFilmCard> =
    map { film ->
        HubFilmCard(
            key = film.itemId ?: film.href ?: film.title.orEmpty(),
            film = film,
            title = film.toTitleDto(),
        )
    }

private fun ProfileFilmDto.toTitleDto(): TitleDto =
    TitleDto(
        idKp = resolvedKp(),
        idTmdb = tmdbId,
        mediaType = mediaType,
        title = TitleNameDto(ru = title),
        year = year?.toIntOrNull(),
        poster = resolvedPoster(),
        ratings = RatingsDto(
            kinopoisk = kp?.takeIf { it > 0 }?.let { ScoreDto(score = it) },
            imdb = imdb?.takeIf { it > 0 }?.let { ScoreDto(score = it) },
        ),
        reason = status?.takeIf { it.isNotBlank() },
    )
