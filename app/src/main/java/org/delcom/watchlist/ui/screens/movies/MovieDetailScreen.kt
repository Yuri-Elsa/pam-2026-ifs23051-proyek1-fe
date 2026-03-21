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
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import org.delcom.watchlist.helper.ImageCompressHelper
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.helper.ToolsHelper
import org.delcom.watchlist.network.data.ResponseMovieData
import org.delcom.watchlist.ui.components.*
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieViewModel
import org.delcom.watchlist.ui.viewmodels.UiState

@Composable
fun MovieDetailScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    movieViewModel: MovieViewModel,
    movieId: String,
) {
    val authToken = authViewModel.authToken
    val detail by movieViewModel.detail.collectAsState()

    var coverTs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val movie = (detail.movie as? UiState.Success)?.data
    val isLoading = detail.movie is UiState.Loading

    LaunchedEffect(Unit) {
        if (authToken.isBlank()) {
            navController.navigate(RouteHelper.LOGIN) { popUpTo(0) { inclusive = true } }
            return@LaunchedEffect
        }
        movieViewModel.resetDetail()
        movieViewModel.loadMovieById(authToken, movieId)
    }

    LaunchedEffect(detail.movie) {
        if (detail.movie is UiState.Error) navController.popBackStack()
    }

    // Handle delete
    LaunchedEffect(detail.delete) {
        when (val s = detail.delete) {
            is UiState.Success -> {
                movieViewModel.resetDelete()
                navController.navigate(RouteHelper.MOVIES) {
                    popUpTo(RouteHelper.MOVIES) { inclusive = true }
                }
            }
            is UiState.Error -> {
                snackbarHost.showSnackbar("error|${s.message}")
                movieViewModel.resetDelete()
            }
            else -> {}
        }
    }

    // Handle cover upload
    LaunchedEffect(detail.cover) {
        when (val s = detail.cover) {
            is UiState.Success -> {
                snackbarHost.showSnackbar("success|Poster berhasil diperbarui")
                coverTs = System.currentTimeMillis()
                movieViewModel.resetCover()
                movieViewModel.loadMovieById(authToken, movieId)
            }
            is UiState.Error -> {
                snackbarHost.showSnackbar("error|${s.message}")
                movieViewModel.resetCover()
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
        ),
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WatchListTopBar(
            title = movie.title,
            navController = navController,
            showBackButton = true,
            showMenu = true,
            menuItems = menuItems,
        )

        Box(modifier = Modifier.weight(1f)) {
            MovieDetailContent(
                movie = movie,
                coverTs = coverTs,
                onChangeCover = { uri, context ->
                    val part = ImageCompressHelper.uriToCompressedMultipart(context, uri, "file")
                    movieViewModel.uploadCover(authToken, movie.id, part)
                },
            )
        }

        BottomNavComponent(navController)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Film") },
            text = { Text("Yakin ingin menghapus \"${movie.title}\" dari watchlist?") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; movieViewModel.deleteMovie(authToken, movieId) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            },
        )
    }
}

// ── Detail Content ────────────────────────────────────────────────────────────

@Composable
private fun MovieDetailContent(
    movie: ResponseMovieData,
    coverTs: Long,
    onChangeCover: (Uri, android.content.Context) -> Unit,
) {
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showCoverConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { pendingUri = it; showCoverConfirm = true }
    }

    val coverUrl = remember(movie.cover, coverTs) {
        ToolsHelper.getMovieImageUrl(movie.cover, coverTs.toString())
    }

    val coverImageRequest = remember(coverUrl) {
        if (coverUrl == null) null
        else ImageRequest.Builder(context)
            .data(coverUrl)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .crossfade(true)
            .build()
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // Hero poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        ) {
            when {
                pendingUri != null -> SubcomposeAsyncImage(
                    model = pendingUri,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                        is AsyncImagePainter.State.Error -> LargePlaceholderPoster()
                        else -> SubcomposeAsyncImageContent()
                    }
                }

                coverUrl != null -> key(coverTs) {
                    SubcomposeAsyncImage(
                        model = coverImageRequest,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                            is AsyncImagePainter.State.Error -> LargePlaceholderPoster()
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                }

                else -> LargePlaceholderPoster()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Ganti Poster", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Action bar pending cover
        if (pendingUri != null) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Poster baru dipilih", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pendingUri = null }) { Text("Batal") }
                        Button(onClick = { showCoverConfirm = true }) { Text("Simpan") }
                    }
                }
            }
        }

        // Info
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(movie.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                if (movie.releaseYear != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Text(movie.releaseYear!!, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }

            WatchStatusBadge(status = movie.watchStatus)

            if (movie.cleanDescription.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Deskripsi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Text(movie.cleanDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, lineHeight = 22.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaInfoItem("Ditambahkan", movie.createdAt.take(10), Modifier.weight(1f))
                MetaInfoItem("Terakhir diubah", movie.updatedAt.take(10), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

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
            },
        )
    }
}

@Composable
private fun LargePlaceholderPoster() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
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