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
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import org.delcom.watchlist.BuildConfig
import org.delcom.watchlist.helper.ImageCompressHelper
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.ui.components.BottomNavComponent
import org.delcom.watchlist.ui.components.WatchListTopBar
import org.delcom.watchlist.ui.theme.CinemaRed
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.ProfileViewModel
import org.delcom.watchlist.ui.viewmodels.UiState

@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
) {
    val authToken = authViewModel.authToken

    if (authToken.isBlank()) {
        LaunchedEffect(Unit) {
            navController.navigate(RouteHelper.LOGIN) { popUpTo(0) { inclusive = true } }
        }
        return
    }

    val context = LocalContext.current
    val profileState by profileViewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var photoTs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showPwSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var logoutTriggered by remember { mutableStateOf(false) }

    val authState by authViewModel.uiState.collectAsState()
    val profile = (profileState.profile as? UiState.Success)?.data

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { pendingUri = it; showPhotoDialog = true }
    }

    LaunchedEffect(Unit) { profileViewModel.loadProfile(authToken) }

    // Handle upload foto
    LaunchedEffect(profileState.photo) {
        when (profileState.photo) {
            is UiState.Success -> {
                photoTs = System.currentTimeMillis()
                pendingUri = null
                profileViewModel.resetPhoto()
                profileViewModel.loadProfile(authToken)
            }
            is UiState.Error -> {
                snackbar.showSnackbar("error|${(profileState.photo as UiState.Error).message}")
                profileViewModel.resetPhoto()
            }
            else -> {}
        }
    }

    // Handle logout
    LaunchedEffect(authState.logout) {
        if (!logoutTriggered) return@LaunchedEffect
        when (authState.logout) {
            is UiState.Success, is UiState.Error ->
                navController.navigate(RouteHelper.LOGIN) { popUpTo(0) { inclusive = true } }
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WatchListTopBar(title = "Profil", showBackButton = false, navController = navController)

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // Avatar
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                ) {
                    val profileId = profile?.id
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
                            contentScale = ContentScale.Crop,
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading -> Box(
                                    modifier = Modifier.size(100.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp, color = CinemaRed) }

                                is AsyncImagePainter.State.Error -> Box(
                                    modifier = Modifier.size(100.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Person, null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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

                    if (profileState.photo is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(108.dp), strokeWidth = 3.dp, color = CinemaRed)
                    }
                }

                if (profile != null) {
                    Text(profile.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                    Text("@${profile.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CircularProgressIndicator(color = CinemaRed)
                }

                // Tentang
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Tentang", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                            IconButton(onClick = { profileViewModel.resetAbout(); showAboutSheet = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, null, tint = CinemaRed, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            profile?.about?.takeIf { it.isNotBlank() } ?: "Belum ada info tentang kamu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (profile?.about.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                HorizontalDivider()
                OutlinedButton(
                    onClick = { profileViewModel.resetUpdate(); showEditSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Edit Profil (Nama & Username)") }
                OutlinedButton(
                    onClick = { profileViewModel.resetPassword(); showPwSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Ubah Kata Sandi") }
                HorizontalDivider()

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
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

    // Dialog konfirmasi foto
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false; pendingUri = null },
            title = { Text("Ganti Foto Profil") },
            text = { Text("Foto akan dikompres sebelum diunggah. Lanjutkan?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingUri?.let {
                        profileViewModel.uploadPhoto(
                            authToken,
                            ImageCompressHelper.uriToCompressedMultipart(context, it, "file")
                        )
                    }
                    showPhotoDialog = false
                }) { Text("Ya", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false; pendingUri = null }) { Text("Batal") }
            }
        )
    }

    // Dialog konfirmasi logout
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

    // Bottom sheets
    if (showEditSheet) {
        EditProfileSheet(
            initialName = profile?.name ?: "",
            initialUsername = profile?.username ?: "",
            state = profileState.update,
            onDismiss = { showEditSheet = false; profileViewModel.resetUpdate() },
            onSave = { n, u -> profileViewModel.updateProfile(authToken, n, u) },
            onSuccess = {
                showEditSheet = false
                profileViewModel.loadProfile(authToken)
                profileViewModel.resetUpdate()
            },
        )
    }
    if (showPwSheet) {
        ChangePasswordSheet(
            state = profileState.password,
            onDismiss = { showPwSheet = false; profileViewModel.resetPassword() },
            onSave = { old, new -> profileViewModel.updatePassword(authToken, old, new) },
            onSuccess = { showPwSheet = false; profileViewModel.resetPassword() },
        )
    }
    if (showAboutSheet) {
        EditAboutSheet(
            initialAbout = profile?.about ?: "",
            state = profileState.about,
            onDismiss = { showAboutSheet = false; profileViewModel.resetAbout() },
            onSave = { profileViewModel.updateAbout(authToken, it) },
            onSuccess = {
                showAboutSheet = false
                profileViewModel.loadProfile(authToken)
                profileViewModel.resetAbout()
            },
        )
    }
}

// ── Bottom Sheets ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    initialName: String,
    initialUsername: String,
    state: org.delcom.watchlist.ui.viewmodels.ActionState,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onSuccess: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var username by remember { mutableStateOf(initialUsername) }
    LaunchedEffect(state) { if (state is UiState.Success) onSuccess() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Edit Profil", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(name, { name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Button(
                onClick = { if (name.isNotBlank() && username.isNotBlank()) onSave(name, username) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
            ) {
                if (state is UiState.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSheet(
    state: org.delcom.watchlist.ui.viewmodels.ActionState,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onSuccess: () -> Unit,
) {
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    LaunchedEffect(state) { if (state is UiState.Success) onSuccess() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ubah Kata Sandi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(oldPw, { oldPw = it }, label = { Text("Sandi Lama") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(newPw, { newPw = it }, label = { Text("Sandi Baru") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(confirm, { confirm = it }, label = { Text("Konfirmasi Sandi Baru") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), isError = err.isNotEmpty())
            if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    if (newPw != confirm) { err = "Konfirmasi tidak cocok"; return@Button }
                    err = ""
                    onSave(oldPw, newPw)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
            ) {
                if (state is UiState.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAboutSheet(
    initialAbout: String,
    state: org.delcom.watchlist.ui.viewmodels.ActionState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onSuccess: () -> Unit,
) {
    var about by remember { mutableStateOf(initialAbout) }
    LaunchedEffect(state) { if (state is UiState.Success) onSuccess() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Edit Tentang", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(about, { about = it }, label = { Text("Tentang kamu") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(12.dp))
            Button(
                onClick = { onSave(about) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
            ) {
                if (state is UiState.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        }
    }
}