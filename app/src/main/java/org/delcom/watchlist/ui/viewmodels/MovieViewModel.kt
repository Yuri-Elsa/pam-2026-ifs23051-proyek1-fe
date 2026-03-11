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

// ── Sealed interfaces ─────────────────────────────────────────────────────────

sealed interface ProfileUIState {
    data class Success(val data: ResponseUserData) : ProfileUIState
    data class Error(val message: String) : ProfileUIState
    object Loading : ProfileUIState
}

sealed interface StatsUIState {
    data class Success(val data: ResponseStatsData) : StatsUIState
    data class Error(val message: String) : StatsUIState
    object Loading : StatsUIState
}

sealed interface MoviesUIState {
    data class Success(val data: List<ResponseMovieData>, val pagination: ResponsePagination) : MoviesUIState
    data class Error(val message: String) : MoviesUIState
    object Loading : MoviesUIState
}

sealed interface MovieUIState {
    data class Success(val data: ResponseMovieData) : MovieUIState
    data class Error(val message: String) : MovieUIState
    object Loading : MovieUIState
}

sealed interface MovieActionUIState {
    data class Success(val message: String) : MovieActionUIState
    data class Error(val message: String) : MovieActionUIState
    object Loading : MovieActionUIState
    object Idle : MovieActionUIState
}

sealed interface HomeMoviesUIState {
    data class Success(val data: List<ResponseMovieData>, val pagination: ResponsePagination) : HomeMoviesUIState
    data class Error(val message: String) : HomeMoviesUIState
    object Loading : HomeMoviesUIState
}

