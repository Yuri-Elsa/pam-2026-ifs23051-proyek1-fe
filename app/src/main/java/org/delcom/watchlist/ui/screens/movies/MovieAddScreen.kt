package org.delcom.watchlist.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.delcom.watchlist.network.data.WatchStatus
import org.delcom.watchlist.ui.components.WatchStatusSelector
import org.delcom.watchlist.ui.viewmodels.MovieActionUIState
import org.delcom.watchlist.ui.viewmodels.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieAddScreen(
    authToken: String,
    movieViewModel: MovieViewModel,
    navController: NavHostController,
    onNavigateBack: () -> Unit,
) {
    val uiState by movieViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var watchStatus by remember { mutableStateOf(WatchStatus.PLANNED) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.movieAdd) {
        when (val state = uiState.movieAdd) {
            is MovieActionUIState.Success -> {
                isLoading = false
                navController.previousBackStackEntry?.savedStateHandle?.set("movie_added", true)
                movieViewModel.resetMovieAddState()
                onNavigateBack()
            }
            is MovieActionUIState.Error -> {
                isLoading = false
                snackbarHostState.showSnackbar("error|${state.message}")
                movieViewModel.resetMovieAddState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                org.delcom.watchlist.ui.components.WatchListSnackbar(data) { snackbarHostState.currentSnackbarData?.dismiss() }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Tambah Film") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul Film *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Contoh: Interstellar") }
            )

            // ── Release Year (optional) ───────────────────────────────────────
            OutlinedTextField(
                value = releaseYear,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) releaseYear = it },
                label = { Text("Tahun Rilis (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Contoh: 2014") }
            )

            // ── Description (optional) ────────────────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi / Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("Ceritakan sedikit tentang film ini...") }
            )

            // ── Watch Status ──────────────────────────────────────────────────
            Text("Status Tonton", style = MaterialTheme.typography.labelLarge)
            WatchStatusSelector(
                selectedStatus = watchStatus,
                onStatusSelected = { watchStatus = it }
            )

            Spacer(Modifier.height(8.dp))

            // ── Submit ────────────────────────────────────────────────────────
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    isLoading = true
                    // Encode year into description if provided
                    val finalDesc = buildString {
                        if (releaseYear.isNotBlank()) append("[$releaseYear] ")
                        append(description)
                    }.trim()
                    movieViewModel.postMovie(authToken, title.trim(), finalDesc, watchStatus.apiValue)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading && title.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Simpan ke Watchlist")
            }
        }
    }
}
