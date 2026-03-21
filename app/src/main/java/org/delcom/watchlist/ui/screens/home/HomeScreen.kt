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
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.network.data.ResponseStatsData
import org.delcom.watchlist.network.data.WatchStatus
import org.delcom.watchlist.ui.components.BottomNavComponent
import org.delcom.watchlist.ui.components.MovieItemUI
import org.delcom.watchlist.ui.components.WatchListTopBar
import org.delcom.watchlist.ui.theme.*
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieViewModel
import org.delcom.watchlist.ui.viewmodels.UiState

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    movieViewModel: MovieViewModel,
) {
    val authToken = authViewModel.authToken
    val isAuth = authToken.isNotBlank()

    val stats by movieViewModel.stats.collectAsState()
    val homeList by movieViewModel.homeList.collectAsState()
    val listState = rememberLazyListState()

    // Load awal
    LaunchedEffect(authToken) {
        if (isAuth) {
            movieViewModel.loadStats(authToken)
            movieViewModel.loadHomeMovies(authToken, page = 1)
        }
    }

    // Reload setelah delete dari list
    LaunchedEffect(homeList.error) { /* handled inline */ }

    // Infinite scroll trigger
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map {
                val last = it.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= it.totalItemsCount - 3
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                val pagination = homeList.pagination
                if (!homeList.isLoadingMore && pagination?.hasNextPage == true && isAuth) {
                    movieViewModel.loadHomeMovies(authToken, page = (pagination.currentPage + 1))
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WatchListTopBar(title = "WatchList", showBackButton = false, navController = navController)

        Box(modifier = Modifier.weight(1f)) {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate(if (isAuth) RouteHelper.MOVIE_ADD else RouteHelper.LOGIN) },
                        containerColor = CinemaRed,
                    ) { Icon(Icons.Default.Add, null, tint = Color.White) }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { pad ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {

                    // Hero banner
                    item(key = "hero") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1A0000), CinemaRed.copy(0.8f), Color(0xFF0D0D1A))
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Default.Movie, null,
                                    tint = Color.White.copy(0.15f),
                                    modifier = Modifier.size(60.dp),
                                )
                                Text(
                                    "Catatan Filmku",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold, color = Color.White
                                    )
                                )
                                Text(
                                    "Lacak semua film favoritmu",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(0.7f))
                                )
                            }
                        }
                    }

                    // Belum login
                    if (!isAuth) {
                        item(key = "no_auth") {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        "Masuk untuk mulai mencatat filmmu",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Button(
                                        onClick = { navController.navigate(RouteHelper.LOGIN) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                                    ) { Text("Masuk / Daftar", color = Color.White) }
                                }
                            }
                        }
                        return@LazyColumn
                    }

                    // Statistik
                    item(key = "stats") {
                        val statsData = (stats as? UiState.Success)?.data
                        val statsLoading = stats is UiState.Loading
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Statistik",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard("Total", statsData?.total?.toString() ?: "-", Icons.Default.VideoLibrary, CinemaRed, statsLoading, Modifier.weight(1f))
                                StatCard("Ditonton", statsData?.done?.toString() ?: "-", Icons.Default.CheckCircle, WatchingBlue, statsLoading, Modifier.weight(1f))
                                StatCard("Belum", statsData?.pending?.toString() ?: "-", Icons.Default.Schedule, PlannedPurple, statsLoading, Modifier.weight(1f))
                            }
                        }
                    }

                    // Legenda status
                    item(key = "legend") {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "Keterangan Status",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                WatchStatus.entries.forEach { status ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(status.dotColorHex))
                                        )
                                        Text(
                                            status.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "recent_header") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Film Terbaru",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    // Isi list
                    when {
                        homeList.isLoading -> item(key = "loading") {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CinemaRed)
                            }
                        }
                        homeList.items.isEmpty() -> item(key = "empty") {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada film. Tambahkan sekarang!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> {
                            itemsIndexed(homeList.items, key = { _, m -> m.id }) { _, movie ->
                                MovieItemUI(
                                    movie = movie,
                                    onClick = { navController.navigate(RouteHelper.movieDetail(movie.id)) },
                                    onDelete = {
                                        movieViewModel.deleteMovie(authToken, movie.id)
                                        // Reload setelah delete
                                        movieViewModel.loadHomeMovies(authToken, 1)
                                        movieViewModel.loadStats(authToken)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                            if (homeList.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { CircularProgressIndicator(Modifier.size(24.dp), color = CinemaRed) }
                                }
                            }
                        }
                    }
                }
            }
        }

        BottomNavComponent(navController)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            if (loading)
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
            else
                Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}