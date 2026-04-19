package at.aau.se2.skyjo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import at.aau.se2.skyjo.ui.screens.friends.FriendsScreen
import at.aau.se2.skyjo.ui.screens.game.GameScreen
import at.aau.se2.skyjo.ui.screens.lobby.LobbyScreen
import at.aau.se2.skyjo.ui.screens.settings.SettingsScreen
import at.aau.se2.skyjo.ui.screens.start.StartScreen
import at.aau.se2.skyjo.viewmodel.GameViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel
) {
    val navigateMain: (AppDestination) -> Unit = { dest ->
        navController.navigate(dest.route) {
            popUpTo(AppDestination.Start.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Start.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Start.route) {
            StartScreen(
                onPlayClicked = { navController.navigate(AppDestination.Lobby.route) },
                onNavigate = navigateMain,
            )
        }
        composable(AppDestination.Lobby.route) {
            LobbyScreen(
                onStartGame = { navController.navigate(AppDestination.Game.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppDestination.Game.route) {
            GameScreen(
                onBack = { navController.popBackStack() })
        }
        composable(AppDestination.Friends.route) {
            FriendsScreen(onNavigate = navigateMain)
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(onNavigate = navigateMain)
        }
    }
}
