package at.aau.se2.skyjo.viewmodel

import android.app.Application
import at.aau.se2.skyjo.model.ActionCardParameters
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.LobbyPlayer
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.model.PlayActionCardCommand
import at.aau.se2.skyjo.model.TotalScore
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
    private val fakeActionCardResults = MutableSharedFlow<ActionCardResultMessage>()
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

        mockkConstructor(GameStompClient::class)

        every { anyConstructed<GameStompClient>().lobbyState } returns fakeLobbyState
        every { anyConstructed<GameStompClient>().gameState } returns fakeGameState
        every { anyConstructed<GameStompClient>().actionCardResults } returns fakeActionCardResults
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
        every { anyConstructed<GameStompClient>().playActionCard(any()) } just runs
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
    fun `myPlayerId resolves connected player from game state`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.connect("Alice")

        fakeGameState.value = makeGameState(currentPlayerId = "p2")

        assertEquals("p1", viewModel.myPlayerId.value)
        assertFalse(viewModel.isMyTurn.value)
    }

    @Test
    fun `isMyTurn is true when current player matches resolved player id`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.connect("Alice")

        fakeGameState.value = makeGameState(currentPlayerId = "p1")

        assertEquals("p1", viewModel.myPlayerId.value)
        assertTrue(viewModel.isMyTurn.value)
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
        verify(exactly = 1) { anyConstructed<GameStompClient>().disconnect() }
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
    fun `drawVisibleActionCard sends DRAW_VISIBLE_ACTION_CARD action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.drawVisibleActionCard(actionCardIndex = 2)
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DRAW_VISIBLE_ACTION_CARD", actionCardIndex = 2)
            )
        }
    }

    @Test
    fun `playActionCard sends PLAY_ACTION_CARD action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.playActionCard(actionCardIndex = 1)
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "PLAY_ACTION_CARD", actionCardIndex = 1)
            )
        }
    }

    @Test
    fun `playActionCard with command delegates to private action card endpoint`() {
        val command = PlayActionCardCommand(
            actionCardIndex = 1,
            parameters = ActionCardParameters.BoardLineTarget(
                targetPlayerId = "p2",
                targetType = BoardLineTargetType.ROW,
                lineIndex = 0,
            ),
        )
        val viewModel = GameViewModel(mockApplication)

        viewModel.playActionCard(command)

        verify(exactly = 1) {
            anyConstructed<GameStompClient>().playActionCard(command)
        }
    }

    @Test
    fun `playEnlightenment builds board line target command`() {
        val viewModel = GameViewModel(mockApplication)

        viewModel.playEnlightenment(
            actionCardIndex = 2,
            targetPlayerId = "p3",
            targetType = BoardLineTargetType.COLUMN,
            lineIndex = 1,
        )

        verify(exactly = 1) {
            anyConstructed<GameStompClient>().playActionCard(
                PlayActionCardCommand(
                    actionCardIndex = 2,
                    parameters = ActionCardParameters.BoardLineTarget(
                        targetPlayerId = "p3",
                        targetType = BoardLineTargetType.COLUMN,
                        lineIndex = 1,
                    ),
                ),
            )
        }
    }

    @Test
    fun `discardActionCard sends DISCARD_ACTION_CARD action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.discardActionCard(actionCardIndex = 0)
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 0)
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

    @Test
    fun `playPlayerSwapCard sends PLAY_ACTION_CARD action with all swap fields`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.playPlayerSwapCard(
            actionCardIndex = 0,
            player1Id = "p1", player1Row = 1, player1Col = 2,
            player2Id = "p2", player2Row = 0, player2Col = 3,
        )
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(
                    type = "PLAY_ACTION_CARD",
                    actionCardIndex = 0,
                    targetPlayer1Id = "p1",
                    targetPlayer1Row = 1,
                    targetPlayer1Col = 2,
                    targetPlayer2Id = "p2",
                    targetPlayer2Row = 0,
                    targetPlayer2Col = 3,
                )
            )
        }
    }

    @Test
    fun `playSwapOwnCards sends PLAY_ACTION_CARD action with own swap coordinates`() {
        val viewModel = GameViewModel(mockApplication)

        viewModel.playSwapOwnCards(
            actionCardIndex = 2,
            firstRow = 0,
            firstCol = 1,
            secondRow = 2,
            secondCol = 3,
        )

        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(
                    type = "PLAY_ACTION_CARD",
                    actionCardIndex = 2,
                    targetPlayer1Row = 0,
                    targetPlayer1Col = 1,
                    targetPlayer2Row = 2,
                    targetPlayer2Col = 3,
                )
            )
        }
    }

    @Test
    fun `discardActionCard sends DISCARD_ACTION_CARD action with correct index`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.discardActionCard(actionCardIndex = 2)
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 2)
            )
        }
    }

    @Test
    fun `playPlayerSwapCard with same player IDs still sends action (backend validates same-player rule)`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.playPlayerSwapCard(
            actionCardIndex = 1,
            player1Id = "p1", player1Row = 0, player1Col = 0,
            player2Id = "p1", player2Row = 0, player2Col = 1,
        )
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(
                    type = "PLAY_ACTION_CARD",
                    actionCardIndex = 1,
                    targetPlayer1Id = "p1",
                    targetPlayer1Row = 0,
                    targetPlayer1Col = 0,
                    targetPlayer2Id = "p1",
                    targetPlayer2Row = 0,
                    targetPlayer2Col = 1,
                )
            )
        }
    }

    @Test
    fun `discardActionCard with index zero sends correct action`() {
        val viewModel = GameViewModel(mockApplication)
        viewModel.discardActionCard(actionCardIndex = 0)
        verify(exactly = 1) {
            anyConstructed<GameStompClient>().sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 0)
            )
        }
    }

    private fun makeGameState(currentPlayerId: String?) = GameUpdateMessage(
        phase = "AWAITING_DRAW",
        currentPlayerId = currentPlayerId,
        roundNumber = 1,
        gameOver = false,
        totalScores = listOf(
            TotalScore("p1", "Alice", 0),
            TotalScore("p2", "Bob", 0),
        ),
        players = listOf(
            GamePlayerState(
                playerId = "p1",
                nickname = "Alice",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) {
                    List(4) { BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(1, 5, "NUMBER")) }
                },
            ),
        ),
    )
}
