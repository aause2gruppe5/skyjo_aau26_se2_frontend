package at.aau.se2.skyjo.network

import android.util.Log
import at.aau.se2.skyjo.model.ServerMessage
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.stomp.sendText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GameStompClientTest {

    private lateinit var mockSession: StompSession
    private lateinit var topicFlow: MutableSharedFlow<String>

    @Before
    fun setup() {
        // Alles auf Null zurücksetzen für jeden Test
        mockkStatic(Log::class)
        mockkStatic("org.hildan.krossbow.stomp.StompClientKt")
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        mockkConstructor(StompClient::class)

        mockSession = mockk(relaxed = true)
        topicFlow = MutableSharedFlow()

        every { Log.d(any(), any()) } returns 0

        // Wir nutzen hier GANZ allgemeine Matcher für connect
        coEvery {
            anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any())
        } returns mockSession

        // Und für die Extensions
        coEvery { any<StompSession>().subscribeText(any()) } returns topicFlow
        coEvery { any<StompSession>().sendText(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `connect baut Verbindung auf und abonniert Topic`() = runBlocking {
        val client = GameStompClient()
        client.connect()

        // Wir geben der Coroutine deutlich Zeit (0.5 Sekunde)
        delay(500)

        // Wir prüfen nur, DASS connect mit IRGENDEINER URL gerufen wurde
        coVerify(atLeast = 1) { anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any()) }
        verify(atLeast = 1) { Log.d("STOMP", any()) }
    }

    @Test
    fun `subscribeToPublicTopic verarbeitet gueltiges JSON`() = runBlocking {
        val client = GameStompClient()
        client.connect()
        delay(500)

        val emittedMessages = mutableListOf<ServerMessage>()
        val job = launch {
            client.messages.collect { emittedMessages.add(it) }
        }

        // 1. Erstelle ein gueltiges JSON.
        // WICHTIG: Das "type"-Feld muss exakt einem Namen aus deinem MessageType-Enum entsprechen!
        val validJson = """
        {
            "type": "PLAYER_JOINED",
            "content": "Hans ist beigetreten",
            "playerName": "Hans"
        }
    """.trimIndent()

        // 2. Sende das JSON
        topicFlow.emit(validJson)

        // Gib dem Hintergrund-Thread genug Zeit zum Parsen
        delay(1000)

        // 3. Assert
        assertEquals("Die Nachricht sollte erfolgreich parst und weitergeleitet worden sein", 1, emittedMessages.size)
        assertEquals("Hans", emittedMessages[0].playerName)

        job.cancel()
    }

    @Test
    fun `joinGame sendet PlayerMessage an Server`() = runBlocking {
        val client = GameStompClient()
        client.connect()
        delay(500)

        client.joinGame("Hans")
        delay(500)

        // Wir nutzen any() für die Session, weil MockK manchmal die Instanz verliert
        coVerify(atLeast = 1) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `leaveGame sendet leeren Text an Server`() = runBlocking {
        val client = GameStompClient()
        client.connect()
        delay(500)

        client.leaveGame()
        delay(500)

        coVerify(atLeast = 1) { any<StompSession>().sendText(destination = "/app/game.leave", body = "") }
    }

    @Test
    fun `connect faengt Exception ab`() = runBlocking {
        // Spezielles Verhalten für diesen Test
        coEvery { anyConstructed<StompClient>().connect(any(), any(), any(), any(), any(), any()) } throws Exception("Fehler")

        val client = GameStompClient()
        client.connect()
        delay(500)

        verify { Log.d("STOMP", match { it.contains("Error") }) }
    }

    @Test
    fun `connect loggt Fehler wenn Verbindung fehlschlaegt`() = runBlocking {
        // Wir lassen connect eine Exception werfen
        val errorMsg = "Connection failed"
        coEvery { anyConstructed<StompClient>().connect(url = any(), any(), any(), any(), any(), any()) } throws Exception(errorMsg)

        val client = GameStompClient()
        client.connect()
        delay(500)

        // Prüft den catch-Block in connect()
        verify { Log.d("STOMP", "Error: $errorMsg") }
    }

    @Test
    fun `subscribeToPublicTopic loggt Fehler bei invalidem JSON`() = runBlocking {
        val client = GameStompClient()
        client.connect()
        delay(500)

        // Wir schicken absichtlich Müll
        topicFlow.emit("Kein gueltiges JSON")
        delay(500)

        // Prüft den catch-Block in subscribeToPublicTopic()
        verify { Log.d("JSON Parsing Error:", any()) }
    }
    @Test
    fun `joinGame macht nichts wenn session null ist`() = runBlocking {
        val client = GameStompClient()
        // Wir rufen connect() NICHT auf -> session bleibt null

        client.joinGame("Hans")
        delay(500)

        // Prüft den Safe-Call session?.sendText -> es darf NICHT gerufen werden
        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `leaveGame macht nichts wenn session null ist`() = runBlocking {
        val client = GameStompClient()
        // session ist null

        client.leaveGame()
        delay(500)

        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

}