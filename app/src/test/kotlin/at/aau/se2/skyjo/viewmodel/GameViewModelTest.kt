package at.aau.se2.skyjo.viewmodel

import at.aau.se2.skyjo.model.ServerMessage
import at.aau.se2.skyjo.network.GameStompClient
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

    private val dummyFlow = MutableSharedFlow<ServerMessage>()

    @Before
    fun setup() {
        //Wir fangen jeden Aufruf von GameStompClient() ab
        mockkConstructor(GameStompClient::class)

        //Wenn das ViewModel auf die Properties des Clients zugreift, geben wir Dummys zurück, damit die App nicht abstürzt.
        every { anyConstructed<GameStompClient>().messages } returns dummyFlow

        //Methodenaufrufe (Unit/Void) abfangen, damit sie nichts echtes ausführen
        every { anyConstructed<GameStompClient>().connect() } returns Unit
        every { anyConstructed<GameStompClient>().joinGame(any()) } returns Unit
        every { anyConstructed<GameStompClient>().leaveGame() } returns Unit
    }

    @After
    fun tearDown() {
        //Nach jedem Test räumen wir auf, damit sich die Tests nicht gegenseitig beeinflussen
        clearAllMocks()
    }

    @Test
    fun `init ruft connect auf`() {
        //Wir erstellen das ViewModel (das triggert den init-Block)
        GameViewModel()

        //Wir prüfen, ob connect() beim konstruierten Client aufgerufen wurde
        verify(exactly = 1) { anyConstructed<GameStompClient>().connect() }
    }

    @Test
    fun `messages stellt den Flow vom Client bereit`() {
        // Arrange
        val viewModel = GameViewModel()

        // Assert: Wir prüfen, ob das ViewModel genau unseren dummyFlow nach außen reicht
        assertEquals(dummyFlow, viewModel.messages)
    }

    @Test
    fun `joinGame ruft client joinGame mit richtigem Namen auf`() {
        val viewModel = GameViewModel()
        val testName = "Spieler1"

        viewModel.joinGame(testName)

        verify(exactly = 1) { anyConstructed<GameStompClient>().joinGame(testName) }
    }

    @Test
    fun `leaveGame ruft client leaveGame auf`() {
        val viewModel = GameViewModel()

        viewModel.leaveGame()

        verify(exactly = 1) { anyConstructed<GameStompClient>().leaveGame() }
    }

    @Test
    fun `onCleared ruft leaveGame auf`() {
        val viewModel = GameViewModel()

        //Da 'onCleared()' in ViewModel 'protected' ist, können wir es nicht direkt aufrufen. Wir nutzen Kotlin Reflection, um die Methode für den Test sichtbar zu machen und auszuführen.
        val method = GameViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)

        //Da onCleared() leaveGame() aufruft, muss der Client dies registrieren
        verify(exactly = 1) { anyConstructed<GameStompClient>().leaveGame() }
    }
}