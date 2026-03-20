package org.delcom.watchlist.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import org.delcom.watchlist.BuildConfig
import org.delcom.watchlist.helper.ImageCompressHelper
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.ui.components.BottomNavComponent
import org.delcom.watchlist.ui.components.WatchListTopBar
import org.delcom.watchlist.ui.theme.CinemaRed
import org.delcom.watchlist.ui.viewmodels.*

@Composable
fun ProfileScreen(
    navController: NavHostController,
    authToken: String,
    movieViewModel: MovieViewModel,
    authViewModel: AuthViewModel
) {
    if (authToken.isBlank()) {
        LaunchedEffect(Unit) { navController.navigate(RouteHelper.LOGIN) { popUpTo(0) { inclusive = true } } }
        return
    }

    val uiState     by movieViewModel.uiState.collectAsState()
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val context     = LocalContext.current
    val snackbar    = remember { SnackbarHostState() }
    // Gunakan Long sebagai cache-buster, update setiap kali upload foto sukses
    var photoTs     by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingUri  by remember { mutableStateOf<Uri?>(null) }
    var showPhotoDialog  by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditSheet    by remember { mutableStateOf(false) }
    var showPwSheet      by remember { mutableStateOf(false) }
    var showAboutSheet   by remember { mutableStateOf(false) }
    var logoutTriggered  by remember { mutableStateOf(false) }

    val profile = (uiState.profile as? ProfileUIState.Success)?.data

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { pendingUri = it; showPhotoDialog = true }
    }

    LaunchedEffect(Unit) { movieViewModel.getProfile(authToken) }

    LaunchedEffect(uiState.profilePhoto) {
        when (val s = uiState.profilePhoto) {
            is MovieActionUIState.Success -> {
                // Update timestamp untuk force reload — ini KUNCI agar Coil fetch ulang
                photoTs = System.currentTimeMillis()
                pendingUri = null
                // Reset state DULU sebelum getProfile agar tidak loop
                movieViewModel.resetProfilePhotoState()
                movieViewModel.getProfile(authToken)
            }
            is MovieActionUIState.Error -> {
                snackbar.showSnackbar("error|${s.message}")
                movieViewModel.resetProfilePhotoState()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiStateAuth.authLogout) {
        if (!logoutTriggered) return@LaunchedEffect
        when (uiStateAuth.authLogout) {
            is AuthLogoutUIState.Success, is AuthLogoutUIState.Error ->
                navController.navigate(RouteHelper.LOGIN) { popUpTo(0) { inclusive = true } }
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WatchListTopBar(title = "Profil", showBackButton = false, showMenu = false, navController = navController)

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Avatar — key berubah setiap photoTs berubah, memaksa rekomposisi + request baru
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                ) {
                    val profileId = profile?.id
                    // key() memastikan SubcomposeAsyncImage di-recreate saat photoTs berubah
                    key(photoTs) {
                        SubcomposeAsyncImage(
                            model = if (profileId != null) {
                                ImageRequest.Builder(context)
                                    .data("${BuildConfig.BASE_URL}images/users/$profileId?t=$photoTs")
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .diskCachePolicy(CachePolicy.DISABLED)
                                    .crossfade(true)
                                    .build()
                            } else null,
                            contentDescription = "Foto Profil",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 2.dp,
                                            color = CinemaRed
                                        )
                                    }
                                }
                                is AsyncImagePainter.State.Error -> {
                                    // Tampilkan placeholder jika gagal load
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                else -> SubcomposeAsyncImageContent()
                            }
                        }
                    }

                    Surface(shape = CircleShape, color = CinemaRed, modifier = Modifier.size(28.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Loading overlay saat upload sedang berjalan
                    if (uiState.profilePhoto is MovieActionUIState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(108.dp),
                            strokeWidth = 3.dp,
                            color = CinemaRed
                        )
                    }
                }

                if (profile != null) {
                    Text(profile.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                    Text("@${profile.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CircularProgressIndicator(color = CinemaRed)
                }

                // About card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Tentang", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                            IconButton(onClick = { movieViewModel.resetProfileAboutState(); showAboutSheet = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, null, tint = CinemaRed, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            profile?.about?.takeIf { it.isNotBlank() } ?: "Belum ada info tentang kamu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (profile?.about.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider()
                OutlinedButton(onClick = { movieViewModel.resetProfileUpdateState(); showEditSheet = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Edit Profil (Nama & Username)")
                }
                OutlinedButton(onClick = { movieViewModel.resetProfilePasswordState(); showPwSheet = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Ubah Kata Sandi")
                }
                HorizontalDivider()

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Keluar", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
        BottomNavComponent(navController)
    }

    // Photo confirm dialog
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false; pendingUri = null },
            title = { Text("Ganti Foto Profil") },
            text = { Text("Foto akan dikompres sebelum diunggah. Lanjutkan?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingUri?.let {
                        movieViewModel.updatePhoto(authToken, ImageCompressHelper.uriToCompressedMultipart(context, it, "file"))
                    }
                    showPhotoDialog = false
                }) { Text("Ya", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false; pendingUri = null }) { Text("Batal") }
            }
        )
    }

    // Logout confirm dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar") },
            text = { Text("Yakin ingin keluar dari akun ini?") },
            confirmButton = {
                TextButton(onClick = {
                    logoutTriggered = true
                    authViewModel.logout(authToken)
                    showLogoutDialog = false
                }) { Text("Ya, Keluar", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showEditSheet) EditProfileSheet(
        profile?.name ?: "", profile?.username ?: "",
        onDismiss = { showEditSheet = false; movieViewModel.resetProfileUpdateState() },
        onSave = { n, u -> movieViewModel.updateProfile(authToken, n, u) },
        state = uiState.profileUpdate,
        onSuccess = { showEditSheet = false; movieViewModel.getProfile(authToken); movieViewModel.resetProfileUpdateState() }
    )
    if (showPwSheet) ChangePasswordSheet(
        onDismiss = { showPwSheet = false; movieViewModel.resetProfilePasswordState() },
        onSave = { o, n -> movieViewModel.updatePassword(authToken, o, n) },
        state = uiState.profilePassword,
        onSuccess = { showPwSheet = false; movieViewModel.resetProfilePasswordState() }
    )
    if (showAboutSheet) EditAboutSheet(
        profile?.about ?: "",
        onDismiss = { showAboutSheet = false; movieViewModel.resetProfileAboutState() },
        onSave = { movieViewModel.updateAbout(authToken, it) },
        state = uiState.profileAbout,
        onSuccess = { showAboutSheet = false; movieViewModel.getProfile(authToken); movieViewModel.resetProfileAboutState() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(name: String, username: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit, state: MovieActionUIState, onSuccess: () -> Unit) {
    var n by remember { mutableStateOf(name) }
    var u by remember { mutableStateOf(username) }
    LaunchedEffect(state) { if (state is MovieActionUIState.Success) onSuccess() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Edit Profil", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(n, { n = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(u, { u = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Button(
                onClick = { if (n.isNotBlank() && u.isNotBlank()) onSave(n, u) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is MovieActionUIState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
            ) {
                if (state is MovieActionUIState.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSheet(onDismiss: () -> Unit, onSave: (String, String) -> Unit, state: MovieActionUIState, onSuccess: () -> Unit) {
    var oldPw   by remember { mutableStateOf("") }
    var newPw   by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var err     by remember { mutableStateOf("") }
    LaunchedEffect(state) { if (state is MovieActionUIState.Success) onSuccess() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ubah Kata Sandi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(oldPw, { oldPw = it }, label = { Text("Sandi Lama") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(newPw, { newPw = it }, label = { Text("Sandi Baru") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(confirm, { confirm = it }, label = { Text("Konfirmasi Sandi Baru") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), isError = err.isNotEmpty())
            if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { if (newPw != confirm) { err = "Konfirmasi tidak cocok"; return@Button }; err = ""; onSave(oldPw, newPw) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is MovieActionUIState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
            ) {
                if (state is MovieActionUIState.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAboutSheet(about: String, onDismiss: () -> Unit, onSave: (String) -> Unit, state: MovieActionUIState, onSuccess: () -> Unit) {
    var a by remember { mutableStateOf(about) }
    LaunchedEffect(state) { if (state is MovieActionUIState.Success) onSuccess() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Edit Tentang", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(a, { a = it }, label = { Text("Tentang kamu") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(12.dp))
            Button(
                onClick = { onSave(a) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is MovieActionUIState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
            ) {
                if (state is MovieActionUIState.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        }
    }
}