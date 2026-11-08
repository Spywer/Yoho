package com.spywer.yoho.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SearchFacetsDto
import com.spywer.yoho.data.remote.SearchFiltersCatalogDto
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.toTitle

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val filters: SearchFilterState = SearchFilterState(),
    val facets: SearchFacetsDto = SearchFacetsDto(),
    val filterCatalog: SearchFiltersCatalogDto = SearchFiltersCatalogDto(),
    val results: List<TitleDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

class SearchViewModel : ViewModel() {
    private val api = NetworkModule.api
    private val _state = MutableStateFlow(SearchUiState(loading = true))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadFacets()
        scheduleSearch(immediate = true)
    }

    private var initialApplied = false

    fun setInitial(
        query: String? = null,
        genre: String? = null,
        type: String? = null,
        actor: String? = null,
        actorKp: String? = null,
    ) {
        searchJob?.cancel()
        val snapshot = _state.value
        val filtersSet = snapshot.filters.actors.isNotEmpty() || snapshot.filters.genres.isNotEmpty() || snapshot.filters.types.isNotEmpty()
        if (filtersSet) return
        val genreSlug = genre?.let { genreSlugOf(it) ?: it.trim().takeIf { s -> s.isNotEmpty() } }
        val typeVal = type?.trim()?.takeIf {
            it.isNotEmpty() && it != "all" && DefaultSearchTypes.any { t -> t.value == it }
        }
        val actorRef = actorKp?.toIntOrNull()?.let { kp ->
            val name = actor?.trim()?.takeIf { it.isNotEmpty() }
            PersonFilterRef(name ?: "", kp)
        }
        val hasInitial = genreSlug != null || typeVal != null || actorRef != null
        if (!hasInitial && query.isNullOrBlank()) return
        _state.update { s ->
            s.copy(
                query = query?.trim().orEmpty().ifBlank { s.query },
                filters = s.filters.copy(
                    genres = if (genreSlug != null) setOf(genreSlug) else s.filters.genres,
                    types = if (typeVal != null) setOf(typeVal) else s.filters.types,
                    actors = if (actorRef != null) listOf(actorRef) else s.filters.actors,
                ),
            )
        }
        if (!query.isNullOrBlank()) {
            onQueryChange(query.trim())
        } else {
            scheduleSearch(immediate = true)
        }
    }

    private fun loadFacets() {
        viewModelScope.launch {
            coroutineScope {
                val facetsD = async { runCatching { api.searchFacets() }.getOrNull() }
                val catalogD = async { runCatching { api.searchFiltersCatalog() }.getOrNull() }
                val facets = facetsD.await()
                val catalog = catalogD.await()
                _state.update {
                    it.copy(
                        facets = (facets ?: it.facets).withFallbackTypes(),
                        filterCatalog = catalog ?: it.filterCatalog,
                    )
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        scheduleSearch()
    }

    fun clearQuery() {
        _state.update { it.copy(query = "") }
        scheduleSearch()
    }

    fun updateFilters(filters: SearchFilterState) {
        _state.update { it.copy(filters = filters) }
        scheduleSearch()
    }

    fun toggleType(type: String) {
        _state.update { it.copy(filters = it.filters.toggleType(type)) }
        scheduleSearch()
    }

    fun clearFilters() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                query = "",
                filters = SearchFilterState(),
                results = emptyList(),
                total = 0,
                page = 1,
                hasMore = false,
                loading = false,
                loadingMore = false,
                error = null,
            )
        }
        scheduleSearch()
    }

    fun removeChip(id: String) {
        _state.update { it.copy(filters = it.filters.removeChip(id)) }
        scheduleSearch()
    }

    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.loading || s.loadingMore) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runDiscover(page = s.page + 1, append = true)
        }
    }

    fun refresh() {
        scheduleSearch(immediate = true)
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(280)
            runDiscover(page = 1, append = false)
        }
    }

    private suspend fun runDiscover(page: Int, append: Boolean) {
        val snapshot = _state.value
        _state.update {
            it.copy(
                loading = !append,
                loadingMore = append,
                error = if (append) it.error else null,
            )
        }
        val f = snapshot.filters
        // Сайт: и строка поиска, и фильтр «Название» уходят в q.
        val qText = snapshot.query.trim().ifBlank { null }
            ?: f.titleContains?.trim()?.ifBlank { null }
        runCatching {
            api.discover(
                source = "catalog",
                query = qText,
                type = discoverTypeParam(f.types),
                genres = f.genres.takeIf { it.isNotEmpty() }?.joinToString(","),
                country = f.countries.takeIf { it.isNotEmpty() }?.joinToString(","),
                region = f.region,
                language = f.language,
                yearFrom = f.year.from,
                yearTo = f.year.to,
                runtimeFrom = f.duration.from,
                runtimeTo = f.duration.to,
                ratingKpFrom = f.ratingKp.from,
                ratingKpTo = f.ratingKp.to,
                ratingImdbFrom = f.ratingImdb.from,
                ratingImdbTo = f.ratingImdb.to,
                ratingTmdbFrom = f.ratingTmdb.from,
                ratingTmdbTo = f.ratingTmdb.to,
                ratingRtFrom = f.ratingRt.from,
                ratingRtTo = f.ratingRt.to,
                ratingMetacriticFrom = f.ratingMc.from,
                ratingMetacriticTo = f.ratingMc.to,
                ratingCriticsFrom = f.ratingCritics.from,
                ratingCriticsTo = f.ratingCritics.to,
                votesKpFrom = f.votesKp.from,
                votesKpTo = f.votesKp.to,
                votesImdbFrom = f.votesImdb.from,
                votesImdbTo = f.votesImdb.to,
                votesTmdbFrom = f.votesTmdb.from,
                votesTmdbTo = f.votesTmdb.to,
                oscarsWonFrom = f.oscars.from,
                oscarsWonTo = f.oscars.to,
                awardsWonFrom = f.awardsTotal.from,
                awardsWonTo = f.awardsTotal.to,
                budgetFrom = f.budget.from,
                budgetTo = f.budget.to,
                revenueFrom = f.boxOfficeWorld.from,
                revenueTo = f.boxOfficeWorld.to,
                boxOfficeFrom = f.boxOfficeUs.from,
                boxOfficeTo = f.boxOfficeUs.to,
                seasonsTotalFrom = f.seasons.from,
                seasonsTotalTo = f.seasons.to,
                episodesTotalFrom = f.episodes.from,
                episodesTotalTo = f.episodes.to,
                startYearFrom = f.airStart.from,
                startYearTo = f.airStart.to,
                endYearFrom = f.airEnd.from,
                endYearTo = f.airEnd.to,
                premiereWorldFrom = f.premiereWorldFrom,
                premiereWorldTo = f.premiereWorldTo,
                premiereRuFrom = f.premiereRuFrom,
                premiereRuTo = f.premiereRuTo,
                digitalReleaseFrom = f.digitalFrom,
                digitalReleaseTo = f.digitalTo,
                released = f.releaseStatus?.takeIf { it != "released" },
                sort = f.sort.takeIf { it != DefaultSort },
                actor = f.actors.takeIf { it.isNotEmpty() }?.map { it.name },
                actorKp = f.actors.takeIf { it.isNotEmpty() }?.map { it.kpId },
                crew = f.crew.firstOrNull()?.name,
                crewKp = f.crew.firstOrNull()?.kpId,
                crewRole = f.crewRole?.takeIf { role ->
                    f.crew.isNotEmpty() && !role.equals("director", ignoreCase = true)
                },
                notPerson = f.excludePeople.takeIf { it.isNotEmpty() }?.map { it.name },
                notPersonKp = f.excludePeople.takeIf { it.isNotEmpty() }?.map { it.kpId },
                tag = f.tags.takeIf { it.isNotEmpty() }?.toList(),
                company = f.companies.takeIf { it.isNotEmpty() }?.toList(),
                platform = f.platforms.takeIf { it.isNotEmpty() }?.joinToString(","),
                age = f.age,
                preset = f.preset?.takeIf { it != "none" },
                collection = f.collection?.takeIf { it != "all" },
                onlyWithPlayer = f.onlyWithPlayer.takeIf { it },
                excludeWatched = f.excludeWatched.takeIf { it },
                page = page,
            )
        }.onSuccess { resp ->
            val pageList = resp.list().map { it.toTitle() }
            val hasMore = resp.hasMore == true ||
                ((resp.totalPages ?: 1) > (resp.page ?: page))
            _state.update {
                val merged = if (append) {
                    (it.results + pageList).distinctBy { item ->
                        item.idKp ?: item.idTmdb ?: item.hashCode()
                    }
                } else {
                    pageList
                }
                it.copy(
                    loading = false,
                    loadingMore = false,
                    page = resp.page ?: page,
                    hasMore = hasMore,
                    total = if (append) maxOf(it.total, merged.size) else resp.resolvedTotal(),
                    results = merged,
                    error = null,
                )
            }
        }.onFailure { e ->
            // Отмена debounce/нового запроса — не ошибка UI.
            if (e is CancellationException) throw e
            _state.update {
                it.copy(
                    loading = false,
                    loadingMore = false,
                    results = if (append) it.results else emptyList(),
                    total = if (append) it.total else 0,
                    hasMore = if (append) it.hasMore else false,
                    error = e.message ?: "Ошибка поиска",
                )
            }
        }
    }

    private fun discoverTypeParam(uiTypes: Set<String>): String? {
        if (uiTypes.isEmpty()) return null
        return uiTypes.mapNotNull { ui ->
            when (ui) {
                "movie" -> "movie"
                "tv" -> "series,tv_show,mini_series"
                "anime" -> "anime"
                "animation" -> "cartoon"
                else -> null
            }
        }.joinToString(",").ifBlank { null }
    }

}
