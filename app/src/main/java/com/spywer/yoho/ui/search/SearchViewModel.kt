package com.spywer.yoho.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SearchFacetsDto
import com.spywer.yoho.data.remote.SearchFiltersCatalogDto
import com.spywer.yoho.data.remote.SuggestItemDto
import com.spywer.yoho.data.remote.TitleDto
import com.spywer.yoho.data.remote.toTitle
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
    val suggestions: List<SuggestItemDto> = emptyList(),
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

    private var suggestJob: Job? = null
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
    ) {
        if (initialApplied) return
        initialApplied = true
        val genreSlug = genre?.let { genreSlugOf(it) ?: it.trim().takeIf { s -> s.isNotEmpty() } }
        val typeVal = type?.trim()?.takeIf {
            it.isNotEmpty() && it != "all" && DefaultSearchTypes.any { t -> t.value == it }
        }
        if (genreSlug == null && typeVal == null && query.isNullOrBlank()) return
        _state.update { s ->
            s.copy(
                query = query?.trim().orEmpty().ifBlank { s.query },
                filters = s.filters.copy(
                    genres = if (genreSlug != null) setOf(genreSlug) else s.filters.genres,
                    types = if (typeVal != null) setOf(typeVal) else s.filters.types,
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
        suggestJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(suggestions = emptyList()) }
            scheduleSearch()
            return
        }
        suggestJob = viewModelScope.launch {
            delay(250)
            runCatching { api.suggest(query.trim()) }
                .onSuccess { list -> _state.update { it.copy(suggestions = list) } }
                .onFailure { _state.update { it.copy(suggestions = emptyList()) } }
        }
        scheduleSearch()
    }

    fun applySuggestion(title: String) {
        suggestJob?.cancel()
        _state.update { it.copy(query = title, suggestions = emptyList()) }
        scheduleSearch()
    }

    fun clearQuery() {
        suggestJob?.cancel()
        _state.update { it.copy(query = "", suggestions = emptyList()) }
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
            )
        }
        val f = snapshot.filters
        val qText = snapshot.query.trim().ifBlank { null }
        runCatching {
            api.discover(
                source = discoverSource(f),
                query = qText,
                type = discoverTypeParam(f.types),
                genres = f.genres.takeIf { it.isNotEmpty() }?.joinToString(","),
                country = f.countries.takeIf { it.isNotEmpty() }?.joinToString(","),
                region = f.region,
                yearFrom = f.year.from,
                yearTo = f.year.to,
                ratingKpMin = f.ratingKp.from,
                ratingKpMax = f.ratingKp.to,
                ratingImdbMin = f.ratingImdb.from,
                ratingImdbMax = f.ratingImdb.to,
                sort = f.sort.takeIf { it != "popularity" },
                language = f.language,
                actors = f.actors.takeIf { it.isNotEmpty() }?.joinToString(","),
                crew = f.crew.takeIf { it.isNotEmpty() }?.joinToString(","),
                crewRole = f.crewRole,
                excludePeople = f.excludePeople.takeIf { it.isNotEmpty() }?.joinToString(","),
                tags = f.tags.takeIf { it.isNotEmpty() }?.joinToString(","),
                age = f.age,
                durationMin = f.duration.from,
                durationMax = f.duration.to,
                votesKpMin = f.votesKp.from,
                votesKpMax = f.votesKp.to,
                companies = f.companies.takeIf { it.isNotEmpty() }?.joinToString(","),
                platforms = f.platforms.takeIf { it.isNotEmpty() }?.joinToString(","),
                preset = f.preset,
                collection = f.collection,
                titleContains = f.titleContains,
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

    private fun discoverSource(f: SearchFilterState): String = when {
        f.preset == "popular" || f.collection == "popular" -> "popular"
        f.preset == "trending" || f.collection == "trending" -> "trending"
        else -> "catalog"
    }
}
