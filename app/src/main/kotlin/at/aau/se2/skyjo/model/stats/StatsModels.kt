package at.aau.se2.skyjo.model.stats

import kotlinx.serialization.Serializable

@Serializable
data class PlayerStatsDto(
    val userId: String,
    val username: String,
    val gamesPlayed: Int,
    val wins: Int,
    val totalScore: Int,
    val bestScore: Int? = null,
    val averageScore: Double,
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val userId: String,
    val username: String,
    val averageScore: Double,
    val wins: Int,
    val gamesPlayed: Int,
    val bestScore: Int? = null,
    val totalScore: Int,
)
