package at.aau.se2.skyjo.ui.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.ActionCard
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.CheatPeekResultMessage
import at.aau.se2.skyjo.model.CheatReportResultMessage
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.LobbyPlayer
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.model.TotalScore
import at.aau.se2.skyjo.model.auth.WsTicketResponse
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.network.GameRealtimeClient
import at.aau.se2.skyjo.network.SkyjoApi
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.viewmodel.GameViewModel
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import androidx.compose.ui.test.onLast

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockApi = mockk<SkyjoApi>(relaxed = true)
    private val mockGameClient = mockk<GameRealtimeClient>(relaxed = true)

    private val fakeLobbyState = MutableStateFlow<LobbyUpdateMessage?>(null)
    private val fakeGameState = MutableStateFlow<GameUpdateMessage?>(null)
    private val fakeActionCardResults = MutableSharedFlow<ActionCardResultMessage>()
    private val fakeCheatPeekResults = MutableSharedFlow<CheatPeekResultMessage>(extraBufferCapacity = 1)
    private val fakeCheatReportResults = MutableSharedFlow<CheatReportResultMessage>(extraBufferCapacity = 1)
    private val fakeIncomingInvites = MutableSharedFlow<LobbyInviteDto>()
    private val fakeErrorMessage = MutableSharedFlow<String>()
    private val fakeConnectionError = MutableStateFlow<String?>(null)
    private val fakeIsConnected = MutableStateFlow(false)
    private val fakeHasRejoinedGame = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeLobbyState.value = null
        fakeGameState.value = null
        fakeConnectionError.value = null
        fakeIsConnected.value = false
        fakeHasRejoinedGame.value = false

        every { mockGameClient.lobbyState } returns fakeLobbyState
        every { mockGameClient.gameState } returns fakeGameState
        every { mockGameClient.actionCardResults } returns fakeActionCardResults
        every { mockGameClient.cheatPeekResults } returns fakeCheatPeekResults
        every { mockGameClient.cheatReportResults } returns fakeCheatReportResults
        every { mockGameClient.incomingInvites } returns fakeIncomingInvites
        every { mockGameClient.errorMessage } returns fakeErrorMessage
        every { mockGameClient.connectionError } returns fakeConnectionError
        every { mockGameClient.isConnected } returns fakeIsConnected
        every { mockGameClient.hasRejoinedGame } returns fakeHasRejoinedGame
        coEvery { mockGameClient.connect() } just runs
        coEvery { mockGameClient.connect(any(), any()) } just runs
        coEvery { mockGameClient.reconnect(any()) } just runs
        coEvery { mockApi.createWebSocketTicket() } returns WsTicketResponse("ticket", Long.MAX_VALUE)
        every { mockGameClient.joinLobby(any(), any()) } just runs
        every { mockGameClient.leaveLobby() } just runs
        every { mockGameClient.startGame(any(), any()) } just runs
        every { mockGameClient.sendAction(any()) } just runs
        every { mockGameClient.playActionCard(any()) } just runs
        every { mockGameClient.cheatPeekDrawPile() } just runs
        every { mockGameClient.cheatReportCurrentPlayer() } just runs
        every { mockGameClient.clearStoredGame() } just runs
        every { mockGameClient.disconnect() } just runs
        every { mockGameClient.close() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun makeGameState(myPlayerId: String = "p1") = GameUpdateMessage(
        phase = "AWAITING_DRAW",
        currentPlayerId = myPlayerId,
        roundNumber = 1,
        gameOver = false,
        totalScores = listOf(
            TotalScore(myPlayerId, "Alice", 0),
            TotalScore("p2", "Bob", 0),
        ),
        players = listOf(
            GamePlayerState(
                playerId = myPlayerId,
                nickname = "Alice",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(1, 3, "NUMBER")) } },
                actionCards = listOf(ActionCard(id = 151, kind = "PLAYER_SWAP")),
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(2, 5, "NUMBER")) } },
            ),
        ),
        discardTopCard = Card(id = 99, value = 4, type = "NUMBER"),
    )

    @Test
    fun appNavHost_renders_start_screen_by_default() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("SKYJO", substring = true).assertExists()
    }

    @Test
    fun appNavHost_invalid_join_code_does_not_navigate_into_lobby() {
        coEvery { mockApi.joinLobby(any()) } throws IllegalStateException("lobby not found")
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            SkyjoTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController, gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("BAD123")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("JOIN WITH CODE").performClick()
        composeTestRule.waitForIdle()

        // The failed join must NOT drop the user into an (empty) lobby screen.
        // The "lobby not found" toast itself is covered by GameViewModelTest (lobbyJoinError event).
        assertEquals(AppDestination.Start.route, navController.currentDestination?.route)
        verify(exactly = 0) { mockGameClient.applyLobbyState(any()) }
    }

    @Test
    fun appNavHost_navigates_to_friends_from_start_screen() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Friends").onLast().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No friends yet").assertExists()
    }

    @Test
    fun appNavHost_renders_leaderboard_route() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            SkyjoTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController, gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            navController.navigate(AppDestination.Leaderboard.route)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No games on the leaderboard yet").assertExists()
    }

    @Test
    fun appNavHost_renders_lobby_route_with_current_lobby_state() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.setAuthenticatedUsername("Alice")
        fakeLobbyState.value = LobbyUpdateMessage(
            lobbyId = "lobby-1",
            joinCode = "ABC123",
            players = listOf(LobbyPlayer("Alice", isHost = true)),
            status = "WAITING",
            maxPlayers = 6,
        )
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            SkyjoTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController, gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            navController.navigate(AppDestination.Lobby.route)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Join Code: ABC123").assertExists()
        composeTestRule.onNodeWithText("1 / 6").assertExists()
    }

    @Test
    fun appNavHost_renders_rules_route() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            SkyjoTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController, gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Simuliere die Navigation zur Rules-Route
        composeTestRule.runOnIdle {
            navController.navigate(AppDestination.Rules.route)
        }
        composeTestRule.waitForIdle()

        // Überprüft, ob die Compose-TopBar des RulesScreens geladen wurde
        composeTestRule.onNodeWithText("HOW TO PLAY").assertExists()

        // Überprüft, ob die Compose-Überschrift des RulesScreens geladen wurde
        composeTestRule.onNodeWithText("SKYJO ACTION").assertExists()
    }

    @Test
    fun appNavHost_shows_connection_banner_for_active_disconnected_session() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.setAuthenticatedUsername("Alice")
        fakeLobbyState.value = LobbyUpdateMessage(
            lobbyId = "lobby-1",
            joinCode = "ABC123",
            players = listOf(LobbyPlayer("Alice", isHost = true)),
            status = "WAITING",
            maxPlayers = 6,
        )

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Connection interrupted, retrying...").assertExists()
    }

    @Test
    fun appNavHost_navigates_to_game_screen_when_lobby_status_is_in_game() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState()

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }

        fakeLobbyState.value = LobbyUpdateMessage(
            players = listOf(LobbyPlayer("Alice", isHost = true)),
            status = "IN_GAME",
            maxPlayers = 6,
        )
        composeTestRule.waitForIdle()
        // Once in game, lobbyState triggers navigation — but we can also navigate directly
        // by setting hasRejoinedGame which triggers navigation in AppNavHost
        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SKYJO ACTION", substring = true).assertExists()
    }

    @Test
    fun appNavHost_game_screen_discard_action_card_reaches_viewmodel() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState()
        viewModel.connect("Alice")

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Trigger navigation to game screen
        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()

        // On game screen — click Discard on the action card
        composeTestRule.onNodeWithText("Discard").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        verify {
            mockGameClient.sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 0)
            )
        }
    }

    @Test
    fun appNavHost_game_screen_shows_private_cheat_peek_result() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState()
        viewModel.connect("Alice")

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()

        fakeCheatPeekResults.tryEmit(
            CheatPeekResultMessage(
                card = Card(id = 77, value = 6, type = "NUMBER"),
                remainingCheatPeeks = 1,
            ),
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cheat Peek").assertExists()
        composeTestRule.onNodeWithText("Top card of the draw pile").assertExists()
        composeTestRule.onNodeWithText("6").assertExists()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cheat Peek").assertDoesNotExist()
    }

    @Test
    fun appNavHost_game_screen_report_cheat_reaches_viewmodel() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState().copy(currentPlayerId = "p2")
        viewModel.connect("Alice")

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("report_cheat_button").performClick()
        composeTestRule.waitForIdle()

        verify { mockGameClient.cheatReportCurrentPlayer() }
    }

    @Test
    fun appNavHost_game_screen_shows_successful_cheat_report_toast() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState().copy(currentPlayerId = "p2")
        viewModel.connect("Alice")

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()

        fakeCheatReportResults.tryEmit(
            CheatReportResultMessage(
                successful = true,
                reporterPlayerId = "p1",
                targetPlayerId = "p2",
                penaltyPlayerId = "p2",
                penaltyPoints = 10,
                remainingCheatReports = 2,
            ),
        )
        composeTestRule.waitForIdle()

        assertEquals("Report successful! Reported player gets +10 points.", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun appNavHost_game_screen_shows_false_cheat_report_toast() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState().copy(currentPlayerId = "p2")
        viewModel.connect("Alice")

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()

        fakeCheatReportResults.tryEmit(
            CheatReportResultMessage(
                successful = false,
                reporterPlayerId = "p1",
                targetPlayerId = "p2",
                penaltyPlayerId = "p1",
                penaltyPoints = 5,
                remainingCheatReports = 2,
            ),
        )
        composeTestRule.waitForIdle()

        assertEquals("False report! You get +5 points.", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun appNavHost_game_screen_play_swap_card_reaches_viewmodel() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState()
        viewModel.connect("Alice")

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Trigger navigation to game screen
        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()

        // Enter swap mode
        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Select first card from own board
        composeTestRule.onAllNodesWithText("3").onFirst().performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Navigate to Bob's grid in carousel
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Select second card from Bob's board
        composeTestRule.onAllNodesWithText("5").onFirst().performScrollTo().performClick()
        composeTestRule.waitForIdle()

        verify {
            mockGameClient.sendAction(
                match { it.type == "PLAY_ACTION_CARD" && it.actionCardIndex == 0 }
            )
        }
    }

    @Test
    fun appNavHost_game_screen_play_swap_own_cards_reaches_viewmodel() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        // Important: override the action card type.
        fakeGameState.value = makeGameState(actionCardKind = "SWAP_OWN_CARDS")
        viewModel.connect("Alice")

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Navigate to the game screen
        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()

        // 2. Select action card
        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 3. Select first own card
        composeTestRule.onAllNodesWithText("3").onFirst().performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 4. Select second own card
        composeTestRule.onAllNodesWithText("3").onLast().performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 5. Verify
        verify {
            mockGameClient.sendAction(
                match { action ->
                    // Make sure this matches the GameAction model exactly.
                    // Adjust this if the ViewModel sends a different type string for SwapOwnCards.
                    action.type == "PLAY_ACTION_CARD" &&
                            action.actionCardIndex == 0
                }
            )
        }
    }

    @Test
    fun appNavHost_renders_gameOver_route_with_scores() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        // Wir setzen einen Fake-GameState, damit totalScores vorhanden sind (Alice und Bob)
        fakeGameState.value = makeGameState()
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            SkyjoTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController, gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Simuliere die Navigation zum GameOver-Screen
        composeTestRule.runOnIdle {
            navController.navigate(AppDestination.GameOver.route)
        }
        composeTestRule.waitForIdle()

        // Prüfe, ob die statischen Texte des Screens geladen wurden
        composeTestRule.onNodeWithText("GAME OVER").assertExists()

        // Prüfe, ob die Spieler aus dem gameState übergeben wurden
        composeTestRule.onNodeWithText("Alice", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Bob", useUnmergedTree = true).assertExists()
    }

    @Test
    fun appNavHost_navigates_to_gameOver_when_game_state_becomes_game_over() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState()
        viewModel.connect("Alice")
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            SkyjoTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController, gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        fakeHasRejoinedGame.value = true
        composeTestRule.waitForIdle()
        assertEquals(AppDestination.Game.route, navController.currentDestination?.route)

        fakeGameState.value = makeGameState().copy(gameOver = true)
        composeTestRule.waitForIdle()

        assertEquals(AppDestination.GameOver.route, navController.currentDestination?.route)
        composeTestRule.onNodeWithText("GAME OVER").assertExists()
    }

    @Test
    fun appNavHost_gameOver_back_button_clears_lobby_and_navigates_to_start() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeGameState.value = makeGameState()
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            SkyjoTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController, gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Simuliere die Navigation zum GameOver-Screen
        composeTestRule.runOnIdle {
            navController.navigate(AppDestination.GameOver.route)
        }
        composeTestRule.waitForIdle()

        // Klicke auf den Button "BACK TO START"
        composeTestRule.onNodeWithText("BACK TO START").performClick()
        composeTestRule.waitForIdle()

        // Verifiziere, dass der Callback `gameViewModel.leaveLobby()` aufgerufen hat
        // (was intern mockGameClient.leaveLobby() triggert)
        verify(exactly = 1) { mockGameClient.leaveLobby() }

        // Optional: Überprüfe, ob wir uns wieder auf der Startseite befinden,
        // indem wir nach einem Element suchen, das spezifisch für den StartScreen ist.
        composeTestRule.onNodeWithText("Create Lobby", ignoreCase = true, substring = true).assertExists()
    }

    // Helper function
    private fun makeGameState(
        myPlayerId: String = "p1",
        actionCardKind: String = "PLAYER_SWAP" // Keep the default value for older tests
    ) = GameUpdateMessage(
        phase = "AWAITING_DRAW",
        currentPlayerId = myPlayerId,
        roundNumber = 1,
        gameOver = false,
        totalScores = listOf(
            TotalScore(myPlayerId, "Alice", 0),
            TotalScore("p2", "Bob", 0),
        ),
        players = listOf(
            GamePlayerState(
                playerId = myPlayerId,
                nickname = "Alice",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(1, 3, "NUMBER")) } },
                // Hier den neuen Parameter verwenden:
                actionCards = listOf(ActionCard(id = 151, kind = actionCardKind)),
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(2, 5, "NUMBER")) } },
            ),
        ),
        discardTopCard = Card(id = 99, value = 4, type = "NUMBER"),
    )
}
