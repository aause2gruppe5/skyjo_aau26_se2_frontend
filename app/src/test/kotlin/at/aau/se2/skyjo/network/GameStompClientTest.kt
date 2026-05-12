package at.aau.se2.skyjo.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.junit.After
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
        every { Log.e(any(), any()) } returns 0

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

    @Test
    fun `connect establishes session and logs success`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        coVerify(atLeast = 1) {
            anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `connect resets connectionError on success`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        assertNull(client.connectionError.value)
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
        delay(300)

        client.joinLobby("Hans")
        delay(300)

        coVerify(atLeast = 1) {
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
}
