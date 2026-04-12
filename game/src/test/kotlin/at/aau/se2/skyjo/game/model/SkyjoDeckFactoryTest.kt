package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SkyjoDeckFactoryTest {
    @Test
    fun cardDistributionMakes150Cards(){
        val drawPile = SkyjoDeckFactory.createShuffledDrawPile()

        assertEquals(150, drawPile.cards.size)
    }

    @Test
    fun cardDistributionIsCorrect(){
        val drawPile = SkyjoDeckFactory.createShuffledDrawPile()
        val cards = drawPile.cards
        val counts = cards.groupingBy { it.value }.eachCount()

        assertEquals(5, counts[-2])
        assertEquals(10, counts[-1])
        assertEquals(15, counts[0])
        for (value in 1..12){assertEquals(10, counts[value])}
    }

    @Test
    fun cardsHaveUniqueId(){
        val drawPile = SkyjoDeckFactory.createShuffledDrawPile()
        val ids = drawPile.cards.map{it.id}

        assertEquals(150, ids.toSet().size) //toSet entfehrnt Duplikate
        assertTrue(ids.all{it in 1..150})
    }

    @Test
    fun shuffleWithSameSeed(){
        val seed = 42L
        val pile1 = SkyjoDeckFactory.createShuffledDrawPile(seed)
        val pile2 = SkyjoDeckFactory.createShuffledDrawPile(seed)

        assertEquals(pile1.cards, pile2.cards)
    }

    @Test
    fun shuffleWithDifferentSeed(){
        val pile1 = SkyjoDeckFactory.createShuffledDrawPile(123L)
        val pile2 = SkyjoDeckFactory.createShuffledDrawPile(456L)

        assertNotEquals(pile1.cards, pile2.cards)
    }

    @Test
    fun shuffleWithoutSeed(){
        val pile1 = SkyjoDeckFactory.createShuffledDrawPile()
        val pile2 = SkyjoDeckFactory.createShuffledDrawPile()

        assertNotEquals(pile1.cards, pile2.cards)
    }
}