package at.aau.se2.skyjo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    gameViewModel: GameViewModel,
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
                onPlayClicked = { playerName ->
                    gameViewModel.connect(playerName)
                    navController.navigate(AppDestination.Lobby.route)
                },
                onNavigate = navigateMain,
            )
        }

        composable(AppDestination.Lobby.route) {
            val lobbyState by gameViewModel.lobbyState.collectAsState()
            val isHost by gameViewModel.isHost.collectAsState()

            // Auto-navigate when the host starts the game
            LaunchedEffect(lobbyState?.status) {
                if (lobbyState?.status == "IN_GAME") {
                    navController.navigate(AppDestination.Game.route) {
                        popUpTo(AppDestination.Lobby.route) { inclusive = true }
                    }
                }
            }

            LobbyScreen(
                players = lobbyState?.players ?: emptyList(),
                maxPlayers = lobbyState?.maxPlayers ?: 6,
                isHost = isHost,
                onStartGame = { maxRounds ->
                    gameViewModel.startGame(maxRounds = maxRounds)
                },
                onBack = {
                    gameViewModel.leaveLobby()
                    navController.popBackStack()
                },
            )
        }

        composable(AppDestination.Game.route) {
            val gameState by gameViewModel.gameState.collectAsState()
            val myPlayerId by gameViewModel.myPlayerId.collectAsState()
            val isMyTurn by gameViewModel.isMyTurn.collectAsState()

            GameScreen(
                gameState = gameState,
                myPlayerId = myPlayerId,
                isMyTurn = isMyTurn,
                onDrawFromDeck = { gameViewModel.drawFromDeck() },
                onDrawFromDiscard = { gameViewModel.drawFromDiscard() },
                onReplaceCard = { row, col -> gameViewModel.replaceCard(row, col) },
                onDiscardAndReveal = { row, col -> gameViewModel.discardAndReveal(row, col) },
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
