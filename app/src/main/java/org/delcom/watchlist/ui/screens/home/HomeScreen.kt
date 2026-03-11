package org.delcom.watchlist.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.helper.ToolsHelper
import org.delcom.watchlist.network.data.ResponseMovieData
import org.delcom.watchlist.network.data.WatchStatus
import org.delcom.watchlist.ui.components.BottomNavComponent
import org.delcom.watchlist.ui.components.MovieItemUI
import org.delcom.watchlist.ui.components.WatchListTopBar
import org.delcom.watchlist.ui.theme.*
import org.delcom.watchlist.ui.viewmodels.*

@Composable
fun HomeScreen(
    navController: NavHostController,
    authToken: String,
    movieViewModel: MovieViewModel
) {
    val isAuth = authToken.isNotBlank()
    val uiState      by movieViewModel.uiState.collectAsState()
    val homeState    by movieViewModel.homeMoviesState.collectAsState()

    // Stats
    LaunchedEffect(authToken) { if (isAuth) movieViewModel.getStats(authToken) }
    val stats = (uiState.stats as? StatsUIState.Success)?.data
    val statsLoading = uiState.stats is StatsUIState.Loading

    // Infinite scroll
    var nextPage     by remember { mutableStateOf(1) }
    var allMovies    by remember { mutableStateOf<List<ResponseMovieData>>(emptyList()) }
    var hasNextPage  by remember { mutableStateOf(false) }
    var loadingMore  by remember { mutableStateOf(false) }
    var firstLoad    by remember { mutableStateOf(true) }
    val loadedIds    = remember { mutableSetOf<String>() }
    val listState    = rememberLazyListState()

    fun load(page: Int) { if (isAuth) movieViewModel.getHomeMovies(authToken, page, 10) }
    fun reset() { nextPage = 1; allMovies = emptyList(); loadedIds.clear(); hasNextPage = false; loadingMore = false; firstLoad = true; load(1) }

    LaunchedEffect(authToken) { if (isAuth) reset() }
    LaunchedEffect(uiState.movieDelete) {
        if (uiState.movieDelete is MovieActionUIState.Success) {
            movieViewModel.resetMovieDeleteState(); if (isAuth) { reset(); movieViewModel.getStats(authToken) }
        }
    }
    LaunchedEffect(homeState) {
        when (val s = homeState) {
            is HomeMoviesUIState.Success -> {
                val newItems = s.data.filter { it.id !in loadedIds }
                loadedIds.addAll(newItems.map { it.id })
                allMovies = if (firstLoad) newItems else allMovies + newItems
                firstLoad = false
                if (newItems.isNotEmpty() || s.pagination.hasNextPage) nextPage = s.pagination.currentPage + 1
                hasNextPage = s.pagination.hasNextPage; loadingMore = false
            }
            is HomeMoviesUIState.Error -> { loadingMore = false; firstLoad = false }
            else -> {}
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { it.visibleItemsInfo.lastOrNull()?.index?.let { i -> i >= it.totalItemsCount - 3 } ?: false }
            .distinctUntilChanged().filter { it }
            .collect { if (!loadingMore && hasNextPage && isAuth) { loadingMore = true; load(nextPage) } }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WatchListTopBar(title = "WatchList", showBackButton = false, navController = navController)

        Box(modifier = Modifier.weight(1f)) {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate(if (isAuth) RouteHelper.MOVIE_ADD else RouteHelper.LOGIN) },
                        containerColor = CinemaRed
                    ) { Icon(Icons.Default.Add, null, tint = Color.White) }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { pad ->
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 16.dp)) {

                    // Hero banner
                    item(key = "hero") {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                                .background(Brush.linearGradient(listOf(Color(0xFF1A0000), CinemaRed.copy(0.8f), Color(0xFF0D0D1A))))
                        ) {
                            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Movie, null, tint = Color.White.copy(0.15f), modifier = Modifier.size(60.dp))
                                Text("Catatan Filmku", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color.White))
                                Text("Lacak semua film favoritmu", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(0.7f)))
                            }
                        }
                    }

                    if (!isAuth) {
                        item(key = "no_auth") {
                            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Masuk untuk mulai mencatat filmmu", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Button(onClick = { navController.navigate(RouteHelper.LOGIN) }, colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)) {
                                        Text("Masuk / Daftar", color = Color.White)
                                    }
                                }
                            }
                        }
                        return@LazyColumn
                    }

                    // Stats
                    item(key = "stats") {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Statistik", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard("Total", stats?.total?.toString() ?: "-", Icons.Default.VideoLibrary, CinemaRed, statsLoading, Modifier.weight(1f))
                                StatCard("Ditonton", stats?.done?.toString() ?: "-", Icons.Default.CheckCircle, WatchingBlue, statsLoading, Modifier.weight(1f))
                                StatCard("Belum", stats?.pending?.toString() ?: "-", Icons.Default.Schedule, PlannedPurple, statsLoading, Modifier.weight(1f))
                            }
                        }
                    }

                    // Watch status legend
                    item(key = "legend") {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Keterangan Status", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                                WatchStatus.entries.forEach { status ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Color(status.dotColorHex)))
                                        Text(status.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "recent_header") {
                        Spacer(Modifier.height(4.dp))
                        Text("Film Terbaru", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }

                    if (firstLoad && homeState is HomeMoviesUIState.Loading) {
                        item(key = "loading") { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CinemaRed) } }
                    } else if (allMovies.isEmpty()) {
                        item(key = "empty") { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Belum ada film. Tambahkan sekarang!", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    } else {
                        itemsIndexed(allMovies, key = { _, m -> m.id }) { _, movie ->
                            MovieItemUI(
                                movie = movie,
                                coverUrl = ToolsHelper.getMovieImage(movie.id, movie.updatedAt),
                                onClick = { navController.navigate(RouteHelper.movieDetail(movie.id)) },
                                onDelete = { movieViewModel.deleteMovie(authToken, movie.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        if (loadingMore) {
                            item(key = "loading_more") { Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = CinemaRed) } }
                        }
                    }
                }
            }
        }
        BottomNavComponent(navController)
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, color: Color, loading: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
            else Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
