package at.aau.se2.skyjo.game.model

data class RoundResult(
    val finisherPlayerId: String,
    val scores: List<PlayerRoundScore>,
) {
    data class PlayerRoundScore(
        val playerId: String,
        val rawScore: Int,
        val finalScore: Int,
    )
}
