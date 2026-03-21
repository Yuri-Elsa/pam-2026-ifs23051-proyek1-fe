package org.delcom.watchlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.delcom.watchlist.helper.RouteHelper
import org.delcom.watchlist.ui.components.WatchListSnackbar
import org.delcom.watchlist.ui.screens.auth.LoginScreen
import org.delcom.watchlist.ui.screens.auth.RegisterScreen
import org.delcom.watchlist.ui.screens.home.HomeScreen
import org.delcom.watchlist.ui.screens.home.ProfileScreen
import org.delcom.watchlist.ui.screens.movies.MovieAddScreen
import org.delcom.watchlist.ui.screens.movies.MovieDetailScreen
import org.delcom.watchlist.ui.screens.movies.MovieEditScreen
import org.delcom.watchlist.ui.screens.movies.MovieListScreen
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieViewModel
import org.delcom.watchlist.ui.viewmodels.ProfileViewModel
import org.delcom.watchlist.ui.viewmodels.UiState

@Composable
fun WatchListApp(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    movieViewModel: MovieViewModel,
    profileViewModel: ProfileViewModel,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val authState by authViewModel.uiState.collectAsState()

    // Redirect berdasarkan session state
    LaunchedEffect(authState.session) {
        when (authState.session) {
            is UiState.Error -> navController.navigate(RouteHelper.LOGIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            is UiState.Success -> {
                val route = navController.currentDestination?.route
                if (route == null || route == RouteHelper.LOGIN || route == RouteHelper.REGISTER) {
                    navController.navigate(RouteHelper.HOME) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            else -> {}
        }
    }

    // Loading splash saat mengecek token
    if (authState.session is UiState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                WatchListSnackbar(data) { snackbarHostState.currentSnackbarData?.dismiss() }
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = RouteHelper.HOME,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            composable(RouteHelper.LOGIN) {
                LoginScreen(navController, snackbarHostState, authViewModel)
            }
            composable(RouteHelper.REGISTER) {
                RegisterScreen(navController, snackbarHostState, authViewModel)
            }
            composable(RouteHelper.HOME) {
                HomeScreen(
                    navController  = navController,
                    authViewModel  = authViewModel,
                    movieViewModel = movieViewModel,
                )
            }
            composable(RouteHelper.PROFILE) {
                ProfileScreen(
                    navController    = navController,
                    authViewModel    = authViewModel,
                    profileViewModel = profileViewModel,
                )
            }
            composable(RouteHelper.MOVIES) {
                MovieListScreen(
                    navController  = navController,
                    authViewModel  = authViewModel,
                    movieViewModel = movieViewModel,
                )
            }
            composable(RouteHelper.MOVIE_ADD) {
                MovieAddScreen(
                    authViewModel  = authViewModel,
                    movieViewModel = movieViewModel,
                    navController  = navController,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route     = RouteHelper.MOVIE_DETAIL,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
            ) { backStack ->
                val movieId = backStack.arguments?.getString("movieId") ?: ""
                MovieDetailScreen(
                    navController  = navController,
                    snackbarHost   = snackbarHostState,
                    authViewModel  = authViewModel,
                    movieViewModel = movieViewModel,
                    movieId        = movieId,
                )
            }
            composable(
                route     = RouteHelper.MOVIE_EDIT,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
            ) { backStack ->
                val movieId = backStack.arguments?.getString("movieId") ?: ""
                MovieEditScreen(
                    authViewModel  = authViewModel,
                    movieId        = movieId,
                    movieViewModel = movieViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}