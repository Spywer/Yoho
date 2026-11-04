package com.spywer.yoho.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spywer.yoho.data.SessionRepository
import com.spywer.yoho.data.SessionUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {
    val state: StateFlow<SessionUiState> = SessionRepository.state

    fun refresh() {
        viewModelScope.launch { SessionRepository.refresh() }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch { SessionRepository.signIn(email, password) }
    }

    fun signUp(email: String, password: String, username: String, name: String) {
        viewModelScope.launch {
            SessionRepository.signUp(email, password, username, name)
        }
    }

    fun signOut() {
        viewModelScope.launch { SessionRepository.signOut() }
    }
}
