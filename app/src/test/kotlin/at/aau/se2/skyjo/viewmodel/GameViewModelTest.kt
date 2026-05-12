package at.aau.se2.skyjo.viewmodel

import android.app.Application
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.LobbyPlayer
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.network.GameStompClient
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

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

    @Test
    fun `init does not connect automatically`() {
        GameViewModel(mockApplication)
        verify(exactly = 0) { anyConstructed<GameStompClient>().joinLobby(any(), any()) }
    }

    @Test
    fun `myPlayerName is empty initially`() {
        val viewModel = GameViewModel(mockApplication)
        assertEquals("", viewModel.myPlayerName.value)
    }

    @Test
    fun `connect sets playerName`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.connect("TestPlayer")
        assertEquals("TestPlayer", viewModel.myPlayerName.value)
    }

    @Test
    fun `isHost is false when lobby is empty`() {
        val viewModel = GameViewModel(mockApplication)
        assertFalse(viewModel.isHost.value)
    }

    @Test
    fun `isHost is true when player is first in lobby`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.connect("Alice")
        fakeLobbyState.value = LobbyUpdateMessage(
            players = listOf(LobbyPlayer("Alice", isHost = true)),
            status = "WAITING",
            maxPlayers = 6,
        )
        assertTrue(viewModel.isHost.value)
    }

    @Test
    fun `isHost is false when player is not host`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.connect("Bob")
        fakeLobbyState.value = LobbyUpdateMessage(
            players = listOf(
                LobbyPlayer("Alice", isHost = true),
                LobbyPlayer("Bob", isHost = false),
            ),
            status = "WAITING",
            maxPlayers = 6,
        )
        assertFalse(viewModel.isHost.value)
    }

    @Test
    fun `leaveLobby delegates to client`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.leaveLobby()
        verify(exactly = 1) { anyConstructed<GameStompClient>().leaveLobby() }
    }

    @Test
    fun `startGame delegates to client with correct params`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.startGame(maxRounds = 5, targetScore = 100)
        verify(exactly = 1) { anyConstructed<GameStompClient>().startGame(5, 100) }
    }

    @Test
    fun `startGame with default params uses correct defaults`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.startGame()
        verify(exactly = 1) { anyConstructed<GameStompClient>().startGame(3, 100) }
    }

    @Test
    fun `drawFromDeck sends DRAW DECK action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.drawFromDeck()
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DRAW", source = "DECK")
            )
        }
    }

    @Test
    fun `drawFromDiscard sends DRAW DISCARD action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.drawFromDiscard()
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DRAW", source = "DISCARD")
            )
        }
    }

    @Test
    fun `replaceCard sends REPLACE action with correct coordinates`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.replaceCard(row = 1, col = 2)
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "REPLACE", row = 1, col = 2)
            )
        }
    }

    @Test
    fun `discardAndReveal sends DISCARD_AND_REVEAL action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.discardAndReveal(row = 0, col = 3)
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DISCARD_AND_REVEAL", row = 0, col = 3)
            )
        }
    }

    @Test
    fun `drawFromActionDeck sends DRAW ACTION_DECK action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.drawFromActionDeck()
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DRAW", source = "ACTION_DECK")
            )
        }
    }

    @Test
    fun `reconnect is triggered when connection drops with player name set`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.connect("Alice")

        fakeIsConnected.value = true
        fakeIsConnected.value = false

        coVerify(atLeast = 1) { anyConstructed<GameStompClient>().reconnect("Alice") }
    }

    @Test
    fun `reconnect is not triggered when player name is empty`() {
        GameViewModel(mockApplication)

        fakeIsConnected.value = true
        fakeIsConnected.value = false

        coVerify(exactly = 0) { anyConstructed<GameStompClient>().reconnect(any()) }
    }

    @Test
    fun `onCleared calls close`() {
        val viewModel = GameViewModel(mockApplication)
        val method = GameViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)
        verify(exactly = 1) { anyConstructed<GameStompClient>().close() }
    }
}
