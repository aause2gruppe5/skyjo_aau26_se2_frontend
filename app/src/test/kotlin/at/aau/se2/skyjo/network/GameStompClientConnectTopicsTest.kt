package at.aau.se2.skyjo.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies that connect() only subscribes the authenticated topic set and no
 * longer the (removed) global /topic/lobby and /topic/game topics.
 */
class GameStompClientConnectTopicsTest {

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
    fun `connect subscribes join-code lobby topic and not the global lobby or game topics`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect(ticket = "ticket", lobbyJoinCode = "ABC123")
        delay(300)

        coVerify(atLeast = 1) { any<StompSession>().subscribeText("/topic/lobbies/ABC123") }
        coVerify(exactly = 0) { any<StompSession>().subscribeText("/topic/lobby") }
        coVerify(exactly = 0) { any<StompSession>().subscribeText("/topic/game") }
    }

    @Test
    fun `connect without a join code subscribes neither the global lobby nor game topic`() = runBlocking {
        val client = GameStompClient(mockContext)
        client.connect()
        delay(300)

        coVerify(exactly = 0) { any<StompSession>().subscribeText("/topic/lobby") }
        coVerify(exactly = 0) { any<StompSession>().subscribeText("/topic/game") }
    }
}
