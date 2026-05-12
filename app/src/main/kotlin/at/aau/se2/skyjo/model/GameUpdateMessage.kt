package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Int,
    val value: Int,
    val type: String
)

@Serializable
data class BoardSlot(
    val type: String,
    val faceUp: Boolean? = null,
    val card: Card? = null
)

@Serializable
data class GamePlayerState(
    val playerId: String,
    val nickname: String,
    val board: List<List<BoardSlot>>
)

@Serializable
data class TotalScore(
    val playerId: String,
    val nickname: String,
    val totalScore: Int
)

@Serializable
data class RoundPlayerScore(
    val playerId: String,
    val rawScore: Int,
    val finalScore: Int,
)

@Serializable
data class RoundResult(
    val finisherPlayerId: String,
    val scores: List<RoundPlayerScore>,
)

@Serializable
data class GameUpdateMessage(
    val phase: String,
    val currentPlayerId: String,
    val roundNumber: Int,
    val gameOver: Boolean,
    val totalScores: List<TotalScore>,
    val players: List<GamePlayerState>,
    val discardTopCard: Card? = null,
    val drawnCard: Card? = null,
    val roundResult: RoundResult? = null,
    val gameId: String? = null,
    val disconnectedPlayers: List<String> = emptyList(),
)
