package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class PlayerBoardTest {
    private val pos00 = BoardPosition(0, 0) //standart Testposition und Karten
    private val card5 = SkyjoCard(100, 5)


    private fun createTestBoard(defaultValue: Int = 0, faceUp: Boolean = false): PlayerBoard { //erstellt vollständiges Board
        val slots = BoardLayout.POSITIONS.associateWith{pos ->
            val uniqueId = pos.row * BoardLayout.COLUMNS + pos.column
            BoardSlot.Occupied(SkyjoCard(id = uniqueId, defaultValue), faceUp)
        }
        return PlayerBoard(slots)
    }

    @Test
    fun positionsDoNotMatchLayout(){
        val incompleteSlots = mapOf(pos00 to BoardSlot.Occupied(card5, false))
        val exception = assertThrows<IllegalArgumentException>{PlayerBoard(incompleteSlots)}

        assertEquals("board must define all positions exactly once", exception.message)
    }

    @Test
    fun innitValidationWorks(){
        assertThrows<IllegalArgumentException>{
            PlayerBoard(emptyMap())
        }
    }

    @Test
    fun slotAtValid(){
        val board = createTestBoard()
        val slot = board.slotAt(BoardPosition(0, 0))

        assertTrue(slot is BoardSlot.Occupied)
    }

    @Test
    fun hiddenPositionsAndHiddenCards(){
        val pos = BoardPosition(0, 0)
        val board = createTestBoard(faceUp = false)

        assertTrue(board.hasHiddenCards())
        assertEquals(12, board.hiddenPositions().size)

        val revealBoard = board.reveal(pos)
        assertEquals(11, revealBoard.hiddenPositions().size)

        val slotsWithCleared = BoardLayout.POSITIONS.associateWith{p ->
            if (p == pos) BoardSlot.Cleared
            else BoardSlot.Occupied(SkyjoCard(0,0), faceUp = true)
        }
        val boardClearedSlots = PlayerBoard(slotsWithCleared)
        assertEquals(0, boardClearedSlots.hiddenPositions().size) //alle Karten aufgedeckt oder nicht occupied
    }

    @Test
    fun revealLogicAndError(){
        val board = createTestBoard(faceUp = false)
        val pos = BoardPosition(1, 1)
        val updatedBoard = board.reveal(pos)
        val slotsWithCleared = BoardLayout.POSITIONS.associateWith{p ->
            if (p == pos) BoardSlot.Cleared
            else BoardSlot.Occupied(SkyjoCard(0,0), faceUp = false)
        }
        val boardClearedSlots = PlayerBoard(slotsWithCleared)
        val exception = assertThrows<IllegalArgumentException>{boardClearedSlots.reveal(pos)}

        assertTrue((updatedBoard.slotAt(pos) as BoardSlot.Occupied).faceUp) //Karte sollte offen sein
        assertThrows<IllegalArgumentException>{updatedBoard.reveal(pos)} //aufdeken sollte nicht mehr möglich sein, da die Karte schon offen ist
        assertThrows<IllegalArgumentException>{boardClearedSlots.reveal(pos)} //aufdecken sollte nict möglich sein, da Slot nicht Occupied ist
        assertEquals("cannot reveal a cleared slot", exception.message)
    }

    @Test
    fun replaceLogicAndError(){
        val pos = BoardPosition(2, 2)
        val oldCard = SkyjoCard(1, 10)
        val newCard = SkyjoCard(2, -2)
        val slots = BoardLayout.POSITIONS.associateWith{
            if (it == pos) BoardSlot.Occupied(oldCard, false)
            else BoardSlot.Occupied(SkyjoCard(it.row * 10, 0), false)
        }
        val board = PlayerBoard(slots)
        val result = board.replace(pos, newCard)

        assertEquals(oldCard, result.replacedCard)
        val newSlot = result.board.slotAt(pos) as BoardSlot.Occupied
        assertEquals(newCard, newSlot.card)
        assertTrue(newSlot.faceUp, "Replaced cards must be face up")

        val slotsWithCleared = BoardLayout.POSITIONS.associateWith{p ->
            if (p == pos) BoardSlot.Cleared
            else BoardSlot.Occupied(SkyjoCard(0,0), faceUp = false)
        }
        val boardClearedSlots = PlayerBoard(slotsWithCleared)
        val exception  = assertThrows<IllegalArgumentException>{boardClearedSlots.replace(pos, newCard)}
        assertThrows<IllegalArgumentException>{boardClearedSlots.replace(pos, newCard)}
        assertEquals("cannot replace a cleared slot", exception.message)
    }

    @Test
    fun fullyRevealValid(){
        val hiddenPos = BoardPosition(0, 0)
        val openPos = BoardPosition(1, 0)
        val card1 = SkyjoCard(1, 5)
        val card2 = SkyjoCard(2, 0)
        val slots = BoardLayout.POSITIONS.associateWith{p ->    //es gibt jeweils eine offene und eine geschlossene Karte
            when (p){
                hiddenPos -> BoardSlot.Occupied(card1, faceUp = false)
                openPos -> BoardSlot.Occupied(card2, faceUp = true)
                else -> BoardSlot.Cleared
            }
        }
        val board = PlayerBoard(slots)
        val revealedBoard = board.fullyReveal()
        val slotHidden = revealedBoard.slotAt(hiddenPos) as BoardSlot.Occupied
        val slotOpen = revealedBoard.slotAt(openPos) as BoardSlot.Occupied

        assertTrue(slotHidden.faceUp)
        assertEquals(5, slotHidden.card.value)
        assertTrue(slotOpen.faceUp)
        assertEquals(0, slotOpen.card.value)
        assertFalse(revealedBoard.hasHiddenCards()) //testet ob alles offen ist
    }

    @Test
    fun rawScoreValid(){
        val pos = BoardPosition(0, 0)
        val board = createTestBoard(defaultValue = 5)
        val slotsWithCleared = BoardLayout.POSITIONS.associateWith{p ->
            if (p == pos) BoardSlot.Cleared
            else BoardSlot.Occupied(SkyjoCard(0, 5), faceUp = true)
        }
        val boardClearedSlots = PlayerBoard(slotsWithCleared)

        assertEquals(60, board.rawScore()) //12 Karten *5 sollte 60 sein
        assertEquals(55, boardClearedSlots.rawScore()) //eine karte ist weniger, wert 0
    }

    @Test
    fun visibleValueSumValid(){
        val pos = BoardPosition(2, 2)
        val pos1 = BoardPosition(1, 2)
        val board = createTestBoard(defaultValue = 10, faceUp = true)
        val hiddenBoard = createTestBoard(faceUp = false)
        val slotsWithCleared = BoardLayout.POSITIONS.associateWith{p ->
            if (p == pos) BoardSlot.Cleared
            else BoardSlot.Occupied(SkyjoCard(0,0), faceUp = true)
        }
        val boardClearedSlots = PlayerBoard(slotsWithCleared)

        assertEquals(20, board.visibleValueSum(setOf(pos, pos1)))
        assertThrows<IllegalArgumentException>{hiddenBoard.visibleValueSum(setOf(pos, pos1))}
        assertThrows<IllegalArgumentException>{boardClearedSlots.visibleValueSum(setOf(pos, pos1))}
    }

    @Test
    fun clearCompletedLinesRemovesColumn(){
        val targetColumn = 0
        val slots = BoardLayout.POSITIONS.associateWith{p ->
            val isMatch = p.column == targetColumn
            BoardSlot.Occupied(SkyjoCard(p.row * 10 + p.column, if(isMatch) 1 else 2), faceUp = isMatch)
        }
        val board = PlayerBoard(slots)
        val result = board.clearCompletedLines()

        assertEquals(3, result.removedCards.size)
        assertTrue(result.removedCards.all{it.value == 1})
        BoardLayout.VERTICAL_LINES[targetColumn].forEach{p ->
            assertTrue(result.board.slotAt(p) is BoardSlot.Cleared)
        }
    }

    @Test
    fun clearCompletedLinesButNoMatches(){
        val targetColumn = 1
        val slots1 = BoardLayout.POSITIONS.associateWith{p ->
            val isMatch = p.column == targetColumn
            val isFaceUp = isMatch && p.row != 1
            BoardSlot.Occupied(SkyjoCard(p.row * 10 + p.column, 1), faceUp = isFaceUp)
        }
        val board1 = PlayerBoard(slots1)
        val result1 = board1.clearCompletedLines()

        assertTrue(result1.removedCards.isEmpty())
        assertEquals(board1, result1.board)

        val slots2 = BoardLayout.POSITIONS.associateWith{p ->
            BoardSlot.Occupied(SkyjoCard(p.row * 10 + p.column, p.row + p.column), faceUp = true)
        }
        val board2 = PlayerBoard(slots2)
        val result2 = board2.clearCompletedLines()

        assertTrue(result2.removedCards.isEmpty())
        assertEquals(board2, result2.board)

        val slots3 = BoardLayout.POSITIONS.associateWith{BoardSlot.Cleared}
        val board3 = PlayerBoard(slots3)
        val result3 = board3.clearCompletedLines()
        assertTrue(result3.removedCards.isEmpty())
        assertEquals(0, result3.board.slots.values.filterIsInstance<BoardSlot.Occupied>().size)
    }

    @Test
    fun fromCardsExpectsTwoOpenCards(){
        val cards = List(12){SkyjoCard(it, it)}
        val revealed = setOf(BoardPosition(0, 0), BoardPosition(1, 0))
        val board = PlayerBoard.fromCards(cards, revealed)

        assertTrue((board.slotAt(BoardPosition(0,0)) as BoardSlot.Occupied).faceUp)
        assertTrue((board.slotAt(BoardPosition(1,0)) as BoardSlot.Occupied).faceUp)
        assertFalse((board.slotAt(BoardPosition(1,1)) as BoardSlot.Occupied).faceUp)
    }

    @Test
    fun fromCardsExceptions(){
        val elevenCards = List(11){SkyjoCard(it, it)}
        val validRevealed = setOf(BoardPosition(0, 0), BoardPosition(1, 0))
        val exception = assertThrows<IllegalArgumentException>{
            PlayerBoard.fromCards(elevenCards, validRevealed)
        }
        assertEquals("a player board requires exactly 12 cards", exception.message)

        val twelveCards = List(12){SkyjoCard(it, it)}
        val onePos = setOf(BoardPosition(0, 0))
        val threePos = setOf(BoardPosition(1, 0), BoardPosition(0, 1), BoardPosition(1, 1))
        val ex1 = assertThrows<IllegalArgumentException>{
            PlayerBoard.fromCards(twelveCards, onePos)
        }
        val ex2 = assertThrows<IllegalArgumentException>{
            PlayerBoard.fromCards(twelveCards, threePos)
        }

        assertEquals("exactly two initial reveal positions are required", ex1.message)
        assertEquals("exactly two initial reveal positions are required", ex2.message)

    }
}