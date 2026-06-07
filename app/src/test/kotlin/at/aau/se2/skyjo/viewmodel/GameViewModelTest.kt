package at.aau.se2.skyjo.viewmodel

import android.app.Application
import at.aau.se2.skyjo.model.ActionCardParameters
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.CheatPeekResultMessage
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.LobbyPlayer
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.model.PlayActionCardCommand
import at.aau.se2.skyjo.model.TotalScore
import at.aau.se2.skyjo.model.auth.WsTicketResponse
import at.aau.se2.skyjo.model.lobby.LobbySummaryResponse
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.LobbyInviteStatus
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.model.stats.PlayerStatsDto
import at.aau.se2.skyjo.network.GameRealtimeClient
import at.aau.se2.skyjo.network.SkyjoApi
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockApi = mockk<SkyjoApi>(relaxed = true)
    private val mockGameClient = mockk<GameRealtimeClient>(relaxed = true)

    private val fakeLobbyState = MutableStateFlow<LobbyUpdateMessage?>(null)
    private val fakeGameState = MutableStateFlow<GameUpdateMessage?>(null)
    private val fakeActionCardResults = MutableSharedFlow<ActionCardResultMessage>()
    private val fakeCheatPeekResults = MutableSharedFlow<CheatPeekResultMessage>()
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
        every { mockGameClient.applyLobbyState(any()) } just runs
        every { mockGameClient.leaveLobby() } just runs
        every { mockGameClient.startGame(any(), any()) } just runs
        every { mockGameClient.sendAction(any()) } just runs
        every { mockGameClient.playActionCard(any()) } just runs
        every { mockGameClient.cheatPeekDrawPile() } just runs
        every { mockGameClient.clearStoredGame() } just runs
        every { mockGameClient.disconnect() } just runs
        every { mockGameClient.close() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `init does not connect automatically`() {
        GameViewModel(mockApplication, mockApi, mockGameClient)
        verify(exactly = 0) { mockGameClient.joinLobby(any(), any()) }
    }

    @Test
    fun `myPlayerName is empty initially`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        assertEquals("", viewModel.myPlayerName.value)
    }

    @Test
    fun `connect sets playerName`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.connect("TestPlayer")
        assertEquals("TestPlayer", viewModel.myPlayerName.value)
    }

    @Test
    fun `setAuthenticatedUsername stores current player name`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.setAuthenticatedUsername("Alice")

        assertEquals("Alice", viewModel.myPlayerName.value)
    }

    @Test
    fun `refreshHomeStats stores returned stats`() {
        coEvery { mockApi.myStats() } returns stats(username = "Alice")
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.refreshHomeStats()

        assertEquals("Alice", viewModel.homeStats.value?.username)
    }

    @Test
    fun `refreshHomeStats keeps previous stats when request fails`() {
        coEvery { mockApi.myStats() } returns stats(username = "Alice")
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.refreshHomeStats()
        coEvery { mockApi.myStats() } throws IllegalStateException("offline")

        viewModel.refreshHomeStats()

        assertEquals("Alice", viewModel.homeStats.value?.username)
    }

    @Test
    fun `myPlayerId resolves connected player from game state`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.connect("Alice")

        fakeGameState.value = makeGameState(currentPlayerId = "p2")

        assertEquals("p1", viewModel.myPlayerId.value)
        assertFalse(viewModel.isMyTurn.value)
    }

    @Test
    fun `isMyTurn is true when current player matches resolved player id`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.connect("Alice")

        fakeGameState.value = makeGameState(currentPlayerId = "p1")

        assertEquals("p1", viewModel.myPlayerId.value)
        assertTrue(viewModel.isMyTurn.value)
    }

    @Test
    fun `isHost is false when lobby is empty`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        assertFalse(viewModel.isHost.value)
    }

    @Test
    fun `isHost is true when player is first in lobby`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
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
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
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
    fun `isHost is true when player is not first but has isHost flag set to true`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.connect("Bob")
        fakeLobbyState.value = LobbyUpdateMessage(
            players = listOf(
                LobbyPlayer("Alice", isHost = false),
                LobbyPlayer("Bob", isHost = true),
            ),
            status = "WAITING",
            maxPlayers = 6,
        )
        assertTrue(viewModel.isHost.value)
    }

    @Test
    fun `leaveLobby delegates to client`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.leaveLobby()
        verify(exactly = 1) { mockGameClient.leaveLobby() }
        verify(exactly = 1) { mockGameClient.disconnect() }
    }

    @Test
    fun `leaveLobby calls rest endpoint when authenticated lobby is active`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        fakeLobbyState.value = LobbyUpdateMessage(
            lobbyId = "lobby-1",
            joinCode = "ABC123",
            players = listOf(LobbyPlayer("Alice", isHost = true)),
            status = "WAITING",
            maxPlayers = 6,
        )
        coEvery { mockApi.leaveLobby("lobby-1") } returns lobbySummary(players = listOf(LobbyPlayer("Bob", false)))

        viewModel.leaveLobby()

        coVerify(exactly = 1) { mockApi.leaveLobby("lobby-1") }
        verify(exactly = 0) { mockGameClient.leaveLobby() }
        verify(exactly = 1) { mockGameClient.clearStoredGame() }
        verify(exactly = 1) { mockGameClient.disconnect() }
        assertEquals("", viewModel.myPlayerName.value)
    }

    @Test
    fun `createLobby connects with ticket and applies lobby state`() {
        coEvery { mockApi.createLobby() } returns lobbySummary()
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.createLobby("Alice")

        assertEquals("Alice", viewModel.myPlayerName.value)
        assertEquals(null, viewModel.lobbyError.value)
        coVerify(exactly = 1) { mockApi.createLobby() }
        coVerify(exactly = 1) { mockApi.createWebSocketTicket() }
        coVerify(exactly = 1) { mockGameClient.connect("ticket", "ABC123") }
        verify(exactly = 1) { mockGameClient.applyLobbyState(expectedLobbyUpdate()) }
    }

    @Test
    fun `createLobby stores readable error on failure`() {
        coEvery { mockApi.createLobby() } throws IllegalStateException("user is already in a lobby")
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.createLobby("Alice")

        assertEquals("user is already in a lobby", viewModel.lobbyError.value)
        coVerify(exactly = 0) { mockGameClient.connect(any(), any()) }
        verify(exactly = 0) { mockGameClient.applyLobbyState(any()) }
    }

    @Test
    fun `joinLobbyByCode connects with returned join code and applies lobby state`() {
        coEvery { mockApi.joinLobby("abc123") } returns lobbySummary()
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.joinLobbyByCode("Alice", "abc123")

        assertEquals("Alice", viewModel.myPlayerName.value)
        coVerify(exactly = 1) { mockApi.joinLobby("abc123") }
        coVerify(exactly = 1) { mockGameClient.connect("ticket", "ABC123") }
        verify(exactly = 1) { mockGameClient.applyLobbyState(expectedLobbyUpdate()) }
    }

    @Test
    fun `joinLobbyByCode stores fallback error when exception has no message`() {
        coEvery { mockApi.joinLobby("bad") } throws object : RuntimeException() {}
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.joinLobbyByCode("Alice", "bad")

        assertEquals("Could not join lobby", viewModel.lobbyError.value)
    }

    @Test
    fun `acceptLobbyInvite connects and applies current lobby when available`() {
        coEvery { mockApi.acceptLobbyInvite("invite-1") } returns lobbyInvite()
        coEvery { mockApi.currentLobby() } returns lobbySummary()
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.acceptLobbyInvite("Alice", "invite-1")

        assertEquals("Alice", viewModel.myPlayerName.value)
        coVerify(exactly = 1) { mockApi.acceptLobbyInvite("invite-1") }
        coVerify(exactly = 1) { mockApi.currentLobby() }
        coVerify(exactly = 1) { mockGameClient.connect("ticket", "ABC123") }
        verify(exactly = 1) { mockGameClient.applyLobbyState(expectedLobbyUpdate()) }
    }

    @Test
    fun `acceptLobbyInvite skips lobby state when current lobby is missing`() {
        coEvery { mockApi.acceptLobbyInvite("invite-1") } returns lobbyInvite()
        coEvery { mockApi.currentLobby() } returns null
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.acceptLobbyInvite("Alice", "invite-1")

        coVerify(exactly = 1) { mockGameClient.connect("ticket", "ABC123") }
        verify(exactly = 0) { mockGameClient.applyLobbyState(any()) }
    }

    @Test
    fun `acceptLobbyInvite stores fallback error when request fails without message`() {
        coEvery { mockApi.acceptLobbyInvite("invite-1") } throws object : RuntimeException() {}
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.acceptLobbyInvite("Alice", "invite-1")

        assertEquals("Could not accept invite", viewModel.lobbyError.value)
    }

    @Test
    fun `startGame delegates to client with correct params`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.startGame(maxRounds = 5, targetScore = 100)
        verify(exactly = 1) { mockGameClient.startGame(5, 100) }
    }

    @Test
    fun `startGame with default params uses correct defaults`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.startGame()
        verify(exactly = 1) { mockGameClient.startGame(3, 100) }
    }

    @Test
    fun `drawFromDeck sends DRAW DECK action`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.drawFromDeck()
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DRAW", source = "DECK")
            )
        }
    }

    @Test
    fun `cheatPeekDrawPile delegates to private cheat endpoint`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.cheatPeekDrawPile()

        verify(exactly = 1) {
            mockGameClient.cheatPeekDrawPile()
        }
    }

    @Test
    fun `drawFromDiscard sends DRAW DISCARD action`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.drawFromDiscard()
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DRAW", source = "DISCARD")
            )
        }
    }

    @Test
    fun `replaceCard sends REPLACE action with correct coordinates`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.replaceCard(row = 1, col = 2)
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "REPLACE", row = 1, col = 2)
            )
        }
    }

    @Test
    fun `discardAndReveal sends DISCARD_AND_REVEAL action`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.discardAndReveal(row = 0, col = 3)
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DISCARD_AND_REVEAL", row = 0, col = 3)
            )
        }
    }

    @Test
    fun `drawFromActionDeck sends DRAW ACTION_DECK action`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.drawFromActionDeck()
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DRAW", source = "ACTION_DECK")
            )
        }
    }

    @Test
    fun `drawVisibleActionCard sends DRAW_VISIBLE_ACTION_CARD action`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.drawVisibleActionCard(actionCardIndex = 2)
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DRAW_VISIBLE_ACTION_CARD", actionCardIndex = 2)
            )
        }
    }

    @Test
    fun `playActionCard sends PLAY_ACTION_CARD action`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.playActionCard(actionCardIndex = 1)
        verify(exactly = 1) {
            mockGameClient.sendAction(
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
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.playActionCard(command)

        verify(exactly = 1) {
            mockGameClient.playActionCard(command)
        }
    }

    @Test
    fun `playEnlightenment builds board line target command`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.playEnlightenment(
            actionCardIndex = 2,
            targetPlayerId = "p3",
            targetType = BoardLineTargetType.COLUMN,
            lineIndex = 1,
        )

        verify(exactly = 1) {
            mockGameClient.playActionCard(
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
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.discardActionCard(actionCardIndex = 0)
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 0)
            )
        }
    }

    @Test
    fun `authenticated reconnect is triggered when connection drops with player name set`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.connect("Alice")

        fakeIsConnected.value = true
        fakeIsConnected.value = false

        coVerify(atLeast = 1) { mockApi.createWebSocketTicket() }
        coVerify(atLeast = 1) { mockGameClient.connect("ticket", any()) }
    }

    @Test
    fun `authenticated reconnect is not triggered when player name is empty`() {
        GameViewModel(mockApplication, mockApi, mockGameClient)

        fakeIsConnected.value = true
        fakeIsConnected.value = false

        coVerify(exactly = 0) { mockApi.createWebSocketTicket() }
    }

    @Test
    fun `authenticated reconnect fetches and applies current lobby state`() {
        coEvery { mockApi.currentLobby() } returns lobbySummary()
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.connect("Alice")

        fakeIsConnected.value = true
        fakeIsConnected.value = false

        coVerify(atLeast = 1) { mockApi.currentLobby() }
        verify(atLeast = 1) { mockGameClient.applyLobbyState(expectedLobbyUpdate()) }
    }

    @Test
    fun `authenticated reconnect skips applying lobby state when currentLobby returns null`() {
        coEvery { mockApi.currentLobby() } returns null
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.connect("Alice")

        fakeIsConnected.value = true
        fakeIsConnected.value = false

        coVerify(atLeast = 1) { mockApi.currentLobby() }
        verify(exactly = 0) { mockGameClient.applyLobbyState(any()) }
    }

    @Test
    fun `onCleared calls close`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        val method = GameViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)
        verify(exactly = 1) { mockGameClient.close() }
    }

    @Test
    fun `playPlayerSwapCard sends PLAY_ACTION_CARD action with all swap fields`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.playPlayerSwapCard(
            actionCardIndex = 0,
            player1Id = "p1", player1Row = 1, player1Col = 2,
            player2Id = "p2", player2Row = 0, player2Col = 3,
        )
        verify(exactly = 1) {
            mockGameClient.sendAction(
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
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        viewModel.playSwapOwnCards(
            actionCardIndex = 2,
            firstRow = 0,
            firstCol = 1,
            secondRow = 2,
            secondCol = 3,
        )

        verify(exactly = 1) {
            mockGameClient.sendAction(
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
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.discardActionCard(actionCardIndex = 2)
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 2)
            )
        }
    }

    @Test
    fun `playPlayerSwapCard with same player IDs still sends action (backend validates same-player rule)`() {
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.playPlayerSwapCard(
            actionCardIndex = 1,
            player1Id = "p1", player1Row = 0, player1Col = 0,
            player2Id = "p1", player2Row = 0, player2Col = 1,
        )
        verify(exactly = 1) {
            mockGameClient.sendAction(
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
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)
        viewModel.discardActionCard(actionCardIndex = 0)
        verify(exactly = 1) {
            mockGameClient.sendAction(
                GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = 0)
            )
        }
    }

    @Test
    fun `startNextRound sends START_NEXT_ROUND action to game client`() {
        // Arrange
        val viewModel = GameViewModel(mockApplication, mockApi, mockGameClient)

        // Act
        viewModel.startNextRound()

        // Assert
        verify(exactly = 1) {
            mockGameClient.sendAction(GameAction(type = "START_NEXT_ROUND"))
        }
    }

    private fun lobbySummary(
        lobbyId: String = "lobby-1",
        joinCode: String = "ABC123",
        players: List<LobbyPlayer> = listOf(LobbyPlayer("Alice", isHost = true)),
        status: String = "WAITING",
        maxPlayers: Int = 6,
    ) = LobbySummaryResponse(
        lobbyId = lobbyId,
        joinCode = joinCode,
        players = players,
        status = status,
        maxPlayers = maxPlayers,
    )

    private fun expectedLobbyUpdate(
        lobbyId: String = "lobby-1",
        joinCode: String = "ABC123",
        players: List<LobbyPlayer> = listOf(LobbyPlayer("Alice", isHost = true)),
        status: String = "WAITING",
        maxPlayers: Int = 6,
    ) = LobbyUpdateMessage(
        lobbyId = lobbyId,
        joinCode = joinCode,
        players = players,
        status = status,
        maxPlayers = maxPlayers,
    )

    private fun lobbyInvite() = LobbyInviteDto(
        inviteId = "invite-1",
        lobbyId = "lobby-1",
        joinCode = "ABC123",
        from = SocialUserDto("user-b", "Bob"),
        to = SocialUserDto("user-a", "Alice"),
        status = LobbyInviteStatus.PENDING,
        createdAt = 1_000L,
    )

    private fun stats(username: String) = PlayerStatsDto(
        userId = "user-a",
        username = username,
        gamesPlayed = 3,
        wins = 1,
        totalScore = 42,
        bestScore = 10,
        averageScore = 14.0,
    )

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