data class UIStateMovie(
    val profile: ProfileUIState = ProfileUIState.Loading,
    val stats: StatsUIState = StatsUIState.Loading,
    val movies: MoviesUIState = MoviesUIState.Loading,
    val movie: MovieUIState = MovieUIState.Loading,
    val movieAdd: MovieActionUIState = MovieActionUIState.Idle,
    val movieChange: MovieActionUIState = MovieActionUIState.Idle,
    val movieDelete: MovieActionUIState = MovieActionUIState.Idle,
    val movieChangeCover: MovieActionUIState = MovieActionUIState.Idle,
    val profileUpdate: MovieActionUIState = MovieActionUIState.Idle,
    val profilePassword: MovieActionUIState = MovieActionUIState.Idle,
    val profileAbout: MovieActionUIState = MovieActionUIState.Idle,
    val profilePhoto: MovieActionUIState = MovieActionUIState.Idle,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: IWatchListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UIStateMovie())
    val uiState = _uiState.asStateFlow()

    private val _homeMoviesState = MutableStateFlow<HomeMoviesUIState>(HomeMoviesUIState.Loading)
    val homeMoviesState = _homeMoviesState.asStateFlow()

    // ── Reset helpers ─────────────────────────────────────────────────────────

    fun resetMovieDetailState() {
        _uiState.update { it.copy(movie = MovieUIState.Loading, movieDelete = MovieActionUIState.Idle, movieChangeCover = MovieActionUIState.Idle) }
    }
    fun resetMovieAddState() { _uiState.update { it.copy(movieAdd = MovieActionUIState.Idle) } }
    fun resetMovieDeleteState() { _uiState.update { it.copy(movieDelete = MovieActionUIState.Idle) } }
    fun resetProfileUpdateState() { _uiState.update { it.copy(profileUpdate = MovieActionUIState.Idle) } }
    fun resetProfilePasswordState() { _uiState.update { it.copy(profilePassword = MovieActionUIState.Idle) } }
    fun resetProfileAboutState() { _uiState.update { it.copy(profileAbout = MovieActionUIState.Idle) } }
    fun resetProfilePhotoState() { _uiState.update { it.copy(profilePhoto = MovieActionUIState.Idle) } }

    // ── Home movies (isolated state) ──────────────────────────────────────────

    fun getHomeMovies(authToken: String, page: Int = 1, perPage: Int = 10) {
        viewModelScope.launch {
            _homeMoviesState.value = HomeMoviesUIState.Loading
            val result = repository.getMovies(authToken, null, page, perPage, null, null)
            _homeMoviesState.value = if (result.status == "success" && result.data != null) {
                val pagination = result.data.pagination ?: ResponsePagination(page, perPage, result.data.todos.size.toLong(), 1, false, false)
                HomeMoviesUIState.Success(result.data.todos, pagination)
            } else {
                HomeMoviesUIState.Error(result.message)
            }
        }
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    fun getProfile(authToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profile = ProfileUIState.Loading) }
            val result = repository.getUserMe(authToken)
            val state = if (result.status == "success" && result.data != null)
                ProfileUIState.Success(result.data.user)
            else ProfileUIState.Error(result.message)
            _uiState.update { it.copy(profile = state) }
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    fun getStats(authToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(stats = StatsUIState.Loading) }
            val result = repository.getMovieStats(authToken)
            val state = if (result.status == "success" && result.data != null)
                StatsUIState.Success(result.data.stats)
            else StatsUIState.Error(result.message)
            _uiState.update { it.copy(stats = state) }
        }
    }

    // ── Movies CRUD ───────────────────────────────────────────────────────────

    fun getAllMovies(authToken: String, search: String? = null, page: Int = 1, perPage: Int = 10, isDone: Boolean? = null, urgency: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(movies = MoviesUIState.Loading) }
            val result = repository.getMovies(authToken, search, page, perPage, isDone, urgency)
            val state = if (result.status == "success" && result.data != null) {
                val pagination = result.data.pagination ?: ResponsePagination(page, perPage, result.data.todos.size.toLong(), 1, false, false)
                MoviesUIState.Success(result.data.todos, pagination)
            } else {
                MoviesUIState.Error(result.message)
            }
            _uiState.update { it.copy(movies = state) }
        }
    }

    fun postMovie(authToken: String, title: String, description: String, watchStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(movieAdd = MovieActionUIState.Loading) }
            val result = repository.postMovie(authToken, RequestMovie(title, description, false, watchStatus))
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(movieAdd = state) }
        }
    }

    fun getMovieById(authToken: String, movieId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(movie = MovieUIState.Loading) }
            val result = repository.getMovieById(authToken, movieId)
            val state = if (result.status == "success" && result.data != null)
                MovieUIState.Success(result.data.todo)
            else MovieUIState.Error(result.message)
            _uiState.update { it.copy(movie = state) }
        }
    }

    fun putMovie(authToken: String, movieId: String, title: String, description: String, isDone: Boolean, watchStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(movieChange = MovieActionUIState.Loading) }
            val result = repository.putMovie(authToken, movieId, RequestMovie(title, description, isDone, watchStatus))
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(movieChange = state) }
        }
    }

    fun putMovieCover(authToken: String, movieId: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(movieChangeCover = MovieActionUIState.Loading) }
            val result = repository.putMovieCover(authToken, movieId, file)
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(movieChangeCover = state) }
        }
    }

    fun deleteMovie(authToken: String, movieId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(movieDelete = MovieActionUIState.Loading) }
            val result = repository.deleteMovie(authToken, movieId)
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(movieDelete = state) }
        }
    }

    // ── Profile operations ────────────────────────────────────────────────────

    fun updateProfile(authToken: String, name: String, username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileUpdate = MovieActionUIState.Loading) }
            val result = repository.putUserMe(authToken, RequestUserChange(name, username))
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(profileUpdate = state) }
        }
    }

    fun updatePassword(authToken: String, password: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profilePassword = MovieActionUIState.Loading) }
            val result = repository.putUserMePassword(authToken, RequestUserChangePassword(newPassword, password))
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(profilePassword = state) }
        }
    }

    fun updateAbout(authToken: String, about: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileAbout = MovieActionUIState.Loading) }
            val result = repository.putUserMeAbout(authToken, RequestUserAbout(about))
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(profileAbout = state) }
        }
    }

    fun updatePhoto(authToken: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(profilePhoto = MovieActionUIState.Loading) }
            val result = repository.putUserMePhoto(authToken, file)
            val state = if (result.status == "success") MovieActionUIState.Success(result.message)
            else MovieActionUIState.Error(result.message)
            _uiState.update { it.copy(profilePhoto = state) }
        }
    }
}
