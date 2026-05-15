package at.aau.se2.skyjo.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import at.aau.se2.skyjo.model.ActionCardResultMessage
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

    val isConnected by gameViewModel.isConnected.collectAsState()
    val myPlayerName by gameViewModel.myPlayerName.collectAsState()
    val hasRejoinedGame by gameViewModel.hasRejoinedGame.collectAsState()

    LaunchedEffect(hasRejoinedGame) {
        if (hasRejoinedGame && navController.currentDestination?.route != AppDestination.Game.route) {
            navController.navigate(AppDestination.Game.route) {
                popUpTo(AppDestination.Start.route) { inclusive = false }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Start.route,
            modifier = Modifier.fillMaxSize(),
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
                var privateActionCardResult by remember {
                    mutableStateOf<ActionCardResultMessage?>(null)
                }

                LaunchedEffect(Unit) {
                    gameViewModel.actionCardResults.collect { result ->
                        privateActionCardResult = result
                    }
                }

                GameScreen(
                    gameState = gameState,
                    myPlayerId = myPlayerId,
                    isMyTurn = isMyTurn,
                    privateActionCardResult = privateActionCardResult,
                    onDrawFromDeck = { gameViewModel.drawFromDeck() },
                    onDrawFromDiscard = { gameViewModel.drawFromDiscard() },
                    onDrawFromActionDeck = { gameViewModel.drawFromActionDeck() },
                    onDrawVisibleActionCard = { index -> gameViewModel.drawVisibleActionCard(index) },
                    onReplaceCard = { row, col -> gameViewModel.replaceCard(row, col) },
                    onDiscardAndReveal = { row, col -> gameViewModel.discardAndReveal(row, col) },
                    onPlayActionCard = { command -> gameViewModel.playActionCard(command) },
                    onDismissActionCardResult = { privateActionCardResult = null },
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

        if (!isConnected && myPlayerName.isNotEmpty()) {
            Text(
                text = "Verbindung unterbrochen, versuche erneut…",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color(0xFFB00020))
                    .padding(vertical = 6.dp, horizontal = 16.dp),
            )
        }
    }
}
