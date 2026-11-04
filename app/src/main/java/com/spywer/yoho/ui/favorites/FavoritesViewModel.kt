package com.spywer.yoho.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.remote.FavoriteItemDto
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.absMediaUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val favorites: List<FavoriteItemDto> = emptyList(),
    val history: List<FavoriteItemDto> = emptyList(),
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

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = !refresh && it.favorites.isEmpty() && it.history.isEmpty(),
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
                        error = null,
                    )
                }
                return@launch
            }
            runCatching { api.sync() }
                .onSuccess { sync ->
                    fun List<FavoriteItemDto>.normalize(): List<FavoriteItemDto> =
                        map { f -> f.copy(posterUrl = f.posterUrl.absMediaUrl()) }
                            .sortedByDescending { it.ts ?: 0L }

                    _state.update {
                        it.copy(
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
                            favorites = emptyList(),
                            history = emptyList(),
                            error = e.message ?: "Не удалось загрузить",
                        )
                    }
                }
        }
    }
}
