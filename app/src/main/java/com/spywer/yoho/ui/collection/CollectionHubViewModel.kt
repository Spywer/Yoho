package com.spywer.yoho.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.DiaryEntryDto
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.PostersResolveRequest
import com.spywer.yoho.data.remote.PostersResolveItem
import com.spywer.yoho.data.remote.ProfileFilmDto
import com.spywer.yoho.data.remote.ProfileFilmsResponseDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class CollectionHubUiState(
    val loading: Boolean = true,
    val films: List<ProfileFilmDto> = emptyList(),
    val needsLogin: Boolean = false,
)

/**
 * Сетка «Фильмы» как на /{username}/films:
 * дневник ∪ избранное (не копия одного дневника), свежие сверху.
 */
class CollectionHubViewModel : ViewModel() {
    private val api = NetworkModule.api
    private val gson = Gson()
    private val _state = MutableStateFlow(CollectionHubUiState())
    val state: StateFlow<CollectionHubUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            if (!SessionRepository.state.value.isLoggedIn) {
                SessionRepository.refresh()
            }
            val user = SessionRepository.state.value.user
            if (user == null) {
                _state.update {
                    it.copy(loading = false, films = emptyList(), needsLogin = true)
                }
                return@launch
            }

            val films = coroutineScope {
                val profileJob = async {
                    user.username?.takeIf { it.isNotBlank() }?.let { loadProfileFilms(it) }
                        .orEmpty()
                }
                val diaryJob = async {
                    runCatching { api.diaryList(page = 1, limit = 100).entries }
                        .getOrDefault(emptyList())
                }
                val syncJob = async {
                    runCatching { api.sync() }.getOrNull()
                }

                val fromProfile = profileJob.await()
                if (fromProfile.isNotEmpty()) return@coroutineScope fromProfile

                val merged = mergeFilmsLibrary(
                    diary = diaryJob.await(),
                    favorites = syncJob.await()?.favorites.orEmpty(),
                )
                enrichPosters(merged)
            }

            _state.update {
                it.copy(loading = false, films = films, needsLogin = false)
            }
        }
    }

    private suspend fun loadProfileFilms(username: String): List<ProfileFilmDto> {
        val resp = runCatching { api.profileFilms(username) }.getOrNull() ?: return emptyList()
        if (!resp.isSuccessful) return emptyList()
        val raw = resp.body()?.string()?.takeIf { it.isNotBlank() } ?: return emptyList()
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
        val need = films.filter { it.posterUrl.isNullOrBlank() && it.itemId != null }
        if (need.isEmpty()) return films
        val resp = runCatching {
            api.resolvePosters(
                PostersResolveRequest(
                    items = need.map {
                        PostersResolveItem(
                            id = it.itemId!!,
                            href = it.href,
                            title = it.title,
                            year = it.year,
                            mediaType = it.mediaType,
                            tmdbId = it.tmdbId,
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
            val resolved = posters[id] ?: return@map film
            if (!film.posterUrl.isNullOrBlank()) film else film.copy(posterUrl = resolved)
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
