package org.delcom.watchlist.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.delcom.watchlist.network.data.*
import org.delcom.watchlist.network.service.IWatchListRepository
import org.delcom.watchlist.prefs.AuthTokenPref
import javax.inject.Inject

// ── Sealed interfaces ─────────────────────────────────────────────────────────

sealed interface AuthUIState {
    data class Success(val data: ResponseAuthLogin) : AuthUIState
    data class Error(val message: String) : AuthUIState
    object Loading : AuthUIState
}

sealed interface AuthActionUIState {
    data class Success(val message: String) : AuthActionUIState
    data class Error(val message: String) : AuthActionUIState
    object Loading : AuthActionUIState
    object Idle : AuthActionUIState
}

sealed interface AuthLogoutUIState {
    data class Success(val message: String) : AuthLogoutUIState
    data class Error(val message: String) : AuthLogoutUIState
    object Loading : AuthLogoutUIState
    object Idle : AuthLogoutUIState
}

data class UIStateAuth(
    val auth: AuthUIState = AuthUIState.Loading,
    val authRegister: AuthActionUIState = AuthActionUIState.Idle,
    val authLogout: AuthLogoutUIState = AuthLogoutUIState.Idle,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: IWatchListRepository,
    private val authTokenPref: AuthTokenPref
) : ViewModel() {

    private val _uiState = MutableStateFlow(UIStateAuth())
    val uiState = _uiState.asStateFlow()

    fun resetRegisterState() {
        _uiState.update { it.copy(authRegister = AuthActionUIState.Idle) }
    }

    fun loadTokenFromPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(auth = AuthUIState.Loading) }
            val token = authTokenPref.getAuthToken()
            val refresh = authTokenPref.getRefreshToken()
            val state = if (token.isNullOrEmpty() || refresh.isNullOrEmpty()) {
                AuthUIState.Error("Token tidak tersedia")
            } else {
                AuthUIState.Success(ResponseAuthLogin(authToken = token, refreshToken = refresh))
            }
            _uiState.update { it.copy(auth = state) }
        }
    }

    fun register(name: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authRegister = AuthActionUIState.Loading) }
            val result = repository.postRegister(RequestAuthRegister(name, username, password))
            val state = if (result.status == "success" && result.data != null)
                AuthActionUIState.Success(result.data.userId)
            else
                AuthActionUIState.Error(result.message)
            _uiState.update { it.copy(authRegister = state) }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(auth = AuthUIState.Loading) }
            val result = repository.postLogin(RequestAuthLogin(username, password))
            val state = if (result.status == "success" && result.data != null) {
                authTokenPref.saveAuthToken(result.data.authToken)
                authTokenPref.saveRefreshToken(result.data.refreshToken)
                AuthUIState.Success(result.data)
            } else {
                AuthUIState.Error(result.message)
            }
            _uiState.update { it.copy(auth = state) }
        }
    }

    fun logout(authToken: String) {
        viewModelScope.launch {
            authTokenPref.clearAuthToken()
            authTokenPref.clearRefreshToken()
            _uiState.update { it.copy(authLogout = AuthLogoutUIState.Loading) }
            repository.postLogout(RequestAuthLogout(authToken))
            _uiState.update { it.copy(authLogout = AuthLogoutUIState.Success("Logged out")) }
        }
    }
}
