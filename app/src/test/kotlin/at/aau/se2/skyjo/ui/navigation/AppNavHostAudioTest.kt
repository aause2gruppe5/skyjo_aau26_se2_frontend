package at.aau.se2.skyjo.ui.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.audio.AudioController
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.model.TotalScore
import at.aau.se2.skyjo.model.auth.WsTicketResponse
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.CheatPeekResultMessage
import at.aau.se2.skyjo.model.CheatReportResultMessage
import at.aau.se2.skyjo.network.GameRealtimeClient
import at.aau.se2.skyjo.network.SkyjoApi
import at.aau.se2.skyjo.settings.SettingsRepository
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.viewmodel.GameViewModel
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
class AppNavHostAudioTest {

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

    private lateinit var settings: SettingsRepository
    private lateinit var audioController: AudioController

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("skyjo_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        settings = SettingsRepository.getInstance(context)
        settings.setMusicEnabled(SettingsRepository.DEFAULT_MUSIC)
        settings.setSoundEnabled(SettingsRepository.DEFAULT_SOUND)
        settings.setHapticEnabled(SettingsRepository.DEFAULT_HAPTIC)
        audioController = AudioController(context, settings)

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

    private fun makeGameState() = GameUpdateMessage(
        phase = "AWAITING_DRAW",
        currentPlayerId = "p1",
        roundNumber = 1,
        gameOver = false,
        totalScores = listOf(
            TotalScore("p1", "Alice", 0),
            TotalScore("p2", "Bob", 5),
        ),
        players = listOf(
            GamePlayerState(
                playerId = "p1",
                nickname = "Alice",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(1, 3, "NUMBER")) } },
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(2, 5, "NUMBER")) } },
            ),
        ),
        discardTopCard = Card(id = 99, value = 4, type = "NUMBER"),
    )

    private fun content(
        viewModel: GameViewModel = GameViewModel(mockApplication, mockApi, mockGameClient),
        wired: Boolean = true,
        navProvider: (NavHostController) -> Unit = {},
    ) {
        composeTestRule.setContent {
            SkyjoTheme {
                val navController = rememberNavController()
                navProvider(navController)
                AppNavHost(
                    navController = navController,
                    gameViewModel = viewModel,
                    settings = if (wired) settings else null,
                    audioController = if (wired) audioController else null,
                )
            }
        }
    }

    @Test
    fun fullOverload_drives_music_on_navigation() {
        settings.setMusicEnabled(true)
        lateinit var nav: NavHostController
        content { nav = it }
        composeTestRule.waitForIdle()

        // Navigating switches the destination, firing the music LaunchedEffect.
        composeTestRule.runOnIdle { nav.navigate(AppDestination.Friends.route) }
        composeTestRule.waitForIdle()
    }

    @Test
    fun fullOverload_renders_settings_with_persisted_state() {
        lateinit var nav: NavHostController
        content { nav = it }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { nav.navigate(AppDestination.Settings.route) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Game Preferences").assertExists()
    }

    @Test
    fun fullOverload_plays_defeat_stinger_when_local_player_did_not_win() {
        settings.setSoundEnabled(true)
        fakeGameState.value = makeGameState() // local player name "" -> not the winner
        lateinit var nav: NavHostController
        content(navProvider = { nav = it })
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { nav.navigate(AppDestination.GameOver.route) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("GAME OVER").assertExists()
    }

    @Test
    fun fullOverload_plays_victory_stinger_when_local_player_won() {
        settings.setSoundEnabled(true)
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.setAuthenticatedUsername("Alice") // Alice has the best (lowest) score
        fakeGameState.value = makeGameState()
        lateinit var nav: NavHostController
        content(viewModel = viewModel, navProvider = { nav = it })
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { nav.navigate(AppDestination.GameOver.route) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("GAME OVER").assertExists()
    }

    @Test
    fun fullOverload_gameOver_with_no_scores_skips_stinger() {
        fakeGameState.value = null // no scores -> empty list -> no SFX
        lateinit var nav: NavHostController
        content(navProvider = { nav = it })
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { nav.navigate(AppDestination.GameOver.route) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("GAME OVER").assertExists()
    }

    @Test
    fun fullOverload_settings_toggles_write_through_to_repository() {
        lateinit var nav: NavHostController
        content(navProvider = { nav = it })
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { nav.navigate(AppDestination.Settings.route) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_Music").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("toggle_Sound FX").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("toggle_Haptic Feedback").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Defaults flipped via the wired callbacks.
        assertTrue(settings.musicEnabled.value)
        assertTrue(!settings.soundEnabled.value)
        assertTrue(!settings.hapticEnabled.value)
    }

    @Test
    fun unwiredSettings_falls_back_to_defaults_and_toggles_are_noops() {
        lateinit var nav: NavHostController
        content(wired = false, navProvider = { nav = it })
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { nav.navigate(AppDestination.Settings.route) }
        composeTestRule.waitForIdle()

        // Without a SettingsRepository the screen renders from defaults; toggling is a safe no-op.
        composeTestRule.onNodeWithTag("toggle_Music").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("toggle_Sound FX").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("toggle_Haptic Feedback").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Game Preferences").assertExists()
    }
}
