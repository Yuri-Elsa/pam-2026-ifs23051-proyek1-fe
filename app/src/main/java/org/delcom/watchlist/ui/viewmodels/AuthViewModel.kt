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

/**
 * State untuk AuthViewModel — lebih ramping dari sebelumnya.
 * Gunakan [UiState] generik daripada sealed interface per-action.
 */
data class AuthUiState(
    val session: UiState<ResponseAuthLogin> = UiState.Loading,
    val register: ActionState = UiState.Idle,
    val logout: ActionState = UiState.Idle,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: IWatchListRepository,
    private val authTokenPref: AuthTokenPref,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    /** Token dari state aktif, atau string kosong jika belum login */
    val authToken: String
        get() = (_uiState.value.session as? UiState.Success)?.data?.authToken ?: ""

    // ── Session ───────────────────────────────────────────────────────────────

    fun loadTokenFromPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(session = UiState.Loading) }
            val token = authTokenPref.getAuthToken()
            val refresh = authTokenPref.getRefreshToken()
            val state: UiState<ResponseAuthLogin> =
                if (!token.isNullOrEmpty() && !refresh.isNullOrEmpty())
                    UiState.Success(ResponseAuthLogin(token, refresh))
                else
                    UiState.Error("Belum login")
            _uiState.update { it.copy(session = state) }
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────

    fun register(name: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(register = UiState.Loading) }
            val result = repository.postRegister(RequestAuthRegister(name, username, password))
            val state: ActionState =
                if (result.status == "success" && result.data != null)
                    UiState.Success(result.data.userId)
                else
                    UiState.Error(result.message)
            _uiState.update { it.copy(register = state) }
        }
    }

    fun resetRegisterState() {
        _uiState.update { it.copy(register = UiState.Idle) }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(session = UiState.Loading) }
            val result = repository.postLogin(RequestAuthLogin(username, password))
            val state: UiState<ResponseAuthLogin> =
                if (result.status == "success" && result.data != null) {
                    authTokenPref.saveAuthToken(result.data.authToken)
                    authTokenPref.saveRefreshToken(result.data.refreshToken)
                    UiState.Success(result.data)
                } else {
                    UiState.Error(result.message)
                }
            _uiState.update { it.copy(session = state) }
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout(authToken: String) {
        viewModelScope.launch {
            // Hapus token lokal dulu agar UI langsung merespons
            authTokenPref.clearAuthToken()
            authTokenPref.clearRefreshToken()
            _uiState.update { it.copy(logout = UiState.Loading) }
            repository.postLogout(RequestAuthLogout(authToken))
            _uiState.update { it.copy(logout = UiState.Success("Logged out")) }
        }
    }
}