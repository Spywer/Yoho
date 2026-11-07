package com.spywer.yoho.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.DiaryEntryDto
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.MeActivityRequest
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.absMediaUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CollectionShelf {
    Favorites,
    History,
    Diary,
}

enum class FavoritesSort {
    /** Сначала новые (ts по убыванию). */
    DateDesc,
    /** Сначала старые (ts по возрастанию). */
    DateAsc,
}

/** Новые сверху при DateDesc; записи без ts — в конец. */
private fun sortByDate(
    items: List<FavoriteItemDto>,
    sort: FavoritesSort,
): List<FavoriteItemDto> =
    when (sort) {
        FavoritesSort.DateDesc -> items.sortedByDescending { it.ts ?: Long.MIN_VALUE }
        FavoritesSort.DateAsc -> items.sortedBy { it.ts ?: Long.MAX_VALUE }
    }

enum class DiarySort {
    AddedDesc,
    AddedAsc,
    WatchedDesc,
    WatchedAsc,
}

/** Статусы дневника как на сайте (выпадающий список). */
val DiaryStatusLabels = listOf(
    "Просмотрено",
    "Смотрю",
    "Хочу посмотреть",
    "Пересматриваю",
    "Отложено",
    "Бросил",
)

/** Служебные ключи фильтра по реакции (не statusLabel). */
const val DiaryFilterLike = "__like__"
const val DiaryFilterDislike = "__dislike__"

data class CollectionUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val mutating: Boolean = false,
    val shelf: CollectionShelf = CollectionShelf.Favorites,
    val favorites: List<FavoriteItemDto> = emptyList(),
    val history: List<FavoriteItemDto> = emptyList(),
    val diary: List<DiaryEntryDto> = emptyList(),
    val favoritesSort: FavoritesSort = FavoritesSort.DateDesc,
    val historySort: FavoritesSort = FavoritesSort.DateDesc,
    val diarySort: DiarySort = DiarySort.AddedDesc,
    /** null — все статусы; иначе значение из [DiaryStatusLabels]. */
    val diaryStatusFilter: String? = null,
    /**
     * null — все оценки;
     * [DiaryFilterLike] / [DiaryFilterDislike] — по sourceReaction.
     * Независим от [diaryStatusFilter] (лайк и статус могут быть вместе).
     */
    val diaryReactionFilter: String? = null,
    /** Кэш сортировки/фильтра — пересчёт только при смене данных/сорта. */
    val displayedFavorites: List<FavoriteItemDto> = emptyList(),
    val displayedHistory: List<FavoriteItemDto> = emptyList(),
    val displayedDiary: List<DiaryEntryDto> = emptyList(),
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null,
    val actionError: String? = null,
    val needsLogin: Boolean = false,
)

/** Пересчитать displayed* из сырых списков + sort/filter. */
private fun CollectionUiState.withDisplayed(): CollectionUiState {
    var filteredDiary = diary
    diaryStatusFilter?.let { status ->
        filteredDiary = filteredDiary.filter { it.statusLabel == status }
    }
    when (diaryReactionFilter) {
        DiaryFilterLike ->
            filteredDiary = filteredDiary.filter { it.sourceReaction == "like" }
        DiaryFilterDislike ->
            filteredDiary = filteredDiary.filter { it.sourceReaction == "dislike" }
    }
    val sortedDiary = when (diarySort) {
        DiarySort.AddedDesc ->
            filteredDiary.sortedByDescending { it.addedMillis() }
        DiarySort.AddedAsc ->
            filteredDiary.sortedBy { it.addedMillis().takeIf { ms -> ms > 0L } ?: Long.MAX_VALUE }
        DiarySort.WatchedDesc ->
            filteredDiary.sortedWith(
                compareByDescending<DiaryEntryDto> { it.watchedMillis() }
                    .thenByDescending { it.addedMillis() },
            )
        DiarySort.WatchedAsc ->
            filteredDiary.sortedWith(
                compareBy<DiaryEntryDto> {
                    it.watchedMillis().takeIf { ms -> ms > 0L } ?: Long.MAX_VALUE
                }.thenBy { it.addedMillis().takeIf { ms -> ms > 0L } ?: Long.MAX_VALUE },
            )
    }
    return copy(
        displayedFavorites = sortByDate(favorites, favoritesSort),
        displayedHistory = sortByDate(history, historySort),
        displayedDiary = sortedDiary,
    )
}

