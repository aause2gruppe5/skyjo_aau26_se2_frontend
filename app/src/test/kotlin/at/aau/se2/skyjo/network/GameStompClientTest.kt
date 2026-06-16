package at.aau.se2.skyjo.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import at.aau.se2.skyjo.model.ActionCardParameters
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.CheatPeekResultMessage
import at.aau.se2.skyjo.model.CheatReportResultMessage
import at.aau.se2.skyjo.model.DrawThreeCardsChoiceMode
import at.aau.se2.skyjo.model.DrawThreeCardsDiscardReference
import at.aau.se2.skyjo.model.PlayActionCardCommand
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GameStompClientTest {

    private lateinit var mockSession: StompSession
    private lateinit var topicFlow: MutableSharedFlow<String>
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockkStatic(Log::class)
        mockkStatic("org.hildan.krossbow.stomp.StompClientKt")
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        mockkConstructor(StompClient::class)

        mockSession = mockk(relaxed = true)
        topicFlow = MutableSharedFlow()

        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString(any(), any()) } returns null
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } just runs

        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } answers {
            println("LOG_ERROR: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }

        coEvery {
            anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any())
        } returns mockSession

        coEvery { any<StompSession>().subscribeText(any()) } returns topicFlow
        coEvery { any<StompSession>().sendText(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    fun `connect establishes session and logs success`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        coVerify(atLeast = 1) {
            anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `connect uses configured websocket url`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        coVerify(atLeast = 1) {
            anyConstructed<StompClient>().connect(
                url = SkyjoApiClient.WS_BASE_URL,
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `connectForInvites does not subscribe to lobby or game topics`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connectForInvites("ticket")
        delay(300)

        coVerify(exactly = 1) { any<StompSession>().subscribeText("/user/queue/invites") }
        coVerify(exactly = 1) { any<StompSession>().subscribeText("/user/queue/errors") }
        coVerify(exactly = 0) { any<StompSession>().subscribeText("/topic/lobby") }
        coVerify(exactly = 0) { any<StompSession>().subscribeText("/topic/game") }
    }

    @Test
    fun `connectForInvites marks disconnected when invite subscription fails`() = runBlocking {
        coEvery { any<StompSession>().subscribeText("/user/queue/invites") } returns flow {
            throw RuntimeException("invite stream closed")
        }

        val client = GameStompClient(mockContext)
        client.connectForInvites("ticket")
        delay(300)

        assertFalse(client.isConnected.value)
    }

    @Test
    fun `connect resets connectionError on success`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        assertNull(client.connectionError.value)
    }

    @Test
    fun `connect disconnects previous session before replacing it`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.connect()
        delay(300)

        coVerify(atLeast = 1) { mockSession.disconnect() }
    }

    @Test
    fun `connect sets connectionError on failure`() = runBlocking {
        val errorMsg = "Connection refused"
        coEvery {
            anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any())
        } throws Exception(errorMsg)

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        assert(client.connectionError.value == errorMsg)
        verify { Log.e("GameStompClient", match { it.contains("Connection error") }) }
    }

    @Test
    fun `joinLobby sends correct destination and payload`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()

        client.joinLobby("Hans")

        coVerify(timeout = 1000, atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/lobby.join",
                body = match { it.contains("Hans") },
            )
        }
    }

    @Test
    fun `leaveLobby sends empty body to correct destination`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.leaveLobby()
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(destination = "/app/lobby.leave", body = "")
        }
    }

    @Test
    fun `startGame sends to correct destination`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.startGame(maxRounds = 5, targetScore = 100)
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText("/app/game.start", any())
        }
    }

    @Test
    fun `sendAction DRAW DECK sends correct JSON`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.sendAction(at.aau.se2.skyjo.model.GameAction(type = "DRAW", source = "DECK"))
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/game.action",
                body = match { it.contains("DRAW") && it.contains("DECK") },
            )
        }
    }

    @Test
    fun `sendAction PLAY_ACTION_CARD sends action card index`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.sendAction(
            at.aau.se2.skyjo.model.GameAction(
                type = "PLAY_ACTION_CARD",
                actionCardIndex = 2,
            ),
        )
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/game.action",
                body = match { it.contains("PLAY_ACTION_CARD") && it.contains("\"actionCardIndex\":2") },
            )
        }
    }

    @Test
    fun `playActionCard sends private action card command`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.playActionCard(
            PlayActionCardCommand(
                actionCardIndex = 2,
                parameters = ActionCardParameters.BoardLineTarget(
                    targetPlayerId = "p2",
                    targetType = BoardLineTargetType.ROW,
                    lineIndex = 0,
                ),
            ),
        )
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/game.action-card",
                body = match {
                    it.contains("\"actionCardIndex\":2") &&
                        it.contains("\"targetPlayerId\":\"p2\"") &&
                        it.contains("\"targetType\":\"ROW\"") &&
                        it.contains("\"lineIndex\":0")
                },
            )
        }
    }

    @Test
    fun `playActionCard sends draw three cards first step without parameters`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.playActionCard(PlayActionCardCommand(actionCardIndex = 0))
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/game.action-card",
                body = match {
                    it.contains("\"actionCardIndex\":0") &&
                            !it.contains("parameters")
                },
            )
        }
    }

    @Test
    fun `playActionCard sends draw three cards choice command`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.playActionCard(
            PlayActionCardCommand(
                actionCardIndex = 0,
                parameters = ActionCardParameters.DrawThreeCardsChoice(
                    mode = DrawThreeCardsChoiceMode.DISCARD_ALL_AND_REVEAL,
                    revealRow = 1,
                    revealColumn = 2,
                    discardOrder = listOf(
                        DrawThreeCardsDiscardReference.DRAWN_CARD_2,
                        DrawThreeCardsDiscardReference.DRAWN_CARD_0,
                        DrawThreeCardsDiscardReference.DRAWN_CARD_1,
                    ),
                ),
            ),
        )
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/game.action-card",
                body = match {
                    it.contains("\"actionCardIndex\":0") &&
                            it.contains("\"mode\":\"DISCARD_ALL_AND_REVEAL\"") &&
                            it.contains("\"revealRow\":1") &&
                            it.contains("\"revealColumn\":2") &&
                            it.contains("\"discardOrder\":[\"DRAWN_CARD_2\",\"DRAWN_CARD_0\",\"DRAWN_CARD_1\"]") &&
                            !it.contains("DrawThreeCardsChoice")
                },
            )
        }
    }

    @Test
    fun `cheatPeekDrawPile sends private cheat peek command`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.cheatPeekDrawPile()
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/game.cheat-peek",
                body = "",
            )
        }
    }

    @Test
    fun `cheatReportCurrentPlayer sends private cheat report command`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.cheatReportCurrentPlayer()
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/game.cheat-report",
                body = "",
            )
        }
    }

    @Test
    fun `playActionCard handles send exception gracefully`() = runBlocking {
        coEvery { any<StompSession>().sendText(any(), any()) } throws Exception("Network error")

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.playActionCard(
            PlayActionCardCommand(
                actionCardIndex = 2,
                parameters = ActionCardParameters.BoardLineTarget(
                    targetPlayerId = "p2",
                    targetType = BoardLineTargetType.ROW,
                    lineIndex = 0,
                ),
            ),
        )
        delay(300)

        verify { Log.e("GameStompClient", match { it.contains("Play action card error") }) }
    }

    @Test
    fun `joinLobby does nothing when session is null`() = runBlocking {
        val client = GameStompClient(mockContext)
        // connect NOT called → session is null

        client.joinLobby("Hans")
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `leaveLobby does nothing when session is null`() = runBlocking {
        val client = GameStompClient(mockContext)

        client.leaveLobby()
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `startGame does nothing when session is null`() = runBlocking {
        val client = GameStompClient(mockContext)

        client.startGame(maxRounds = 3, targetScore = 100)
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `sendAction does nothing when session is null`() = runBlocking {
        val client = GameStompClient(mockContext)

        client.sendAction(at.aau.se2.skyjo.model.GameAction(type = "DRAW", source = "DECK"))
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `playActionCard does nothing when session is null`() = runBlocking {
        val client = GameStompClient(mockContext)

        client.playActionCard(PlayActionCardCommand(actionCardIndex = 1))
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `cheatPeekDrawPile does nothing when session is null`() = runBlocking {
        val client = GameStompClient(mockContext)

        client.cheatPeekDrawPile()
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `cheatReportCurrentPlayer does nothing when session is null`() = runBlocking {
        val client = GameStompClient(mockContext)

        client.cheatReportCurrentPlayer()
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `lobby invalid JSON is handled without crash`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        topicFlow.emit("not valid json {{{")
        delay(300)

        assert(client.lobbyState.value == null) { "lobbyState should remain null on parse error" }
    }

    @Test
    fun `joinLobby handles send exception gracefully`() = runBlocking {
        coEvery { any<StompSession>().sendText(any(), any()) } throws Exception("Network error")

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.joinLobby("Hans")
        delay(300)

        verify { Log.e("GameStompClient", match { it.contains("Join lobby error") }) }
    }

    @Test
    fun `leaveLobby handles send exception gracefully`() = runBlocking {
        coEvery { any<StompSession>().sendText(any(), any()) } throws Exception("Network error")

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.leaveLobby()
        delay(300)

        verify { Log.e("GameStompClient", match { it.contains("Leave lobby error") }) }
    }

    @Test
    fun `sendAction handles send exception gracefully`() = runBlocking {
        coEvery { any<StompSession>().sendText(any(), any()) } throws Exception("Network error")

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.sendAction(at.aau.se2.skyjo.model.GameAction(type = "DRAW", source = "DECK"))
        delay(300)

        verify { Log.e("GameStompClient", match { it.contains("Send action error") }) }
    }

    @Test
    fun `cheatPeekDrawPile handles send exception gracefully`() = runBlocking {
        coEvery { any<StompSession>().sendText(any(), any()) } throws Exception("Network error")

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.cheatPeekDrawPile()
        delay(300)

        verify { Log.e("GameStompClient", match { it.contains("Cheat peek error") }) }
    }

    @Test
    fun `cheatReportCurrentPlayer handles send exception gracefully`() = runBlocking {
        coEvery { any<StompSession>().sendText(any(), any()) } throws Exception("Network error")

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.cheatReportCurrentPlayer()
        delay(300)

        verify { Log.e("GameStompClient", match { it.contains("Cheat report error") }) }
    }

    @Test
    fun `clearStoredGame removes stored game id from prefs`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.clearStoredGame()
        verify { mockEditor.remove("game_id") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `disconnect sets isConnected to false`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.disconnect()
        delay(300)

        assert(!client.isConnected.value)
    }

    @Test
    fun `close sets isConnected to false`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.close()
        delay(100)

        assert(!client.isConnected.value)
    }

    @Test
    fun `startGame handles send exception gracefully`() = runBlocking {
        coEvery { any<StompSession>().sendText(any(), any()) } throws Exception("Network error")

        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.startGame(maxRounds = 3, targetScore = 100)
        delay(300)

        verify { Log.e("GameStompClient", match { it.contains("Start game error") }) }
    }

    @Test
    fun `game update is emitted when valid game JSON arrives`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validGameJson = """
            {
                "phase": "AWAITING_DRAW",
                "currentPlayerId": "p1",
                "roundNumber": 1,
                "gameOver": false,
                "totalScores": [],
                "players": [],
                "disconnectedPlayers": []
            }
        """.trimIndent()

        val collected = mutableListOf<at.aau.se2.skyjo.model.GameUpdateMessage>()
        val job = launch { client.gameState.collect { if (it != null) collected.add(it) } }

        topicFlow.emit(validGameJson)
        delay(500)

        assert(collected.isNotEmpty()) { "Expected game update to be emitted" }
        assert(collected.first().phase == "AWAITING_DRAW")

        job.cancel()
    }

    @Test
    fun `action card result is emitted when valid private result arrives`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validActionCardResultJson = """
            {
                "type": "ENLIGHTENMENT",
                "actionCardIndex": 1,
                "targetPlayerId": "p2",
                "targetType": "COLUMN",
                "lineIndex": 2,
                "inspectedValues": [3, null, 7]
            }
        """.trimIndent()

        val collected = mutableListOf<ActionCardResultMessage>()
        val job = launch { client.actionCardResults.collect { collected.add(it) } }
        delay(100)

        topicFlow.emit(validActionCardResultJson)
        delay(500)

        assert(collected.isNotEmpty()) { "Expected action card result to be emitted" }
        assertEquals("ENLIGHTENMENT", collected.first().type)
        assertEquals(BoardLineTargetType.COLUMN, collected.first().targetType)
        assertEquals(listOf(3, null, 7), collected.first().inspectedValues)

        job.cancel()
    }

    @Test
    fun `draw three cards result is emitted when valid private result arrives`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validActionCardResultJson = """
            {
                "type": "DRAW_THREE_CARDS",
                "actionCardIndex": 0,
                "drawnCards": [
                    { "id": 201, "value": 8, "type": "NUMBER" },
                    { "id": 202, "value": -2, "type": "NUMBER" },
                    { "id": 203, "value": 13, "type": "NUMBER" }
                ]
            }
        """.trimIndent()

        val collected = mutableListOf<ActionCardResultMessage>()
        val job = launch { client.actionCardResults.collect { collected.add(it) } }
        delay(100)

        topicFlow.emit(validActionCardResultJson)
        delay(500)

        assert(collected.isNotEmpty()) { "Expected action card result to be emitted" }
        assertEquals("DRAW_THREE_CARDS", collected.first().type)
        assertEquals(listOf(8, -2, 13), collected.first().drawnCards.map { it.value })
        assertNull(collected.first().targetType)

        job.cancel()
    }

    @Test
    fun `cheat peek result is emitted when valid private result arrives`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validCheatPeekResultJson = """
            {
                "card": {"id": 77, "value": 6, "type": "NUMBER"},
                "remainingCheatPeeks": 1
            }
        """.trimIndent()

        val collected = mutableListOf<CheatPeekResultMessage>()
        val job = launch { client.cheatPeekResults.collect { collected.add(it) } }
        delay(100)

        topicFlow.emit(validCheatPeekResultJson)
        delay(500)

        assert(collected.isNotEmpty()) { "Expected cheat peek result to be emitted" }
        assertEquals(6, collected.first().card.value)
        assertEquals(1, collected.first().remainingCheatPeeks)

        job.cancel()
    }

    @Test
    fun `cheat report result is emitted when valid private result arrives`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validCheatReportResultJson = """
            {
                "successful": true,
                "reporterPlayerId": "p2",
                "targetPlayerId": "p1",
                "penaltyPlayerId": "p1",
                "penaltyPoints": 10,
                "remainingCheatReports": 2
            }
        """.trimIndent()

        val collected = mutableListOf<CheatReportResultMessage>()
        val job = launch { client.cheatReportResults.collect { collected.add(it) } }
        delay(100)

        topicFlow.emit(validCheatReportResultJson)
        delay(500)

        assert(collected.isNotEmpty()) { "Expected cheat report result to be emitted" }
        assertEquals(true, collected.first().successful)
        assertEquals("p1", collected.first().penaltyPlayerId)
        assertEquals(2, collected.first().remainingCheatReports)

        job.cancel()
    }

    @Test
    fun `game update stores game id when present`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validGameJson = """
            {
                "gameId": "game-123",
                "phase": "AWAITING_DRAW",
                "currentPlayerId": "p1",
                "roundNumber": 1,
                "gameOver": false,
                "totalScores": [],
                "players": [],
                "disconnectedPlayers": []
            }
        """.trimIndent()

        topicFlow.emit(validGameJson)
        delay(500)

        verify { mockEditor.putString("game_id", "game-123") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `rejoin state sets hasRejoinedGame true when valid game JSON arrives`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validGameJson = """
            {
                "phase": "AWAITING_DRAW",
                "currentPlayerId": "p1",
                "roundNumber": 2,
                "gameOver": false,
                "totalScores": [],
                "players": [],
                "disconnectedPlayers": []
            }
        """.trimIndent()

        topicFlow.emit(validGameJson)

        assert(waitUntil { client.hasRejoinedGame.value }) {
            "Expected hasRejoinedGame to be true after rejoin JSON"
        }
    }

    @Test
    fun `rejoin state does not set hasRejoinedGame for round finished game JSON`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val finishedGameJson = """
            {
                "phase": "ROUND_FINISHED",
                "currentPlayerId": null,
                "roundNumber": 2,
                "gameOver": false,
                "totalScores": [],
                "players": [],
                "disconnectedPlayers": []
            }
        """.trimIndent()

        topicFlow.emit(finishedGameJson)
        delay(500)

        assert(!client.hasRejoinedGame.value) { "Expected finished rejoin JSON not to trigger rejoin navigation" }
    }

    @Test
    fun `game over update clears stored game id`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val gameOverJson = """
            {
                "phase": "ROUND_FINISHED",
                "currentPlayerId": null,
                "roundNumber": 2,
                "gameOver": true,
                "gameId": "game-123",
                "totalScores": [],
                "players": [],
                "disconnectedPlayers": []
            }
        """.trimIndent()

        topicFlow.emit(gameOverJson)
        delay(500)

        verify(atLeast = 1) { mockEditor.remove("game_id") }
        verify(atLeast = 1) { mockEditor.apply() }
    }

    @Test
    fun `game invalid JSON is handled without crash`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        topicFlow.emit("not valid game json {{{")
        delay(300)

        assert(client.gameState.value == null) { "gameState should remain null on parse error" }
    }

    @Test
    fun `error collector emits raw text when message is not a JSON map`() = runBlocking {
        val client = GameStompClient(mockContext)

        val collected = mutableListOf<String>()
        val job = launch { client.errorMessage.collect { collected.add(it) } }
        delay(100)

        client.connect()
        delay(300)

        topicFlow.emit("plain error message")
        delay(500)

        assert(collected.any { it == "plain error message" }) {
            "Expected raw error text to be emitted when JSON parse fails"
        }
        job.cancel()
    }

    @Test
    fun `error collector emits message field from JSON map`() = runBlocking {
        val client = GameStompClient(mockContext)

        val collected = mutableListOf<String>()
        val job = launch { client.errorMessage.collect { collected.add(it) } }
        delay(100)

        client.connect()
        delay(300)

        topicFlow.emit("""{"message":"Not your turn"}""")
        delay(500)

        assert(collected.any { it == "Not your turn" }) {
            "Expected message field to be emitted from error JSON"
        }
        job.cancel()
    }

    @Test
    fun `error collector falls back to raw JSON when message field is absent`() = runBlocking {
        val client = GameStompClient(mockContext)

        val collected = mutableListOf<String>()
        val job = launch { client.errorMessage.collect { collected.add(it) } }
        delay(100)

        client.connect()
        delay(300)

        val jsonWithoutMessage = """{"code":"TURN_ERROR"}"""
        topicFlow.emit(jsonWithoutMessage)
        delay(500)

        assert(collected.any { it == jsonWithoutMessage }) {
            "Expected raw JSON text when error message field is absent"
        }
        job.cancel()
    }

    @Test
    fun `joinLobby with gameId sends gameId in payload`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        client.joinLobby("Hans", "game-123")
        delay(300)

        coVerify(atLeast = 1) {
            any<StompSession>().sendText(
                destination = "/app/lobby.join",
                body = match { it.contains("game-123") },
            )
        }
    }

    @Test
    fun `reconnect calls connect and joinLobby after successful connection`() = runBlocking {
        val client = GameStompClient(mockContext)

        val job = launch { client.reconnect("Hans") }
        delay(1500)
        job.cancel()

        coVerify(atLeast = 1) {
            anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `lobby update is emitted when valid JSON arrives`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        val validLobbyJson = """
            {
                "players": [{"nickname": "Alice", "isHost": true}],
                "status": "WAITING",
                "maxPlayers": 6
            }
        """.trimIndent()

        val collected = mutableListOf<at.aau.se2.skyjo.model.LobbyUpdateMessage>()
        val job = launch { client.lobbyState.collect { if (it != null) collected.add(it) } }

        topicFlow.emit(validLobbyJson)
        delay(500)

        assert(collected.isNotEmpty()) { "Expected lobby update to be emitted" }
        assert(collected.first().players.first().nickname == "Alice")

        job.cancel()
    }

    private suspend fun waitUntil(
        timeoutMillis: Long = 1000,
        intervalMillis: Long = 25,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            delay(intervalMillis)
        }
        return condition()
    }
}
