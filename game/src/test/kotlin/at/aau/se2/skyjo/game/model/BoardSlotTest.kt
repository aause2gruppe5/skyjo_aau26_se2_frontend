package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BoardSlotTest {

    @Test
    fun occupiedAndFaceUpTrue(){
        val card = SkyjoCard(101, 12) //dummyKarte
        val slot = BoardSlot.Occupied(card, true)

        assertEquals(card, slot.card)
        assertTrue(slot.faceUp)
    }

    @Test
    fun occupiedAndFaceUpFalse(){
        val card = SkyjoCard(102, -2)
        val slot = BoardSlot.Occupied(card, false)

        assertEquals(card, slot.card)
        assertFalse(slot.faceUp)
    }

    @Test
    fun occupiedSlotsDifferentIdSameValue(){
        val card1 = SkyjoCard(10, 1)
        val card2 = SkyjoCard(11, 1)

        val slot1 = BoardSlot.Occupied(card1, true)
        val slot2 = BoardSlot.Occupied(card2, true)
        assertEquals(card1, slot1.card)
        assertEquals(card2, slot2.card)
        assertNotEquals(slot1, slot2)
    }

    @Test
    fun occupiedSlotCardChangesFaceUp(){
        val card1 = SkyjoCard(10, 1)
        val stateBeforeTurn = BoardSlot.Occupied(card1, false)
        val stateAfterTurn = BoardSlot.Occupied(card1, true)

        assertNotEquals(stateBeforeTurn, stateAfterTurn)
    }

    @Test
    fun clearedIsASingleton(){
        val slot1 = BoardSlot.Cleared
        val slot2 = BoardSlot.Cleared

        assertEquals(slot1, slot2) //prüft auf selbe Instanz
        assertSame(slot1, slot2) //prüft auf selben Speicher

    }

}