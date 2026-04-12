package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.*

class DrawPileTest {

    @Test
    fun emptyPile() {
        val pile = DrawPile.empty()
        assertEquals(0, pile.size)
        assertTrue(pile.cards.isEmpty())
    }

    @Test
    fun drawValid(){
        val card1 = SkyjoCard(1, 2)
        val card2 = SkyjoCard(2, 10)
        val pile = DrawPile(listOf(card1, card2))
        val result = pile.draw()

        assertEquals(card2, result.card)
        assertEquals(1, result.remainingPile.size)
        assertEquals(card1, result.remainingPile.cards.first())
    }

    @Test
    fun drawInvalid(){ //Stapel ist leer
        val emptyPile = DrawPile.empty()
        val exception = assertThrows<IllegalArgumentException>{emptyPile.draw()}

        assertEquals("draw pile is empty", exception.message)
    }
}