package at.aau.se2.skyjo.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LobbyAndStartMessageTest {

    @Test
    fun `join lobby message defaults game id to null`() {
        val message = JoinLobbyMessage(playerName = "Alice")

        assertEquals("Alice", message.playerName)
        assertNull(message.gameId)
    }

    @Test
    fun `start game message uses default game settings`() {
        val message = StartGameMessage()

        assertEquals(3, message.maxRounds)
        assertEquals(100, message.targetScore)
    }
}
