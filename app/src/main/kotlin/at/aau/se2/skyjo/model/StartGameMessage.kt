package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class StartGameMessage(
    val maxRounds: Int = 3,
    val targetScore: Int = 100
)
