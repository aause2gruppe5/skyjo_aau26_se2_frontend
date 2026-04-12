package at.aau.se2.skyjo.game.model

data class DiscardPile(
    val cards: List<SkyjoCard>,
) {
    val size: Int
        get() = cards.size

    fun topCard(): SkyjoCard {
        require(cards.isNotEmpty()) { "discard pile is empty" }
        return cards.last()
    }

    fun takeTop(): DiscardDrawResult {
        require(cards.isNotEmpty()) { "discard pile is empty" }
        return DiscardDrawResult(
            card = cards.last(),
            remainingPile = copy(cards = cards.dropLast(1)),
        )
    }

    fun add(card: SkyjoCard): DiscardPile = copy(cards = cards + card)

    fun addAll(newCards: List<SkyjoCard>): DiscardPile = copy(cards = cards + newCards)

    companion object {
        fun empty(): DiscardPile = DiscardPile(emptyList())
    }
}

data class DiscardDrawResult(
    val card: SkyjoCard,
    val remainingPile: DiscardPile,
)
