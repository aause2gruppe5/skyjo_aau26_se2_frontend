package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class GameAction(
    val type: String,
    val source: String? = null,
    val row: Int? = null,
    val col: Int? = null,
    val actionCardIndex: Int? = null,
    val targetPlayer1Id: String? = null,
    val targetPlayer1Row: Int? = null,
    val targetPlayer1Col: Int? = null,
    val targetPlayer2Id: String? = null,
    val targetPlayer2Row: Int? = null,
    val targetPlayer2Col: Int? = null,
)
