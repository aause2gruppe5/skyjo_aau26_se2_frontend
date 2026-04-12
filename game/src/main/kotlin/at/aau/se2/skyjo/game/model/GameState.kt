package at.aau.se2.skyjo.game.model

data class GameState(
    val players: List<PlayerState> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val drawPile: DrawPile = DrawPile.empty(),
    val discardPile: DiscardPile = DiscardPile.empty(),
    val phase: GamePhase = GamePhase.NOT_STARTED,
    val drawnCard: SkyjoCard? = null,
    val drawSource: DrawSource? = null,
    val finisherPlayerId: String? = null,
    val finalTurnsRemaining: Int = 0,
    val roundResult: RoundResult? = null,
    val shuffleSeed: Long? = null,
    val shuffleCount: Int = 0,
) {
    val currentPlayerId: String?
        get() = players.getOrNull(currentPlayerIndex)?.id

    fun currentPlayer(): PlayerState = players[currentPlayerIndex]
}
