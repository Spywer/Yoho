package com.spywer.yoho.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.DiaryEntryDto
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.absMediaUrl
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

data class FavoritesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val shelf: CollectionShelf = CollectionShelf.Favorites,
    val favorites: List<FavoriteItemDto> = emptyList(),
    val history: List<FavoriteItemDto> = emptyList(),
    val diary: List<DiaryEntryDto> = emptyList(),
    val error: String? = null,
    val needsLogin: Boolean = false,
)

class FavoritesViewModel : ViewModel() {
    private val api = NetworkModule.api
    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun selectShelf(shelf: CollectionShelf) {
        _state.update { it.copy(shelf = shelf) }
    }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            val emptyShelves = _state.value.let {
                it.favorites.isEmpty() && it.history.isEmpty() && it.diary.isEmpty()
            }
            _state.update {
                it.copy(
                    loading = !refresh && emptyShelves,
                    refreshing = refresh,
                    error = null,
                )
            }
            SessionRepository.refresh()
            if (!SessionRepository.state.value.isLoggedIn) {
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        needsLogin = true,
                        favorites = emptyList(),
                        history = emptyList(),
                        diary = emptyList(),
                        error = null,
                    )
                }
                return@launch
            }
            val syncResult = runCatching { api.sync() }
            val diaryResult = runCatching { api.diaryList(page = 1, limit = 50) }

            fun List<FavoriteItemDto>.normalize(): List<FavoriteItemDto> =
                map { f -> f.copy(posterUrl = f.posterUrl.absMediaUrl()) }
                    .sortedByDescending { it.ts ?: 0L }

            syncResult
                .onSuccess { sync ->
                    val diaryItems = diaryResult.getOrNull()?.entries.orEmpty()
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            needsLogin = false,
                            favorites = sync.favorites.normalize(),
                            history = sync.watchHistory.normalize(),
                            diary = diaryItems,
                            error = if (diaryResult.isFailure &&
                                sync.favorites.isEmpty() &&
                                sync.watchHistory.isEmpty()
                            ) {
                                diaryResult.exceptionOrNull()?.message ?: "Не удалось загрузить"
                            } else {
                                null
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            needsLogin = false,
                            favorites = emptyList(),
                            history = emptyList(),
                            diary = diaryResult.getOrNull()?.entries.orEmpty(),
                            error = e.message ?: "Не удалось загрузить",
                        )
                    }
                }
        }
    }
}
