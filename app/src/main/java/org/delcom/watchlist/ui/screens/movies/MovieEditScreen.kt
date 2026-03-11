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
import org.delcom.watchlist.ui.components.WatchListSnackbar
import org.delcom.watchlist.ui.components.WatchStatusSelector
import org.delcom.watchlist.ui.viewmodels.MovieActionUIState
import org.delcom.watchlist.ui.viewmodels.MovieUIState
import org.delcom.watchlist.ui.viewmodels.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieEditScreen(
    authToken: String,
    movieId: String,
    movieViewModel: MovieViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by movieViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var watchStatus by remember { mutableStateOf(WatchStatus.PLANNED) }
    var isLoading   by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    // ── Load movie data once ───────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (authToken.isNotBlank() && !initialized) {
            movieViewModel.getMovieById(authToken, movieId)
        }
    }

    LaunchedEffect(uiState.movie) {
        val state = uiState.movie
        if (!initialized && state is MovieUIState.Success) {
            val m = state.data
            title       = m.title
            description = m.cleanDescription
            releaseYear = m.releaseYear ?: ""
            watchStatus = m.watchStatus
            initialized = true
        }
    }

    // ── Handle edit result ─────────────────────────────────────────────────────
    LaunchedEffect(uiState.movieChange) {
        when (val s = uiState.movieChange) {
            is MovieActionUIState.Success -> {
                isLoading = false
                snackbarHostState.showSnackbar("success|Film berhasil diperbarui")
                // Refresh detail state so parent screen re-loads
                authToken.let { movieViewModel.getMovieById(it, movieId) }
                onNavigateBack()
            }
            is MovieActionUIState.Error -> {
                isLoading = false
                snackbarHostState.showSnackbar("error|${s.message}")
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                WatchListSnackbar(data) { snackbarHostState.currentSnackbarData?.dismiss() }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Edit Film") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        if (!initialized) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

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

            // ── Release Year ──────────────────────────────────────────────────
            OutlinedTextField(
                value = releaseYear,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) releaseYear = it },
                label = { Text("Tahun Rilis (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Contoh: 2014") }
            )

            // ── Description ───────────────────────────────────────────────────
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
                    val finalDesc = buildString {
                        if (releaseYear.isNotBlank()) append("[$releaseYear] ")
                        append(description)
                    }.trim()
                    movieViewModel.putMovie(
                        authToken   = authToken,
                        movieId     = movieId,
                        title       = title.trim(),
                        description = finalDesc,
                        isDone      = watchStatus == WatchStatus.COMPLETED,
                        watchStatus = watchStatus.apiValue
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !isLoading && title.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.onPrimary
                )
                else Text("Simpan Perubahan")
            }
        }
    }
}
