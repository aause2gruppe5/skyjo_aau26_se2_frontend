package at.aau.se2.skyjo.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.CheatPeekResultMessage
import at.aau.se2.skyjo.ui.screens.auth.AuthScreen
import at.aau.se2.skyjo.ui.screens.friends.FriendsScreen
import at.aau.se2.skyjo.ui.screens.game.GameScreen
import at.aau.se2.skyjo.ui.screens.leaderboard.LeaderboardScreen
import at.aau.se2.skyjo.ui.screens.lobby.LobbyScreen
import at.aau.se2.skyjo.ui.screens.gameOver.GameOverScreen
import at.aau.se2.skyjo.ui.screens.settings.SettingsScreen
import at.aau.se2.skyjo.ui.screens.start.StartScreen
import at.aau.se2.skyjo.viewmodel.AuthViewModel
import at.aau.se2.skyjo.viewmodel.AuthUiState
import at.aau.se2.skyjo.viewmodel.FriendsUiState
import at.aau.se2.skyjo.viewmodel.FriendsViewModel
import at.aau.se2.skyjo.viewmodel.GameViewModel
import at.aau.se2.skyjo.viewmodel.LeaderboardUiState
import at.aau.se2.skyjo.viewmodel.LeaderboardViewModel
import at.aau.se2.skyjo.ui.screens.rules.RulesScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel? = null,
    gameViewModel: GameViewModel,
    friendsViewModel: FriendsViewModel? = null,
    leaderboardViewModel: LeaderboardViewModel? = null,
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
    val currentLobbyState by gameViewModel.lobbyState.collectAsState()
    val currentGameState by gameViewModel.gameState.collectAsState()
    val fallbackAuthState = remember { mutableStateOf(AuthUiState(isCheckingSession = false, isAuthenticated = true)) }
    val fallbackFriendsState = remember { mutableStateOf(FriendsUiState()) }
    val fallbackLeaderboardState = remember { mutableStateOf(LeaderboardUiState()) }
    val authState = authViewModel?.state?.collectAsState()?.value ?: fallbackAuthState.value
    val homeStats by gameViewModel.homeStats.collectAsState()
    val friendsState = friendsViewModel?.state?.collectAsState()?.value ?: fallbackFriendsState.value
    val leaderboardState = leaderboardViewModel?.state?.collectAsState()?.value ?: fallbackLeaderboardState.value
    val isHost by gameViewModel.isHost.collectAsState()

    LaunchedEffect(hasRejoinedGame) {
        if (hasRejoinedGame && navController.currentDestination?.route != AppDestination.Game.route) {
            navController.navigate(AppDestination.Game.route) {
                popUpTo(AppDestination.Start.route) { inclusive = false }
            }
        }
    }

    LaunchedEffect(Unit) {
        gameViewModel.incomingInvites.collect { invite ->
            friendsViewModel?.addLobbyInvite(invite)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Auth.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppDestination.Auth.route) {
                LaunchedEffect(authState.isAuthenticated) {
                    if (authState.isAuthenticated) {
                        authState.user?.username?.let(gameViewModel::setAuthenticatedUsername)
                        navController.navigate(AppDestination.Start.route) {
                            popUpTo(AppDestination.Auth.route) { inclusive = true }
                        }
                    }
                }
                AuthScreen(
                    state = authState,
                    onUsernameChange = { authViewModel?.updateUsername(it) },
                    onPasswordChange = { authViewModel?.updatePassword(it) },
                    onSubmit = { authViewModel?.submit() },
                    onToggleMode = { authViewModel?.toggleMode() },
                )
            }

            composable(AppDestination.Start.route) {
                val username = authState.user?.username.orEmpty()
                LaunchedEffect(username) {
                    if (username.isNotBlank()) {
                        gameViewModel.setAuthenticatedUsername(username)
                        gameViewModel.refreshHomeStats()
                    }
                }
                StartScreen(
                    onPlayClicked = { playerName ->
                        gameViewModel.connect(playerName)
                        navController.navigate(AppDestination.Lobby.route)
                    },
                    onNavigate = navigateMain,
                    username = username,
                    stats = homeStats,
                    onCreateLobby = {
                        gameViewModel.createLobby(username)
                        navController.navigate(AppDestination.Lobby.route)
                    },
                    onJoinLobby = { code ->
                        gameViewModel.joinLobbyByCode(username, code)
                        navController.navigate(AppDestination.Lobby.route)
                    },
                    onLogout = {
                        authViewModel?.logout()
                        gameViewModel.leaveLobby()
                        navController.navigate(AppDestination.Auth.route) {
                            popUpTo(AppDestination.Start.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(AppDestination.Lobby.route) {
                val lobbyState by gameViewModel.lobbyState.collectAsState()
                val lobbyError by gameViewModel.lobbyError.collectAsState()
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
                    joinCode = lobbyState?.joinCode,
                    errorMessage = lobbyError,
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
                val context = LocalContext.current
                val gameState by gameViewModel.gameState.collectAsState()
                val myPlayerId by gameViewModel.myPlayerId.collectAsState()
                val isMyTurn by gameViewModel.isMyTurn.collectAsState()
                var privateActionCardResult by remember {
                    mutableStateOf<ActionCardResultMessage?>(null)
                }
                var privateCheatPeekResult by remember {
                    mutableStateOf<CheatPeekResultMessage?>(null)
                }

                LaunchedEffect(Unit) {
                    gameViewModel.actionCardResults.collect { result ->
                        privateActionCardResult = result
                    }
                }
                LaunchedEffect(Unit) {
                    gameViewModel.cheatPeekResults.collect { result ->
                        privateCheatPeekResult = result
                    }
                }
                LaunchedEffect(context) {
                    gameViewModel.cheatReportResults.collect { result ->
                        val message = if (result.successful) {
                            "Report successful! Reported player gets +${result.penaltyPoints} points."
                        } else {
                            "False report! You get +${result.penaltyPoints} points."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                GameScreen(
                    gameState = gameState,
                    myPlayerId = myPlayerId,
                    isMyTurn = isMyTurn,
                    isHost = isHost,
                    privateActionCardResult = privateActionCardResult,
                    privateCheatPeekResult = privateCheatPeekResult,
                    onDrawFromDeck = { gameViewModel.drawFromDeck() },
                    onDrawFromDiscard = { gameViewModel.drawFromDiscard() },
                    onDrawFromActionDeck = { gameViewModel.drawFromActionDeck() },
                    onDrawVisibleActionCard = { index -> gameViewModel.drawVisibleActionCard(index) },
                    onReplaceCard = { row, col -> gameViewModel.replaceCard(row, col) },
                    onDiscardAndReveal = { row, col -> gameViewModel.discardAndReveal(row, col) },
                    onPlayPlayerSwapCard = { cardIdx, p1Id, p1Row, p1Col, p2Id, p2Row, p2Col ->
                        gameViewModel.playPlayerSwapCard(cardIdx, p1Id, p1Row, p1Col, p2Id, p2Row, p2Col)
                    },
                    onPlaySwapOwnCards = { cardIdx, firstRow, firstCol, secondRow, secondCol ->
                        gameViewModel.playSwapOwnCards(cardIdx, firstRow, firstCol, secondRow, secondCol)
                    },
                    onPlayActionCard = { index -> gameViewModel.playActionCard(index) },
                    onPlayEnlightenmentCard = { command -> gameViewModel.playActionCard(command) },
                    onDiscardActionCard = { index -> gameViewModel.discardActionCard(index) },
                    onDismissActionCardResult = { privateActionCardResult = null },
                    onCheatPeekDrawPile = { gameViewModel.cheatPeekDrawPile() },
                    onReportCheat = { gameViewModel.cheatReportCurrentPlayer() },
                    onDismissCheatPeekResult = { privateCheatPeekResult = null },
                    onReadyForNextRoundClick = { gameViewModel.startNextRound() },
                    onBack = { navController.popBackStack() },
                    onNavigateToGameOver = {
                        navController.navigate(AppDestination.GameOver.route) {
                            // Das Spiel wird aus dem Verlauf gelöscht,
                            // damit man mit "Zurück" nicht ins beendete Spiel kommt.
                            popUpTo(AppDestination.Game.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(AppDestination.Friends.route) {
                LaunchedEffect(Unit) { friendsViewModel?.refresh() }
                FriendsScreen(
                    onNavigate = navigateMain,
                    friends = friendsState.friends,
                    incomingRequests = friendsState.incomingRequests,
                    lobbyInvites = friendsState.lobbyInvites,
                    searchResults = friendsState.searchResults,
                    query = friendsState.query,
                    activeLobbyId = gameViewModel.lobbyState.collectAsState().value?.lobbyId,
                    onQueryChange = { friendsViewModel?.updateSearch(it) },
                    onSendRequest = { friendsViewModel?.sendFriendRequest(it) },
                    onAcceptRequest = { friendsViewModel?.acceptRequest(it) },
                    onDeclineRequest = { friendsViewModel?.declineRequest(it) },
                    onInviteFriend = { friendUserId ->
                        friendsViewModel?.inviteFriend(gameViewModel.lobbyState.value?.lobbyId, friendUserId)
                    },
                    onAcceptInvite = { inviteId ->
                        friendsViewModel?.removeLobbyInvite(inviteId)
                        gameViewModel.acceptLobbyInvite(username = authState.user?.username.orEmpty(), inviteId = inviteId)
                        navController.navigate(AppDestination.Lobby.route)
                    },
                    onDeclineInvite = { inviteId ->
                        friendsViewModel?.declineLobbyInvite(inviteId)
                    },
                )
            }

            composable(AppDestination.Leaderboard.route) {
                LaunchedEffect(Unit) { leaderboardViewModel?.refresh() }
                LeaderboardScreen(onNavigate = navigateMain, entries = leaderboardState.entries)
            }

            composable(AppDestination.Settings.route) {
                SettingsScreen(onNavigate = navigateMain)
            }

            composable(AppDestination.Rules.route) {
                RulesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppDestination.GameOver.route) {
                // 1. Wir holen uns den aktuellen State aus dem ViewModel
                val gameState by gameViewModel.gameState.collectAsState()
                fun exitGameOver() {
                    // Optional: Aufräumen, falls der ViewModel-State zurückgesetzt werden muss
                    gameViewModel.leaveLobby()

                    // Navigation zurück zum Start
                    navController.navigate(AppDestination.Start.route) {
                        // Löscht alles bis zum Start-Screen aus der Back-History
                        popUpTo(AppDestination.Start.route) { inclusive = true }
                        // Verhindert, dass der Start-Screen mehrfach auf dem Stack liegt
                        launchSingleTop = true
                    }
                }

                BackHandler(onBack = ::exitGameOver)

                // 2. Screen aufrufen und die Scores übergeben
                GameOverScreen(
                    totalScores = gameState?.totalScores ?: emptyList(),
                    onBackToStart = ::exitGameOver
                )
            }

        }

        if (!isConnected && myPlayerName.isNotEmpty() && (currentLobbyState != null || currentGameState != null)) {
            Text(
                text = "Connection interrupted, retrying...",
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