class CollectionViewModel(
    initialShelf: CollectionShelf = CollectionShelf.Favorites,
) : ViewModel() {
    private val api = NetworkModule.api
    private val _state = MutableStateFlow(CollectionUiState(shelf = initialShelf))
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    companion object {
        fun factory(shelf: CollectionShelf): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(CollectionViewModel::class.java))
                    return CollectionViewModel(shelf) as T
                }
            }
    }

    /** Обновить state и пересчитать displayed*. */
    private inline fun updateDerived(transform: (CollectionUiState) -> CollectionUiState) {
        _state.update { transform(it).withDisplayed() }
    }

    fun setFavoritesSort(sort: FavoritesSort) {
        updateDerived { it.copy(favoritesSort = sort) }
    }

    fun setHistorySort(sort: FavoritesSort) {
        updateDerived { it.copy(historySort = sort) }
    }

    fun setDiarySort(sort: DiarySort) {
        updateDerived { it.copy(diarySort = sort) }
    }

    fun setDiaryStatusFilter(status: String?) {
        val next = status?.takeIf { it in DiaryStatusLabels }
        updateDerived { it.copy(diaryStatusFilter = next) }
    }

    fun setDiaryReactionFilter(reaction: String?) {
        val next = when (reaction) {
            null -> null
            DiaryFilterLike, DiaryFilterDislike -> reaction
            else -> null
        }
        updateDerived { it.copy(diaryReactionFilter = next) }
    }

    fun startSelectionMode() {
        _state.update {
            it.copy(selectionMode = true, actionError = null)
        }
    }

    fun enterSelection(id: String) {
        _state.update {
            it.copy(
                selectionMode = true,
                selectedIds = it.selectedIds + id,
                actionError = null,
            )
        }
    }

    fun toggleSelection(id: String) {
        _state.update { st ->
            val next = if (id in st.selectedIds) st.selectedIds - id else st.selectedIds + id
            st.copy(
                selectionMode = true,
                selectedIds = next,
            )
        }
    }

    fun selectAllVisible() {
        val st = _state.value
        val ids = when (st.shelf) {
            CollectionShelf.Favorites -> st.displayedFavorites.map { it.selectionKey() }
            CollectionShelf.Diary -> st.displayedDiary.map { it.selectionKey() }
            CollectionShelf.History -> emptyList()
        }
        _state.update {
            it.copy(selectionMode = true, selectedIds = ids.toSet())
        }
    }

    fun clearSelection() {
        _state.update {
            it.copy(selectionMode = false, selectedIds = emptySet(), actionError = null)
        }
    }

    fun consumeActionError() {
        _state.update { it.copy(actionError = null) }
    }

    /** Сменить statusLabel записи дневника (POST /api/diary). */
    fun changeDiaryStatus(entryKey: String, statusLabel: String) {
        if (statusLabel !in DiaryStatusLabels) return
        if (_state.value.mutating) return
        val entry = _state.value.diary.find { it.selectionKey() == entryKey } ?: return
        val body = entry.toDiaryRequest(statusLabel) ?: return
        viewModelScope.launch {
            _state.update { it.copy(mutating = true, actionError = null) }
            val ok = runCatching {
                val resp = api.diary(body)
                resp.isSuccessful
            }.getOrDefault(false)
            if (!ok) {
                _state.update {
                    it.copy(mutating = false, actionError = "Не удалось сменить статус")
                }
            } else {
                updateDerived { prev ->
                    prev.copy(
                        mutating = false,
                        diary = prev.diary.map { item ->
                            if (item.selectionKey() == entryKey) {
                                // Не затираем like/dislike — они живут отдельно от statusLabel
                                item.copy(statusLabel = statusLabel)
                            } else {
                                item
                            }
                        },
                    )
                }
            }
        }
    }

    /**
     * Убрать только statusLabel. Если есть like/dislike — запись оставляем;
     * иначе удаляем из дневника целиком.
     */
    fun clearDiaryStatus(entryKey: String) {
        if (_state.value.mutating) return
        val entry = _state.value.diary.find { it.selectionKey() == entryKey } ?: return
        if (entry.statusLabel.isNullOrBlank()) return
        if (!entry.hasLikeOrDislike()) {
            deleteDiary(setOf(entryKey))
            return
        }
        val currentStatus = entry.statusLabel ?: return
        // Пустой statusLabel + прежняя реакция: сброс статуса, лайк/дизлайк остаётся
        val request = entry.toDiaryRequest(currentStatus)?.copy(statusLabel = "") ?: return
        viewModelScope.launch {
            _state.update { it.copy(mutating = true, actionError = null) }
            val ok = runCatching {
                api.diary(request).isSuccessful
            }.getOrDefault(false)
            if (!ok) {
                _state.update {
                    it.copy(mutating = false, actionError = "Не удалось удалить статус")
                }
            } else {
                updateDerived { prev ->
                    prev.copy(
                        mutating = false,
                        diary = prev.diary.map { item ->
                            if (item.selectionKey() == entryKey) {
                                item.copy(statusLabel = null)
                            } else {
                                item
                            }
                        },
                    )
                }
            }
        }
    }

    fun deleteSelected() {
        val st = _state.value
        if (st.mutating || st.selectedIds.isEmpty()) return
        when (st.shelf) {
            CollectionShelf.Favorites -> deleteFavorites(st.selectedIds)
            CollectionShelf.Diary -> deleteDiary(st.selectedIds)
            CollectionShelf.History -> Unit
        }
    }

    private fun deleteFavorites(keys: Set<String>) {
        viewModelScope.launch {
            _state.update { it.copy(mutating = true, actionError = null) }
            val targets = _state.value.favorites.filter { it.selectionKey() in keys }
            var failed = 0
            val removed = mutableSetOf<String>()
            for (item in targets) {
                val activity = item.toActivityItem()
                if (activity == null) {
                    failed++
                    continue
                }
                val ok = runCatching {
                    val resp = api.meActivity(
                        MeActivityRequest(op = "removeFavorite", item = activity),
                    )
                    resp.isSuccessful || resp.code() == 204
                }.getOrDefault(false)
                if (ok) removed += item.selectionKey() else failed++
            }
            updateDerived { prev ->
                prev.copy(
                    mutating = false,
                    favorites = prev.favorites.filterNot { it.selectionKey() in removed },
                    selectedIds = prev.selectedIds - removed,
                    selectionMode = (prev.selectedIds - removed).isNotEmpty(),
                    actionError = if (failed > 0) "Не удалось удалить ($failed)" else null,
                )
            }
        }
    }

    private fun deleteDiary(keys: Set<String>) {
        viewModelScope.launch {
            _state.update { it.copy(mutating = true, actionError = null) }
            val targets = _state.value.diary.filter { it.selectionKey() in keys }
            var failed = 0
            val removed = mutableSetOf<String>()
            for (item in targets) {
                val itemId = item.itemId ?: item.resolvedKp()?.let { "kp:$it" }
                if (itemId.isNullOrBlank()) {
                    failed++
                    continue
                }
                val ok = runCatching {
                    val resp = api.diaryRemove(itemId)
                    resp.isSuccessful && (resp.body()?.removed != false)
                }.getOrDefault(false)
                if (ok) removed += item.selectionKey() else failed++
            }
            updateDerived { prev ->
                prev.copy(
                    mutating = false,
                    diary = prev.diary.filterNot { it.selectionKey() in removed },
                    selectedIds = prev.selectedIds - removed,
                    selectionMode = (prev.selectedIds - removed).isNotEmpty(),
                    actionError = if (failed > 0) "Не удалось удалить ($failed)" else null,
                )
            }
        }
    }

    fun load(refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val shelf = _state.value.shelf
            val emptyForShelf = when (shelf) {
                CollectionShelf.Favorites -> _state.value.favorites.isEmpty()
                CollectionShelf.History -> _state.value.history.isEmpty()
                CollectionShelf.Diary -> _state.value.diary.isEmpty()
            }
            _state.update {
                it.copy(
                    loading = !refresh && emptyForShelf,
                    refreshing = refresh,
                    error = null,
                )
            }
            SessionRepository.refresh()
            if (!SessionRepository.state.value.isLoggedIn) {
                updateDerived {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        needsLogin = true,
                        favorites = emptyList(),
                        history = emptyList(),
                        diary = emptyList(),
                        selectionMode = false,
                        selectedIds = emptySet(),
                        error = null,
                    )
                }
                return@launch
            }

            fun List<FavoriteItemDto>.normalize(): List<FavoriteItemDto> =
                map { f -> f.copy(posterUrl = f.posterUrl.absMediaUrl()) }

            when (shelf) {
                CollectionShelf.Favorites, CollectionShelf.History -> {
                    val syncResult = runCatching { api.sync() }
                    syncResult
                        .onSuccess { sync ->
                            updateDerived { prev ->
                                prev.copy(
                                    loading = false,
                                    refreshing = false,
                                    needsLogin = false,
                                    favorites = sync.favorites.normalize(),
                                    history = sync.watchHistory.normalize(),
                                    error = null,
                                )
                            }
                        }
                        .onFailure { e ->
                            _state.update {
                                it.copy(
                                    loading = false,
                                    refreshing = false,
                                    needsLogin = false,
                                    error = e.message ?: "Не удалось загрузить",
                                )
                            }
                        }
                }
                CollectionShelf.Diary -> {
                    val diaryResult = runCatching { api.diaryList(page = 1, limit = 100) }
                    diaryResult
                        .onSuccess { body ->
                            updateDerived { prev ->
                                val statusFilter =
                                    prev.diaryStatusFilter?.takeIf { it in DiaryStatusLabels }
                                val reactionFilter = prev.diaryReactionFilter?.takeIf {
                                    it == DiaryFilterLike || it == DiaryFilterDislike
                                }
                                prev.copy(
                                    loading = false,
                                    refreshing = false,
                                    needsLogin = false,
                                    diary = body.entries,
                                    diaryStatusFilter = statusFilter,
                                    diaryReactionFilter = reactionFilter,
                                    error = null,
                                )
                            }
                        }
                        .onFailure { e ->
                            _state.update {
                                it.copy(
                                    loading = false,
                                    refreshing = false,
                                    needsLogin = false,
                                    error = e.message ?: "Не удалось загрузить",
                                )
                            }
                        }
                }
            }
        }
    }
}
