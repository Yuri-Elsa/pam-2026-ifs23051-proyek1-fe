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
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.network.data.WatchStatus
import org.delcom.watchlist.ui.components.*
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieViewModel

private const val PER_PAGE = 10

@Composable
fun MovieListScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    movieViewModel: MovieViewModel,
) {
    val authToken = authViewModel.authToken

    if (authToken.isBlank()) {
        LaunchedEffect(Unit) {
            navController.navigate(RouteHelper.LOGIN) { popUpTo(0) { inclusive = true } }
        }
        return
    }

    val watchList by movieViewModel.watchList.collectAsState()
    val navBackStack by navController.currentBackStackEntryAsState()
    val listState = rememberLazyListState()

    var selectedStatus by remember { mutableStateOf<WatchStatus?>(null) }

    fun reload(urgency: String? = selectedStatus?.apiValue) {
        movieViewModel.resetWatchList()
        movieViewModel.loadWatchList(authToken, page = 1, perPage = PER_PAGE, urgency = urgency)
    }

    LaunchedEffect(authToken) { reload() }

    // Reload saat kembali dari AddScreen
    LaunchedEffect(navBackStack) {
        val handle = navBackStack?.savedStateHandle
        if (handle?.get<Boolean>("movie_added") == true) {
            handle.remove<Boolean>("movie_added")
            reload()
        }
    }

    // Infinite scroll
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { (it.visibleItemsInfo.lastOrNull()?.index ?: 0) to it.totalItemsCount }
            .distinctUntilChanged()
            .filter { (last, total) -> total > 0 && last >= total - 3 }
            .collect {
                val pagination = watchList.pagination
                if (!watchList.isLoadingMore && pagination?.hasNextPage == true) {
                    movieViewModel.loadWatchList(
                        authToken,
                        page = (pagination.currentPage + 1),
                        perPage = PER_PAGE,
                        urgency = selectedStatus?.apiValue,
                    )
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WatchListTopBar(title = "Watchlist Saya", navController = navController, showBackButton = false)

        Box(modifier = Modifier.weight(1f)) {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate(RouteHelper.MOVIE_ADD) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Icon(Icons.Default.Add, contentDescription = "Tambah Film") }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                    // Filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { selectedStatus = null; reload(null) },
                            label = { Text("Semua") },
                        )
                        WatchStatus.entries.forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = {
                                    selectedStatus = if (selectedStatus == status) null else status
                                    reload(selectedStatus?.apiValue)
                                },
                                label = { Text(status.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = androidx.compose.ui.graphics.Color(status.bgColorHex),
                                    selectedLabelColor = androidx.compose.ui.graphics.Color(status.dotColorHex),
                                ),
                            )
                        }
                    }

                    // List
                    when {
                        watchList.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        watchList.items.isEmpty() && !watchList.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Text("Belum ada film di watchlist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tap + untuk menambahkan.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(watchList.items, key = { _, m -> m.id }) { _, movie ->
                                MovieItemUI(
                                    movie = movie,
                                    onClick = { navController.navigate(RouteHelper.movieDetail(movie.id)) },
                                    onDelete = {
                                        movieViewModel.deleteMovie(authToken, movie.id)
                                        reload()
                                    },
                                )
                            }
                            if (watchList.isLoadingMore) {
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

        BottomNavComponent(navController)
    }
}