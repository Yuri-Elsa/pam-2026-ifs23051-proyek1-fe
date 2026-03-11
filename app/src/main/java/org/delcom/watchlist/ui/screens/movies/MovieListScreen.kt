package org.delcom.watchlist.ui.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.delcom.watchlist.helper.ToolsHelper
import org.delcom.watchlist.network.data.ResponseMovieData
import org.delcom.watchlist.network.data.WatchStatus
import org.delcom.watchlist.ui.components.*
import org.delcom.watchlist.ui.viewmodels.MovieActionUIState
import org.delcom.watchlist.ui.viewmodels.MovieViewModel
import org.delcom.watchlist.ui.viewmodels.MoviesUIState

private const val PER_PAGE = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    navController: NavHostController,
    authToken: String,
    movieViewModel: MovieViewModel,
) {
    if (authToken.isBlank()) {
        LaunchedEffect(Unit) { navController.navigate("auth/login") { popUpTo(0) { inclusive = true } } }
        return
    }

    val uiState by movieViewModel.uiState.collectAsState()
    val navBackStack by navController.currentBackStackEntryAsState()

    var selectedStatus by remember { mutableStateOf<WatchStatus?>(null) }
    var nextPageToLoad by remember { mutableStateOf(1) }
    var allMovies by remember { mutableStateOf<List<ResponseMovieData>>(emptyList()) }
    var hasNextPage by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isFirstLoad by remember { mutableStateOf(true) }
    val loadedIds = remember { mutableSetOf<String>() }
    val listState = rememberLazyListState()

    fun loadPage(page: Int, urgency: String?) =
        movieViewModel.getAllMovies(authToken, page = page, perPage = PER_PAGE, urgency = urgency)

    fun resetAndLoad(urgency: String? = selectedStatus?.apiValue) {
        nextPageToLoad = 1; allMovies = emptyList(); loadedIds.clear()
        hasNextPage = false; isLoadingMore = false; isFirstLoad = true
        loadPage(1, urgency)
    }

    LaunchedEffect(authToken) { resetAndLoad() }

    LaunchedEffect(navBackStack) {
        val handle = navBackStack?.savedStateHandle
        if (handle?.get<Boolean>("movie_added") == true) {
            handle.remove<Boolean>("movie_added")
            resetAndLoad()
        }
    }

    LaunchedEffect(uiState.movieDelete) {
        if (uiState.movieDelete is MovieActionUIState.Success) {
            movieViewModel.resetMovieDeleteState()
            resetAndLoad()
        }
    }

    LaunchedEffect(uiState.movies) {
        when (val state = uiState.movies) {
            is MoviesUIState.Success -> {
                val newItems = state.data.filter { it.id !in loadedIds }
                loadedIds.addAll(newItems.map { it.id })
                allMovies = if (isFirstLoad) { isFirstLoad = false; newItems } else allMovies + newItems
                nextPageToLoad = state.pagination.currentPage + 1
                hasNextPage = state.pagination.hasNextPage
                isLoadingMore = false
            }
            is MoviesUIState.Error -> { isLoadingMore = false; isFirstLoad = false }
            is MoviesUIState.Loading -> {}
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { (it.visibleItemsInfo.lastOrNull()?.index ?: 0) to it.totalItemsCount }
            .distinctUntilChanged()
            .filter { (last, total) -> total > 0 && last >= total - 3 }
            .collect {
                if (!isLoadingMore && hasNextPage) {
                    isLoadingMore = true
                    loadPage(nextPageToLoad, selectedStatus?.apiValue)
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        WatchListTopBar(title = "Watchlist Saya", navController = navController, showBackButton = false)

        Box(modifier = Modifier.weight(1f)) {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate("movies/add") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) { Icon(Icons.Default.Add, contentDescription = "Tambah Film") }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                    // ── Status filter chips ────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { selectedStatus = null; resetAndLoad(null) },
                            label = { Text("Semua") }
                        )
                        WatchStatus.entries.forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = {
                                    selectedStatus = if (selectedStatus == status) null else status
                                    resetAndLoad(selectedStatus?.apiValue)
                                },
                                label = { Text(status.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = androidx.compose.ui.graphics.Color(status.bgColorHex),
                                    selectedLabelColor = androidx.compose.ui.graphics.Color(status.dotColorHex)
                                )
                            )
                        }
                    }

                    // ── List ───────────────────────────────────────────────────
                    when (val moviesState = uiState.movies) {
                        is MoviesUIState.Loading -> if (isFirstLoad) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is MoviesUIState.Error -> {
                             // error handled by snackbar or empty view
                        }
                        else -> {}
                    }

                    if (!isFirstLoad && allMovies.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Text("Belum ada film di watchlist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tap + untuk menambahkan.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else if (allMovies.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(allMovies, key = { _, m -> m.id }) { _, movie ->
                                MovieItemUI(
                                    movie = movie,
                                    coverUrl = ToolsHelper.getMovieImage(movie.id, movie.updatedAt),
                                    onClick = { navController.navigate("movies/${movie.id}") },
                                    onDelete = { movieViewModel.deleteMovie(authToken, movie.id) }
                                )
                            }
                            if (isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        BottomNavComponent(navController = navController)
    }
}
