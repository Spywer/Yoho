package com.spywer.yoho.data

import com.spywer.yoho.data.remote.AuthErrors
import com.spywer.yoho.data.remote.AuthSessionResponse
import com.spywer.yoho.data.remote.AuthUserDto
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SessionCookieJar
import com.spywer.yoho.data.remote.SignInEmailRequest
import com.spywer.yoho.data.remote.SignUpEmailRequest
import com.spywer.yoho.data.remote.absMediaUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.Response

data class SessionUiState(
    val loading: Boolean = true,
    val user: AuthUserDto? = null,
    val error: String? = null,
    val busy: Boolean = false,
) {
    val isLoggedIn: Boolean get() = user != null
}

/** Текущая сессия (cookie yoho_session через OkHttp CookieJar). */
object SessionRepository {
    private val api get() = NetworkModule.api
    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    suspend fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        val user = fetchCurrentUser()
        _state.update {
            it.copy(
                loading = false,
                user = user,
                error = null,
            )
        }
    }

    suspend fun signIn(email: String, password: String): Boolean {
        _state.update { it.copy(busy = true, error = null) }
        return runCatching {
            api.signInEmail(SignInEmailRequest(email = email.trim(), password = password))
        }.fold(
            onSuccess = { resp -> applyAuthResponse(resp, failFallback = "Неверный email или пароль") },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        busy = false,
                        error = AuthErrors.fromThrowable(e, "Неверный email или пароль"),
                    )
                }
                false
            },
        )
    }

    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        name: String,
    ): Boolean {
        _state.update { it.copy(busy = true, error = null) }
        val nick = username.trim()
        val display = name.trim().ifBlank { nick }
        return runCatching {
            api.signUpEmail(
                SignUpEmailRequest(
                    email = email.trim(),
                    password = password,
                    username = nick,
                    name = display,
                ),
            )
        }.fold(
            onSuccess = { resp ->
                applyAuthResponse(resp, failFallback = "Не удалось зарегистрироваться")
            },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        busy = false,
                        error = AuthErrors.fromThrowable(e, "Не удалось зарегистрироваться"),
                    )
                }
                false
            },
        )
    }

    suspend fun signOut() {
        _state.update { it.copy(busy = true, error = null) }
        runCatching { api.signOut() }
        clearCookies()
        _state.update {
            it.copy(busy = false, user = null, loading = false)
        }
    }

    private suspend fun fetchCurrentUser(): AuthUserDto? {
        val resp = runCatching { api.authGetSession() }.getOrNull() ?: return null
        if (!resp.isSuccessful) return null
        // Better Auth: 200 + null body = нет сессии
        return resp.body()?.user?.normalized()
    }

    private fun applyAuthResponse(
        resp: Response<AuthSessionResponse>,
        failFallback: String,
    ): Boolean {
        if (!resp.isSuccessful) {
            _state.update {
                it.copy(busy = false, error = AuthErrors.fromResponse(resp))
            }
            return false
        }
        val user = resp.body()?.user?.normalized()
        if (user == null) {
            _state.update {
                it.copy(busy = false, error = failFallback, user = null)
            }
            return false
        }
        _state.update {
            it.copy(busy = false, user = user, error = null, loading = false)
        }
        return true
    }

    private fun clearCookies() {
        runCatching { SessionCookieJar.get().clear() }
    }

    private fun AuthUserDto.normalized(): AuthUserDto =
        copy(
            displayName = resolvedDisplayName(),
            avatar = resolvedAvatar().absMediaUrl(),
            name = name,
            image = image,
        )
}
