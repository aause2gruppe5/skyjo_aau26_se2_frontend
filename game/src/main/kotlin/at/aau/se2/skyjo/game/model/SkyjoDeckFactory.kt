package at.aau.se2.skyjo.game.model

import kotlin.random.Random

object SkyjoDeckFactory {
    private val cardDistribution: List<Int> = buildList {
        addAll(List(5) { -2 })
        addAll(List(10) { -1 })
        addAll(List(15) { 0 })
        for (value in 1..12) {
            addAll(List(10) { value })
        }
    }

    fun createShuffledDrawPile(seed: Long? = null): DrawPile {
        val random = seed?.let(::Random) ?: Random.Default
        val cards = cardDistribution
            .mapIndexed { index, value -> SkyjoCard(id = index + 1, value = value) }
            .shuffled(random)

        return DrawPile(cards)
    }
}
