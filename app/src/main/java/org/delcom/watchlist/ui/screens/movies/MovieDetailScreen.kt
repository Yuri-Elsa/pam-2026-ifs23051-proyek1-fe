package org.delcom.watchlist.ui.screens.movies

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImageContent
import org.delcom.watchlist.helper.ImageCompressHelper
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.helper.ToolsHelper
import org.delcom.watchlist.network.data.ResponseMovieData
import org.delcom.watchlist.ui.components.*
import org.delcom.watchlist.ui.viewmodels.AuthUIState
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieActionUIState
import org.delcom.watchlist.ui.viewmodels.MovieUIState
import org.delcom.watchlist.ui.viewmodels.MovieViewModel

@Composable
fun MovieDetailScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    movieViewModel: MovieViewModel,
    movieId: String
) {
    val uiState    by movieViewModel.uiState.collectAsState()
    val uiStateAuth by authViewModel.uiState.collectAsState()

    val authToken = remember(uiStateAuth.auth) {
        (uiStateAuth.auth as? AuthUIState.Success)?.data?.authToken
    }

    var isLoading        by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val movie = (uiState.movie as? MovieUIState.Success)?.data

    // ── Load movie ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            navController.navigate(RouteHelper.LOGIN) { popUpTo(0) { inclusive = true } }
            return@LaunchedEffect
        }
        movieViewModel.resetMovieDetailState()
        val token = authToken ?: run { navController.popBackStack(); return@LaunchedEffect }
        movieViewModel.getMovieById(token, movieId)
    }

    LaunchedEffect(uiState.movie) {
        when (uiState.movie) {
            is MovieUIState.Success -> isLoading = false
            is MovieUIState.Error   -> { isLoading = false; navController.popBackStack() }
            is MovieUIState.Loading -> isLoading = true
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────
    fun onDelete() {
        val token = authToken ?: return
        isLoading = true
        movieViewModel.deleteMovie(token, movieId)
    }

    LaunchedEffect(uiState.movieDelete) {
        when (val s = uiState.movieDelete) {
            is MovieActionUIState.Success -> {
                movieViewModel.resetMovieDeleteState()
                navController.navigate(RouteHelper.MOVIES) { popUpTo(RouteHelper.MOVIES) { inclusive = true } }
            }
            is MovieActionUIState.Error -> {
                snackbarHost.showSnackbar("error|${s.message}")
                isLoading = false
            }
            is MovieActionUIState.Loading -> isLoading = true
            else -> {}
        }
    }

    // ── Cover change ───────────────────────────────────────────────────────────
    LaunchedEffect(uiState.movieChangeCover) {
        when (val s = uiState.movieChangeCover) {
            is MovieActionUIState.Success -> {
                snackbarHost.showSnackbar("success|Poster berhasil diperbarui")
                authToken?.let { movieViewModel.getMovieById(it, movieId) }
                isLoading = false
            }
            is MovieActionUIState.Error -> {
                snackbarHost.showSnackbar("error|${s.message}")
                isLoading = false
            }
            else -> {}
        }
    }

    if (isLoading || movie == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val menuItems = listOf(
        TopBarMenuItem(
            text = "Edit Film",
            icon = Icons.Default.Edit,
            onClick = { navController.navigate(RouteHelper.movieEdit(movie.id)) }
        ),
        TopBarMenuItem(
            text = "Hapus Film",
            icon = Icons.Default.Delete,
            onClick = { showDeleteDialog = true },
            isDestructive = true
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WatchListTopBar(
            title = movie.title,
            navController = navController,
            showBackButton = true,
            showMenu = true,
            menuItems = menuItems
        )

        Box(modifier = Modifier.weight(1f)) {
            MovieDetailContent(
                movie = movie,
                onChangeCover = { uri, context ->
                    val token = authToken ?: return@MovieDetailContent
                    isLoading = true
                    val part = ImageCompressHelper.uriToCompressedMultipart(context, uri, "file")
                    movieViewModel.putMovieCover(token, movie.id, part)
                }
            )
        }

        BottomNavComponent(navController = navController)
    }

    // ── Delete dialog ──────────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Film") },
            text = { Text("Yakin ingin menghapus \"${movie.title}\" dari watchlist?") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun MovieDetailContent(
    movie: ResponseMovieData,
    onChangeCover: (Uri, android.content.Context) -> Unit
) {
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showCoverConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pendingUri = it; showCoverConfirm = true }
    }

    val status = movie.watchStatus
    val coverUrl = ToolsHelper.getMovieImage(movie.id, movie.updatedAt)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero poster ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
        ) {
            if (movie.cover != null) {
                SubcomposeAsyncImage(
                    model = pendingUri ?: coverUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is AsyncImagePainter.State.Error -> LargePlaceholderPoster()
                        else -> SubcomposeAsyncImageContent()
                    }
                }
            } else {
                if (pendingUri != null) {
                    SubcomposeAsyncImage(
                        model = pendingUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                } else {
                    LargePlaceholderPoster()
                }
            }

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
            )

            // Camera icon hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Ganti Poster", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // ── Pending cover save bar ─────────────────────────────────────────────
        if (pendingUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Poster baru dipilih", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pendingUri = null }) { Text("Batal") }
                        Button(onClick = { showCoverConfirm = true }) { Text("Simpan") }
                    }
                }
            }
        }

        // ── Info card ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title + Year
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (movie.releaseYear != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = movie.releaseYear!!,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Watch status
            WatchStatusBadge(status = status)

            // Description
            if (movie.cleanDescription.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Deskripsi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = movie.cleanDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 22.sp
                    )
                }
            }

            // Meta info
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetaInfoItem(
                    label = "Ditambahkan",
                    value = movie.createdAt.take(10),
                    modifier = Modifier.weight(1f)
                )
                MetaInfoItem(
                    label = "Terakhir diubah",
                    value = movie.updatedAt.take(10),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Cover save confirm dialog
    if (showCoverConfirm && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showCoverConfirm = false },
            title = { Text("Ganti Poster?") },
            text = { Text("Poster akan dikompres sebelum diunggah. Lanjutkan?") },
            confirmButton = {
                Button(onClick = {
                    showCoverConfirm = false
                    pendingUri?.let { onChangeCover(it, context) }
                    pendingUri = null
                }) { Text("Ya, Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showCoverConfirm = false; pendingUri = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun LargePlaceholderPoster() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
            Text("Tap untuk menambahkan poster", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MetaInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
    }
}
