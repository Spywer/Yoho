package com.spywer.yoho.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SessionCookieJar
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.toTitle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ContentFilter(val labelRes: String) {
    All("Все"),
    Movie("Фильмы"),
    Tv("Сериалы"),
    Animation("Мультфильмы"),
    Anime("Аниме"),
    ;

    fun trendingNow(): String? = when (this) {
        All -> null
        Movie -> "movie"
        Tv -> "tv"
        Animation, Anime -> "animation"
    }

    fun matchesMedia(mediaType: String?): Boolean {
        val m = mediaType?.lowercase().orEmpty()
        return when (this) {
            All -> true
            Movie -> m == "movie" || m == "film"
            Tv -> m == "tv" || m == "series"
            Animation -> m.contains("anim") || m == "cartoon"
            Anime -> m.contains("anime")
        }
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
    val filteredRecommendations: List<TitleDto>
        get() = recommendations.filter { filter.matchesMedia(it.mediaType) }

    val hasContent: Boolean
        get() = recommendations.isNotEmpty() || nowPopular.isNotEmpty() ||
            trailers.isNotEmpty() || popular.isNotEmpty() || newcomers.isNotEmpty()
}

class HomeViewModel : ViewModel() {
    private val api = NetworkModule.api
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

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
        viewModelScope.launch {
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
                    val popD = async { api.trending("popular") }
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
                _state.update {
                    it.copy(
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
                _state.update {
                    it.copy(
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
        // Фильтр только клиентский для рекомендаций; тренды перегружаем.
        _state.update { it.copy(filter = filter) }
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
