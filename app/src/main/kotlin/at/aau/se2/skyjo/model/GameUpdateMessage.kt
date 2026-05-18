package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Int,
    val value: Int,
    val type: String
)

@Serializable
data class ActionCard(
    val id: Int,
    val kind: String,
    val label: String = kind,
    val value: Int = 10,
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
    val board: List<List<BoardSlot>>,
    val actionCards: List<ActionCard> = emptyList(),
)

@Serializable
data class TotalScore(
    val playerId: String,
    val nickname: String,
    val totalScore: Int
)

@Serializable
data class PlayerRoundScore(
    val playerId: String,
    val rawScore: Int,
    val finalScore: Int,
)

@Serializable
data class RoundResult(
    val finisherPlayerId: String,
    val scores: List<PlayerRoundScore>,
)

@Serializable
data class GameUpdateMessage(
    val phase: String,
    val currentPlayerId: String?,
    val roundNumber: Int,
    val gameOver: Boolean,
    val totalScores: List<TotalScore>,
    val players: List<GamePlayerState>,
    val discardTopCard: Card? = null,
    val drawnCard: Card? = null,
    val visibleActionCards: List<ActionCard> = emptyList(),
    val actionDrawPileCount: Int = 0,
    val roundResult: RoundResult? = null,
    val gameId: String? = null,
    val lobbyId: String? = null,
    val disconnectedPlayers: List<String> = emptyList(),
)
