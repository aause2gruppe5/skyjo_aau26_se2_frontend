package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class GameAction(
    val type: String,
    val source: String? = null,
    val row: Int? = null,
    val col: Int? = null
)
