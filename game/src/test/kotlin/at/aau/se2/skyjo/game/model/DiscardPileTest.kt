package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DiscardPileTest {

    @Test
    fun emptyPile(){
        val pile = DiscardPile.empty()
        assertEquals(0, pile.size)
        assertTrue(pile.cards.isEmpty())
    }

    @Test
    fun topCardValid(){
        val card1 = SkyjoCard(1, -2)
        val card2 = SkyjoCard(2, -2)
        val pile = DiscardPile(listOf(card1, card2))
        val top = pile.topCard()

        assertEquals(card2, top)
        assertEquals(2, pile.size)
    }

    @Test
    fun topCardInvalid(){ //weil der Stack leer ist
        val emptyPile = DiscardPile.empty()
        val exception = assertThrows<IllegalArgumentException> { emptyPile.topCard() }

        assertEquals("discard pile is empty", exception.message)
    }

    @Test
    fun takeTopValid(){
        val card1 = SkyjoCard(1, -2)
        val card2 = SkyjoCard(2, -2)
        val pile = DiscardPile(listOf(card1, card2))
        val top = pile.takeTop()

        assertEquals(card2, top.card)
        assertEquals(1, top.remainingPile.size)
        assertEquals(card1, top.remainingPile.cards.first())
    }

    @Test
    fun takeTopInvalid(){
        val emptyPile = DiscardPile.empty()
        val exception = assertThrows<IllegalArgumentException> { emptyPile.takeTop() }

        assertEquals("discard pile is empty", exception.message)
    }

    @Test
    fun addCard(){
        val card = SkyjoCard(1, -2)
        val pile = DiscardPile(listOf(card))
        val newCard = SkyjoCard(2, -2)
        val newPile = pile.add(newCard)

        assertEquals(1, pile.size)
        assertEquals(2, newPile.size)
        assertEquals(newCard, newPile.topCard())
    }

    @Test
    fun addSeveralCards(){
        val initialCard = SkyjoCard(1, -2)
        val pile = DiscardPile(listOf(initialCard))
        val newCards = listOf(SkyjoCard(2, 6), SkyjoCard(3, 12))
        val newPile = pile.addAll(newCards)

        assertEquals(3, newPile.size)
        assertEquals(3, newPile.topCard().id)
        assertEquals(listOf(1,2,3), newPile.cards.map{it.id})
    }

}