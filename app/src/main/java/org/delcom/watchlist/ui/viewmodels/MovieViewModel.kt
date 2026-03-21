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
 * State untuk daftar film dengan pagination.
 */
data class MovieListUiState(
    val items: List<ResponseMovieData> = emptyList(),
    val pagination: ResponsePagination? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

/**
 * State untuk satu film detail.
 */
data class MovieDetailUiState(
    val movie: UiState<ResponseMovieData> = UiState.Idle,
    val cover: ActionState = UiState.Idle,
    val edit: ActionState = UiState.Idle,
    val delete: ActionState = UiState.Idle,
)

/**
 * ViewModel khusus film — stats, list, detail, CRUD.
 * Profil user dipindah ke [ProfileViewModel].
 */
@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: IWatchListRepository,
) : ViewModel() {

    // ── Stats ─────────────────────────────────────────────────────────────────
    private val _stats = MutableStateFlow<UiState<ResponseStatsData>>(UiState.Idle)
    val stats = _stats.asStateFlow()

    // ── Home list (unfiltered, infinite scroll) ───────────────────────────────
    private val _homeList = MutableStateFlow(MovieListUiState())
    val homeList = _homeList.asStateFlow()

    // ── Watchlist (dengan filter + infinite scroll) ───────────────────────────
    private val _watchList = MutableStateFlow(MovieListUiState())
    val watchList = _watchList.asStateFlow()

    // ── Detail + aksi per-film ────────────────────────────────────────────────
    private val _detail = MutableStateFlow(MovieDetailUiState())
    val detail = _detail.asStateFlow()

    // ── Add film ──────────────────────────────────────────────────────────────
    private val _addState = MutableStateFlow<ActionState>(UiState.Idle)
    val addState = _addState.asStateFlow()

    // ═════════════════════════════════════════════════════════════════════════
    // Stats
    // ═════════════════════════════════════════════════════════════════════════

    fun loadStats(authToken: String) {
        viewModelScope.launch {
            _stats.value = UiState.Loading
            val result = repository.getMovieStats(authToken)
            _stats.value =
                if (result.status == "success" && result.data != null)
                    UiState.Success(result.data.stats)
                else UiState.Error(result.message)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Home list  (halaman pertama = reset, halaman berikut = append)
    // ═════════════════════════════════════════════════════════════════════════

    fun loadHomeMovies(authToken: String, page: Int = 1, perPage: Int = 10) {
        viewModelScope.launch {
            val isFirstPage = page == 1
            if (isFirstPage) {
                _homeList.update { it.copy(isLoading = true, error = null) }
            } else {
                _homeList.update { it.copy(isLoadingMore = true) }
            }

            val result = repository.getMovies(authToken, null, page, perPage, null, null)

            if (result.status == "success" && result.data != null) {
                val pagination = result.data.pagination
                    ?: ResponsePagination(page, perPage, result.data.watchlists.size.toLong(), 1, false, false)
                _homeList.update { state ->
                    val merged = if (isFirstPage) result.data.watchlists
                    else (state.items + result.data.watchlists).distinctBy { it.id }
                    state.copy(
                        items = merged,
                        pagination = pagination,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                    )
                }
            } else {
                _homeList.update { it.copy(isLoading = false, isLoadingMore = false, error = result.message) }
            }
        }
    }

    fun resetHomeList() {
        _homeList.value = MovieListUiState()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Watchlist  (dengan filter urgency, reset + append)
    // ═════════════════════════════════════════════════════════════════════════

    fun loadWatchList(
        authToken: String,
        page: Int = 1,
        perPage: Int = 10,
        urgency: String? = null,
    ) {
        viewModelScope.launch {
            val isFirstPage = page == 1
            if (isFirstPage) {
                _watchList.update { it.copy(isLoading = true, error = null) }
            } else {
                _watchList.update { it.copy(isLoadingMore = true) }
            }

            val result = repository.getMovies(authToken, null, page, perPage, null, urgency)

            if (result.status == "success" && result.data != null) {
                val pagination = result.data.pagination
                    ?: ResponsePagination(page, perPage, result.data.watchlists.size.toLong(), 1, false, false)
                _watchList.update { state ->
                    val merged = if (isFirstPage) result.data.watchlists
                    else (state.items + result.data.watchlists).distinctBy { it.id }
                    state.copy(
                        items = merged,
                        pagination = pagination,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                    )
                }
            } else {
                _watchList.update { it.copy(isLoading = false, isLoadingMore = false, error = result.message) }
            }
        }
    }

    fun resetWatchList() {
        _watchList.value = MovieListUiState()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Detail
    // ═════════════════════════════════════════════════════════════════════════

    fun loadMovieById(authToken: String, movieId: String) {
        viewModelScope.launch {
            _detail.update { it.copy(movie = UiState.Loading) }
            val result = repository.getMovieById(authToken, movieId)
            val state: UiState<ResponseMovieData> =
                if (result.status == "success" && result.data != null)
                    UiState.Success(result.data.watchlist)
                else UiState.Error(result.message)
            _detail.update { it.copy(movie = state) }
        }
    }

    fun resetDetail() {
        _detail.value = MovieDetailUiState()
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    fun editMovie(
        authToken: String,
        movieId: String,
        title: String,
        description: String,
        isDone: Boolean,
        watchStatus: String,
    ) {
        viewModelScope.launch {
            _detail.update { it.copy(edit = UiState.Loading) }
            val result = repository.putMovie(
                authToken, movieId,
                RequestMovie(title, description, isDone, watchStatus)
            )
            val state: ActionState =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
            _detail.update { it.copy(edit = state) }
        }
    }

    fun resetEdit() = _detail.update { it.copy(edit = UiState.Idle) }

    // ── Cover ─────────────────────────────────────────────────────────────────

    fun uploadCover(authToken: String, movieId: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _detail.update { it.copy(cover = UiState.Loading) }
            val result = repository.putMovieCover(authToken, movieId, file)
            val state: ActionState =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
            _detail.update { it.copy(cover = state) }
        }
    }

    fun resetCover() = _detail.update { it.copy(cover = UiState.Idle) }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteMovie(authToken: String, movieId: String) {
        viewModelScope.launch {
            _detail.update { it.copy(delete = UiState.Loading) }
            val result = repository.deleteMovie(authToken, movieId)
            val state: ActionState =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
            _detail.update { it.copy(delete = state) }
        }
    }

    fun resetDelete() = _detail.update { it.copy(delete = UiState.Idle) }

    // ═════════════════════════════════════════════════════════════════════════
    // Add
    // ═════════════════════════════════════════════════════════════════════════

    fun addMovie(authToken: String, title: String, description: String, watchStatus: String) {
        viewModelScope.launch {
            _addState.value = UiState.Loading
            val result = repository.postMovie(
                authToken,
                RequestMovie(title, description, false, watchStatus)
            )
            _addState.value =
                if (result.status == "success") UiState.Success(result.message)
                else UiState.Error(result.message)
        }
    }

    fun resetAdd() {
        _addState.value = UiState.Idle
    }
}