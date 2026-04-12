package at.aau.se2.skyjo.game.model

data class PlayerBoard(
    val slots: Map<BoardPosition, BoardSlot>,
) {
    init {
        require(slots.keys == BoardLayout.POSITIONS.toSet()) { "board must define all positions exactly once" }
    }

    fun slotAt(position: BoardPosition): BoardSlot = slots.getValue(position)

    fun hiddenPositions(): List<BoardPosition> =
        BoardLayout.POSITIONS.filter { position ->
            val slot = slotAt(position)
            slot is BoardSlot.Occupied && !slot.faceUp
        }

    fun hasHiddenCards(): Boolean = hiddenPositions().isNotEmpty()

    fun reveal(position: BoardPosition): PlayerBoard {
        val slot = slotAt(position)
        require(slot is BoardSlot.Occupied) { "cannot reveal a cleared slot" }
        require(!slot.faceUp) { "card at $position is already face up" }

        return copy(
            slots = slots + (position to slot.copy(faceUp = true)),
        )
    }

    fun replace(position: BoardPosition, card: SkyjoCard): ReplacementResult {
        val slot = slotAt(position)
        require(slot is BoardSlot.Occupied) { "cannot replace a cleared slot" }

        val updatedBoard = copy(
            slots = slots + (position to BoardSlot.Occupied(card = card, faceUp = true)),
        )
        return ReplacementResult(
            board = updatedBoard,
            replacedCard = slot.card,
        )
    }

    fun fullyReveal(): PlayerBoard {
        val updatedSlots = BoardLayout.POSITIONS.associateWith { position ->
            when (val slot = slotAt(position)) {
                is BoardSlot.Cleared -> slot
                is BoardSlot.Occupied -> slot.copy(faceUp = true)
            }
        }
        return copy(slots = updatedSlots)
    }

    fun clearCompletedLines(): BoardCleanupResult {
        val matchedPositions = (BoardLayout.VERTICAL_LINES + BoardLayout.HORIZONTAL_LINES)
            .filter(::isCompletedMatchingLine)
            .flatten()
            .distinct()

        if (matchedPositions.isEmpty()) {
            return BoardCleanupResult(board = this, removedCards = emptyList())
        }

        val removedCards = matchedPositions
            .sortedWith(compareBy(BoardPosition::row, BoardPosition::column))
            .map { position -> (slotAt(position) as BoardSlot.Occupied).card }

        val updatedSlots = slots.toMutableMap()
        matchedPositions.forEach { position ->
            updatedSlots[position] = BoardSlot.Cleared
        }

        return BoardCleanupResult(
            board = copy(slots = updatedSlots),
            removedCards = removedCards,
        )
    }

    fun rawScore(): Int =
        BoardLayout.POSITIONS.sumOf { position ->
            when (val slot = slotAt(position)) {
                is BoardSlot.Cleared -> 0
                is BoardSlot.Occupied -> slot.card.value
            }
        }

    fun visibleValueSum(positions: Set<BoardPosition>): Int =
        positions.sumOf { position ->
            val slot = slotAt(position)
            require(slot is BoardSlot.Occupied && slot.faceUp) { "position $position must contain a face-up card" }
            slot.card.value
        }

    private fun isCompletedMatchingLine(line: List<BoardPosition>): Boolean {
        val occupiedSlots = line.map { position -> slotAt(position) as? BoardSlot.Occupied ?: return false }
        if (occupiedSlots.any { !it.faceUp }) {
            return false
        }

        return occupiedSlots.map { it.card.value }.distinct().size == 1
    }

    companion object {
        fun fromCards(cards: List<SkyjoCard>, revealedPositions: Set<BoardPosition>): PlayerBoard {
            require(cards.size == BoardLayout.POSITIONS.size) { "a player board requires exactly ${BoardLayout.POSITIONS.size} cards" }
            require(revealedPositions.size == 2) { "exactly two initial reveal positions are required" }

            val slots = BoardLayout.POSITIONS.mapIndexed { index, position ->
                position to BoardSlot.Occupied(
                    card = cards[index],
                    faceUp = position in revealedPositions,
                )
            }.toMap()

            return PlayerBoard(slots)
        }
    }
}

data class ReplacementResult(
    val board: PlayerBoard,
    val replacedCard: SkyjoCard,
)

data class BoardCleanupResult(
    val board: PlayerBoard,
    val removedCards: List<SkyjoCard>,
)
