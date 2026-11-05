package com.spywer.yoho.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SessionCookieJar
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.toTitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ContentFilter(val labelRes: String) {
    All("Все"),
    Movie("Фильмы"),
    Tv("Сериалы"),
    Animation("Мультфильмы"),
    Anime("Аниме"),
    ;

    /** Как на сайте: / → all, /film → movie, /series → tv, /animation, /anime. */
    fun trendingNow(): String = when (this) {
        All -> "all"
        Movie -> "movie"
        Tv -> "tv"
        Animation -> "animation"
        Anime -> "anime"
    }

    /** Блок «Популярные»: popular / popular_movie / popular_tv / … */
    fun trendingPopular(): String = when (this) {
        All -> "popular"
        Movie -> "popular_movie"
        Tv -> "popular_tv"
        Animation -> "popular_animation"
        Anime -> "popular_anime"
    }
}

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val filter: ContentFilter = ContentFilter.All,
    val recommendations: List<TitleDto> = emptyList(),
    val nowPopular: List<TitleDto> = emptyList(),
    val trailers: List<TitleDto> = emptyList(),
    val popular: List<TitleDto> = emptyList(),
    val newcomers: List<TitleDto> = emptyList(),
) {
    val hasContent: Boolean
        get() = recommendations.isNotEmpty() || nowPopular.isNotEmpty() ||
            trailers.isNotEmpty() || popular.isNotEmpty() || newcomers.isNotEmpty()
}

class HomeViewModel : ViewModel() {
    private val api = NetworkModule.api
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
        // Главная грузится до логина — после входа дотягиваем рекомендации.
        viewModelScope.launch {
            SessionRepository.state
                .map { it.isLoggedIn }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    if (loggedIn) {
                        loadRecommendations()
                    } else {
                        _state.update { it.copy(recommendations = emptyList()) }
                    }
                }
        }
    }

    fun load(refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val filter = _state.value.filter
            _state.update {
                it.copy(
                    loading = !refresh && !it.hasContent,
                    refreshing = refresh,
                    error = null,
                )
            }
            runCatching {
                coroutineScope {
                    val recD = async { fetchRecommendations() }
                    val nowD = async { api.trending(filter.trendingNow()) }
                    val popD = async { api.trending(filter.trendingPopular()) }
                    val trailD = async { api.latestTrailers() }
                    val recentD = async { api.searchRecent() }
                    val now = nowD.await()
                    val pop = popD.await()
                    val trail = trailD.await()
                    val recent = recentD.await()
                    HomeSections(
                        recommendations = recD.await(),
                        nowPopular = now.items.map { it.toTitle(now.ratings) },
                        popular = pop.items.map { it.toTitle(pop.ratings) },
                        trailers = trail.items.map { it.toTitle() },
                        newcomers = recent.items.map { it.toTitle() },
                    )
                }
            }.onSuccess { sections ->
                if (!isActive) return@onSuccess
                _state.update { cur ->
                    // Не затираем данные устаревшим ответом после быстрой смены фильтра.
                    if (cur.filter != filter) return@update cur
                    cur.copy(
                        loading = false,
                        refreshing = false,
                        error = null,
                        recommendations = sections.recommendations,
                        nowPopular = sections.nowPopular,
                        popular = sections.popular,
                        trailers = sections.trailers,
                        newcomers = sections.newcomers,
                    )
                }
            }.onFailure { e ->
                if (!isActive) return@onFailure
                _state.update { cur ->
                    if (cur.filter != filter) return@update cur
                    cur.copy(
                        loading = false,
                        refreshing = false,
                        error = e.message ?: "Ошибка загрузки",
                    )
                }
            }
        }
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            val items = fetchRecommendations()
            _state.update { it.copy(recommendations = items) }
        }
    }

    private suspend fun fetchRecommendations(): List<TitleDto> {
        if (!SessionRepository.state.value.isLoggedIn && !SessionCookieJar.get().hasSessionCookie()) {
            return emptyList()
        }
        return runCatching {
            val resp = api.recommendationsLatest()
            if (!resp.isSuccessful) return@runCatching emptyList()
            resp.body()?.items.orEmpty().map { it.toTitle().copy(reason = null) }
        }.getOrElse { emptyList() }
    }

    fun setFilter(filter: ContentFilter) {
        if (_state.value.filter == filter) return
        // Сразу убираем чужой контент — иначе «Сейчас популярно»/«Популярные» выглядят как зависшие.
        _state.update {
            it.copy(
                filter = filter,
                loading = true,
                error = null,
                nowPopular = emptyList(),
                popular = emptyList(),
            )
        }
        load()
    }

    private data class HomeSections(
        val recommendations: List<TitleDto>,
        val nowPopular: List<TitleDto>,
        val popular: List<TitleDto>,
        val trailers: List<TitleDto>,
        val newcomers: List<TitleDto>,
    )
}
