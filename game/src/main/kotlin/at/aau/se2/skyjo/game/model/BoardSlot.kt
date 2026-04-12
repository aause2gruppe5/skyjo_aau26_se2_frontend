package at.aau.se2.skyjo.game.model

sealed interface BoardSlot {
    data class Occupied(
        val card: SkyjoCard,
        val faceUp: Boolean,
    ) : BoardSlot

    data object Cleared : BoardSlot
}
