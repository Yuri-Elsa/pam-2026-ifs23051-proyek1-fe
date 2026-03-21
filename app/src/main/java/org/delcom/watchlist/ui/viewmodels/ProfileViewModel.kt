package org.delcom.watchlist.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.delcom.watchlist.network.data.*
import org.delcom.watchlist.network.service.IWatchListRepository
import javax.inject.Inject

/**
 * State khusus profil user — dipisah dari MovieViewModel agar lebih fokus.
 */
data class ProfileUiState(
    val profile: UiState<ResponseUserData> = UiState.Idle,
    val update: ActionState = UiState.Idle,
    val password: ActionState = UiState.Idle,
    val about: ActionState = UiState.Idle,
    val photo: ActionState = UiState.Idle,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: IWatchListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    // ── Fetch ─────────────────────────────────────────────────────────────────

    fun loadProfile(authToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profile = UiState.Loading) }
            val result = repository.getUserMe(authToken)
            val state: UiState<ResponseUserData> =
                if (result.status == "success" && result.data != null)
                    UiState.Success(result.data.user)
                else
                    UiState.Error(result.message)
            _uiState.update { it.copy(profile = state) }
        }
    }

    // ── Update nama & username ────────────────────────────────────────────────

    fun updateProfile(authToken: String, name: String, username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(update = UiState.Loading) }
            val result = repository.putUserMe(authToken, RequestUserChange(name, username))
            val state: ActionState =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
            _uiState.update { it.copy(update = state) }
        }
    }

    fun resetUpdate() = _uiState.update { it.copy(update = UiState.Idle) }

    // ── Ganti password ────────────────────────────────────────────────────────

    fun updatePassword(authToken: String, oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(password = UiState.Loading) }
            val result = repository.putUserMePassword(
                authToken,
                RequestUserChangePassword(newPassword, oldPassword)
            )
            val state: ActionState =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
            _uiState.update { it.copy(password = state) }
        }
    }

    fun resetPassword() = _uiState.update { it.copy(password = UiState.Idle) }

    // ── Tentang (bio) ─────────────────────────────────────────────────────────

    fun updateAbout(authToken: String, about: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(about = UiState.Loading) }
            val result = repository.putUserMeAbout(authToken, RequestUserAbout(about))
            val state: ActionState =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
            _uiState.update { it.copy(about = state) }
        }
    }

    fun resetAbout() = _uiState.update { it.copy(about = UiState.Idle) }

    // ── Foto profil ───────────────────────────────────────────────────────────

    fun uploadPhoto(authToken: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(photo = UiState.Loading) }
            val result = repository.putUserMePhoto(authToken, file)
            val state: ActionState =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
            _uiState.update { it.copy(photo = state) }
        }
    }

    fun resetPhoto() = _uiState.update { it.copy(photo = UiState.Idle) }
}