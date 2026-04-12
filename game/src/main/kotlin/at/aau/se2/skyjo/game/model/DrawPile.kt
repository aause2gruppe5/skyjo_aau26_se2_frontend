package at.aau.se2.skyjo.game.model

data class DrawPile(
    val cards: List<SkyjoCard>,
) {
    val size: Int
        get() = cards.size

    fun draw(): DrawResult {
        require(cards.isNotEmpty()) { "draw pile is empty" }
        return DrawResult(
            card = cards.last(),
            remainingPile = copy(cards = cards.dropLast(1)),
        )
    }

    companion object {
        fun empty(): DrawPile = DrawPile(emptyList())
    }
}

data class DrawResult(
    val card: SkyjoCard,
    val remainingPile: DrawPile,
)
