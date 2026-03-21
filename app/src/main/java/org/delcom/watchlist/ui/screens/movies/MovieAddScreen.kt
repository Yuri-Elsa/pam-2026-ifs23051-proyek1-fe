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
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieViewModel
import org.delcom.watchlist.ui.viewmodels.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieAddScreen(
    authViewModel: AuthViewModel,
    movieViewModel: MovieViewModel,
    navController: NavHostController,
    onNavigateBack: () -> Unit,
) {
    val authToken = authViewModel.authToken
    val addState by movieViewModel.addState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var watchStatus by remember { mutableStateOf(WatchStatus.PLANNED) }

    val isLoading = addState is UiState.Loading

    LaunchedEffect(addState) {
        when (val s = addState) {
            is UiState.Success -> {
                navController.previousBackStackEntry?.savedStateHandle?.set("movie_added", true)
                movieViewModel.resetAdd()
                onNavigateBack()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar("error|${s.message}")
                movieViewModel.resetAdd()
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
                title = { Text("Tambah Film") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
            )
        },
    ) { padding ->
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
                    movieViewModel.addMovie(authToken, title.trim(), finalDesc, watchStatus.apiValue)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading && title.isNotBlank(),
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Simpan ke Watchlist")
            }
        }
    }
}