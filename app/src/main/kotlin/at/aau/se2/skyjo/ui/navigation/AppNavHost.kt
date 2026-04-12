package at.aau.se2.skyjo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import at.aau.se2.skyjo.ui.game.GameViewModel
import at.aau.se2.skyjo.ui.screens.friends.FriendsScreen
import at.aau.se2.skyjo.ui.screens.game.GameScreen
import at.aau.se2.skyjo.ui.screens.lobby.LobbyScreen
import at.aau.se2.skyjo.ui.screens.settings.SettingsScreen
import at.aau.se2.skyjo.ui.screens.start.StartScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.state.collectAsState()

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
                gameViewModel = gameViewModel,
                onStartGame = { navController.navigate(AppDestination.Game.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppDestination.Game.route) {
            GameScreen(
                gameState = gameState,
                onDrawFromDeck = { gameViewModel.drawFromDeck() },
                onTakeDiscardCard = { gameViewModel.takeDiscardCard() },
                onReplaceDrawnCard = { pos -> gameViewModel.replaceDrawnCard(pos) },
                onDiscardAndReveal = { pos -> gameViewModel.discardAndReveal(pos) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppDestination.Friends.route) {
            FriendsScreen(onNavigate = navigateMain)
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(onNavigate = navigateMain)
        }
    }
}
