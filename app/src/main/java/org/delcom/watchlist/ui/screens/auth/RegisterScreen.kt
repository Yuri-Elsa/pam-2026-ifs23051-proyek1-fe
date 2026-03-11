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
import org.delcom.watchlist.ui.theme.CinemaGold
import org.delcom.watchlist.ui.theme.CinemaRed
import org.delcom.watchlist.ui.viewmodels.AuthActionUIState
import org.delcom.watchlist.ui.viewmodels.AuthViewModel

@Composable
fun RegisterScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()
    val scope   = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { authViewModel.resetRegisterState() }

    LaunchedEffect(uiState.authRegister) {
        if (!isLoading) return@LaunchedEffect
        when (val s = uiState.authRegister) {
            is AuthActionUIState.Success -> {
                isLoading = false
                navController.navigate(RouteHelper.LOGIN) {
                    popUpTo(RouteHelper.REGISTER) { inclusive = true }; launchSingleTop = true
                }
            }
            is AuthActionUIState.Error -> {
                isLoading = false
                scope.launch { snackbarHost.showSnackbar("error|${s.message}") }
            }
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
            Spacer(Modifier.height(64.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CinemaRed.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MovieCreation, null, tint = CinemaRed, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("WatchList", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp))
            Text("Buat akun baru", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f)), textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                RegisterForm(isLoading = isLoading, onRegister = { name, username, password ->
                    isLoading = true
                    authViewModel.register(name, username, password)
                })
            }

            Spacer(Modifier.height(20.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Sudah punya akun? ", color = Color.White.copy(alpha = 0.6f))
                Text("Masuk", color = CinemaGold, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RegisterForm(isLoading: Boolean, onRegister: (String, String, String) -> Unit) {
    var name     by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val (uFocus, pFocus) = remember { FocusRequester() to FocusRequester() }
    val focusMgr = LocalFocusManager.current

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Daftar", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))

        OutlinedTextField(value = name, onValueChange = { name = it },
            label = { Text("Nama Lengkap") },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = CinemaRed) },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = cinemaTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { uFocus.requestFocus() })
        )

        OutlinedTextField(value = username, onValueChange = { username = it },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = CinemaRed) },
            singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(uFocus), shape = RoundedCornerShape(12.dp),
            colors = cinemaTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { pFocus.requestFocus() })
        )

        OutlinedTextField(value = password, onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = CinemaRed) },
            trailingIcon = { IconButton(onClick = { showPass = !showPass }) { Icon(if (showPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.White.copy(0.5f)) } },
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(pFocus), shape = RoundedCornerShape(12.dp),
            colors = cinemaTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { focusMgr.clearFocus(); if (name.isNotBlank() && username.isNotBlank() && password.isNotBlank()) onRegister(name, username, password) })
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { if (name.isNotBlank() && username.isNotBlank() && password.isNotBlank()) onRegister(name, username, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && name.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Daftar", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
