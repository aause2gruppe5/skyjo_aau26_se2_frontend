package at.aau.se2.skyjo.ui.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.LobbyPlayer
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.model.TotalScore
import at.aau.se2.skyjo.network.GameStompClient
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.viewmodel.GameViewModel
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApplication = mockk<Application>(relaxed = true)

    private val fakeLobbyState = MutableStateFlow<LobbyUpdateMessage?>(null)
    private val fakeGameState = MutableStateFlow<GameUpdateMessage?>(null)
    private val fakeErrorMessage = MutableSharedFlow<String>()
    private val fakeConnectionError = MutableStateFlow<String?>(null)
    private val fakeIsConnected = MutableStateFlow(false)
    private val fakeHasRejoinedGame = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkConstructor(GameStompClient::class)
        every { anyConstructed<GameStompClient>().lobbyState } returns fakeLobbyState
        every { anyConstructed<GameStompClient>().gameState } returns fakeGameState
        every { anyConstructed<GameStompClient>().errorMessage } returns fakeErrorMessage
        every { anyConstructed<GameStompClient>().connectionError } returns fakeConnectionError
        every { anyConstructed<GameStompClient>().isConnected } returns fakeIsConnected
        every { anyConstructed<GameStompClient>().hasRejoinedGame } returns fakeHasRejoinedGame
        coEvery { anyConstructed<GameStompClient>().connect() } just runs
        coEvery { anyConstructed<GameStompClient>().reconnect(any()) } just runs
        every { anyConstructed<GameStompClient>().joinLobby(any(), any()) } just runs
        every { anyConstructed<GameStompClient>().leaveLobby() } just runs
        every { anyConstructed<GameStompClient>().startGame(any(), any()) } just runs
        every { anyConstructed<GameStompClient>().sendAction(any()) } just runs
        every { anyConstructed<GameStompClient>().disconnect() } just runs
        every { anyConstructed<GameStompClient>().close() } just runs
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
                actionCardTypes = listOf("PLAYER_SWAP"),
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
        val viewModel = GameViewModel(mockApplication)
        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("SKYJO", substring = true).assertExists()
    }

    @Test
    fun appNavHost_navigates_to_game_screen_when_lobby_status_is_in_game() {
        val viewModel = GameViewModel(mockApplication)
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
        val viewModel = GameViewModel(mockApplication)
        fakeGameState.value = makeGameState()
        fakeHasRejoinedGame.value = true

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // On game screen — click Discard on the action card
        composeTestRule.onNodeWithText("Discard").performClick()
        composeTestRule.waitForIdle()

        verify {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 0)
            )
        }
    }

    @Test
    fun appNavHost_game_screen_play_swap_card_reaches_viewmodel() {
        val viewModel = GameViewModel(mockApplication)
        fakeGameState.value = makeGameState()
        fakeHasRejoinedGame.value = true

        composeTestRule.setContent {
            SkyjoTheme {
                AppNavHost(navController = rememberNavController(), gameViewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Enter swap mode
        composeTestRule.onNodeWithText("Play").performClick()
        composeTestRule.waitForIdle()

        // Select first card from own board
        composeTestRule.onAllNodesWithText("3").onFirst().performClick()
        composeTestRule.waitForIdle()

        // Select second card from Bob's board
        composeTestRule.onAllNodesWithText("5").onFirst().performClick()
        composeTestRule.waitForIdle()

        verify {
            anyConstructed<GameStompClient>().sendAction(
                match { it.type == "PLAY_ACTION_CARD" && it.actionCardIndex == 0 }
            )
        }
    }
}
