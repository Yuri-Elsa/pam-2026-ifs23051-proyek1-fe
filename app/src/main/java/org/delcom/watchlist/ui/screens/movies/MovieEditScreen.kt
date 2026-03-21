package org.delcom.watchlist.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.delcom.watchlist.network.data.WatchStatus
import org.delcom.watchlist.ui.components.WatchListSnackbar
import org.delcom.watchlist.ui.components.WatchStatusSelector
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieViewModel
import org.delcom.watchlist.ui.viewmodels.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieEditScreen(
    authViewModel: AuthViewModel,
    movieId: String,
    movieViewModel: MovieViewModel,
    onNavigateBack: () -> Unit,
) {
    val authToken = authViewModel.authToken
    val detail by movieViewModel.detail.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var watchStatus by remember { mutableStateOf(WatchStatus.PLANNED) }
    var initialized by remember { mutableStateOf(false) }

    val isLoading = detail.edit is UiState.Loading

    // Load data awal dari ViewModel (sudah di-load oleh DetailScreen)
    LaunchedEffect(detail.movie) {
        if (!initialized && detail.movie is UiState.Success) {
            val m = (detail.movie as UiState.Success).data
            title = m.title
            description = m.cleanDescription
            releaseYear = m.releaseYear ?: ""
            watchStatus = m.watchStatus
            initialized = true
        }
    }

    // Jika belum ada data (masuk langsung ke EditScreen), load dulu
    LaunchedEffect(Unit) {
        if (authToken.isNotBlank() && detail.movie !is UiState.Success) {
            movieViewModel.loadMovieById(authToken, movieId)
        }
    }

    // Handle edit result
    LaunchedEffect(detail.edit) {
        when (val s = detail.edit) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("success|Film berhasil diperbarui")
                movieViewModel.loadMovieById(authToken, movieId)
                movieViewModel.resetEdit()
                onNavigateBack()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar("error|${s.message}")
                movieViewModel.resetEdit()
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
                },
            )
        },
    ) { padding ->
        if (!initialized) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul Film *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Contoh: Interstellar") },
            )
            OutlinedTextField(
                value = releaseYear,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) releaseYear = it },
                label = { Text("Tahun Rilis (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Contoh: 2014") },
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi / Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("Ceritakan sedikit tentang film ini...") },
            )
            Text("Status Tonton", style = MaterialTheme.typography.labelLarge)
            WatchStatusSelector(selectedStatus = watchStatus, onStatusSelected = { watchStatus = it })
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val finalDesc = buildString {
                        if (releaseYear.isNotBlank()) append("[$releaseYear] ")
                        append(description)
                    }.trim()
                    movieViewModel.editMovie(
                        authToken   = authToken,
                        movieId     = movieId,
                        title       = title.trim(),
                        description = finalDesc,
                        isDone      = watchStatus == WatchStatus.COMPLETED,
                        watchStatus = watchStatus.apiValue,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading && title.isNotBlank(),
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Simpan Perubahan")
            }
        }
    }
}