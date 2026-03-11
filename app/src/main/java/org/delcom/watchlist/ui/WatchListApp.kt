package org.delcom.watchlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.delcom.watchlist.ui.viewmodels.AuthUIState
import org.delcom.watchlist.ui.viewmodels.AuthViewModel
import org.delcom.watchlist.ui.viewmodels.MovieViewModel

@Composable
fun WatchListApp(
    navController: NavHostController = rememberNavController(),
    movieViewModel: MovieViewModel,
    authViewModel: AuthViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiStateAuth by authViewModel.uiState.collectAsState()

    val authState = uiStateAuth.auth
    val authToken = (authState as? AuthUIState.Success)?.data?.authToken ?: ""

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUIState.Error -> navController.navigate(RouteHelper.LOGIN) {
                popUpTo(0) { inclusive = true }; launchSingleTop = true
            }
            is AuthUIState.Success -> {
                val route = navController.currentDestination?.route
                if (route == null || route == RouteHelper.LOGIN || route == RouteHelper.REGISTER) {
                    navController.navigate(RouteHelper.HOME) {
                        popUpTo(0) { inclusive = true }; launchSingleTop = true
                    }
                }
            }
            else -> {}
        }
    }

    if (authState is AuthUIState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            composable(RouteHelper.LOGIN) {
                LoginScreen(navController, snackbarHostState, authViewModel)
            }
            composable(RouteHelper.REGISTER) {
                RegisterScreen(navController, snackbarHostState, authViewModel)
            }
            composable(RouteHelper.HOME) {
                HomeScreen(navController, authToken, movieViewModel)
            }
            composable(RouteHelper.PROFILE) {
                ProfileScreen(navController, authToken, movieViewModel, authViewModel)
            }
            composable(RouteHelper.MOVIES) {
                MovieListScreen(
                    navController  = navController,
                    authToken      = authToken,
                    movieViewModel = movieViewModel,
                )
            }
            composable(RouteHelper.MOVIE_ADD) {
                MovieAddScreen(
                    authToken      = authToken,
                    movieViewModel = movieViewModel,
                    navController  = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route     = RouteHelper.MOVIE_DETAIL,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { back ->
                val movieId = back.arguments?.getString("movieId") ?: ""
                MovieDetailScreen(
                    navController  = navController,
                    snackbarHost   = snackbarHostState,
                    authViewModel  = authViewModel,
                    movieViewModel = movieViewModel,
                    movieId        = movieId
                )
            }
            composable(
                route     = RouteHelper.MOVIE_EDIT,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { back ->
                val movieId = back.arguments?.getString("movieId") ?: ""
                MovieEditScreen(
                    authToken      = authToken,
                    movieId        = movieId,
                    movieViewModel = movieViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
