package org.delcom.watchlist.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.ui.theme.CinemaDark
import org.delcom.watchlist.ui.theme.CinemaGold
import org.delcom.watchlist.ui.theme.CinemaRed
import org.delcom.watchlist.ui.viewmodels.AuthUIState
import org.delcom.watchlist.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(uiState.auth) {
        if (!isLoading) return@LaunchedEffect
        when (val s = uiState.auth) {
            is AuthUIState.Error -> {
                isLoading = false
                errorMessage = s.message
            }
            is AuthUIState.Success -> { /* NavHost handles redirect */ }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(CinemaRed.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = CinemaRed,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "WatchList",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )
            Text(
                "Catat perjalanan sinematikmu",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f)),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                LoginForm(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onClearError = { errorMessage = "" },
                    onLogin = { u, p ->
                        errorMessage = ""
                        // Validasi client-side
                        when {
                            u.isBlank() -> { errorMessage = "Username tidak boleh kosong"; return@LoginForm }
                            p.isBlank() -> { errorMessage = "Password tidak boleh kosong"; return@LoginForm }
                        }
                        isLoading = true
                        authViewModel.login(u, p)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = {
                navController.navigate(RouteHelper.REGISTER) { launchSingleTop = true }
            }) {
                Text("Belum punya akun? ", color = Color.White.copy(alpha = 0.6f))
                Text("Daftar", color = CinemaGold, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LoginForm(
    isLoading: Boolean,
    errorMessage: String,
    onClearError: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val passFocus = remember { FocusRequester() }
    val focusMgr = LocalFocusManager.current

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Masuk",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        // Error banner
        if (errorMessage.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFD32F2F),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearError, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                }
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; onClearError() },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = CinemaRed) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = cinemaTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passFocus.requestFocus() })
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onClearError() },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = CinemaRed) },
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(if (showPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.White.copy(0.5f))
                }
            },
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(passFocus),
            shape = RoundedCornerShape(12.dp),
            colors = cinemaTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                focusMgr.clearFocus()
                onLogin(username, password)
            })
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { onLogin(username, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Masuk", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun cinemaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = CinemaRed,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    cursorColor = CinemaRed,
    focusedLabelColor = CinemaRed,
    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)