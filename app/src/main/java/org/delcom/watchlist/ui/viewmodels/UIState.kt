package org.delcom.watchlist.ui.viewmodels

/**
 * Generic UI state wrapper digunakan di seluruh ViewModel.
 * Menggantikan sealed interface yang terulang-ulang di setiap ViewModel.
 */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Shorthand untuk action state (tanpa data payload) */
typealias ActionState = UiState<String>