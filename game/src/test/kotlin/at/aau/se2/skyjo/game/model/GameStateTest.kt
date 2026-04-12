package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameStateTest {

    object TestData{ //erstellt Players mit Boards um Korrekt testen zu können
        private var cardIdCounter = 0

        fun createCard(id: Int = cardIdCounter++, value: Int = 5): SkyjoCard {return SkyjoCard(id = id, value = value) }
        fun createBoard(fillWith: (BoardPosition) -> BoardSlot = {_ -> BoardSlot.Occupied(createCard(), faceUp = false)}) : PlayerBoard{
            val slots = BoardLayout.POSITIONS.associateWith{pos -> fillWith(pos)}
            return PlayerBoard(slots)
        }
        fun createPlayer(id: String = "p1") =PlayerState(id = id, board = createBoard())
        fun createDefaultState(playerCount: Int = 2): GameState{
            val players = (1..playerCount).map { createPlayer("p$it") }
            return GameState(players = players, phase = GamePhase.AWAITING_DRAW)
        }
    }


    @Test
    fun playerListIsEmpty() {
        val gameState = GameState(players = emptyList())

        assertNull(gameState.currentPlayerId)
    }

    @Test
    fun currentPlayerIdReturnsValidIndex(){
        val p1 = TestData.createPlayer("player-1")
        val p2 = TestData.createPlayer("player-2")
        val state = TestData.createDefaultState().copy(players = listOf(p1, p2), currentPlayerIndex = 1)

        assertEquals("player-2", state.currentPlayerId)
    }

    @Test
    fun currentPlayerReturnsValidPlayer(){
        val p1 = TestData.createPlayer("player-1")
        val p2 = TestData.createPlayer("player-2")
        val state = TestData.createDefaultState().copy(players = listOf(p1, p2), currentPlayerIndex = 0)
        val result = state.currentPlayer()

        assertEquals(p1, result)
        assertEquals("player-1", result.id)
    }

    @Test
    fun currentPlayerThrowsOutOfBoundsException(){
        val state = GameState(players = emptyList(), currentPlayerIndex = 0)

        assertThrows<IndexOutOfBoundsException> { state.currentPlayer() }
    }

    @Test
    fun gameStateChangesStateWithCopy(){
        val initialState = TestData.createDefaultState(playerCount = 2)
        val drawnCard = TestData.createCard(id = 99)
        val newState = initialState.copy(drawnCard = drawnCard, phase = GamePhase.AWAITING_REPLACEMENT)

        assertNull(initialState.drawnCard)
        assertEquals(drawnCard, newState.drawnCard)
        assertEquals(99, newState.drawnCard?.id)
        assertNotEquals(initialState.phase, newState.phase)
    }

}