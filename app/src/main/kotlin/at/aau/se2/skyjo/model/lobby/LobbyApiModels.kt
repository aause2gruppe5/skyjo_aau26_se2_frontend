package at.aau.se2.skyjo.model.lobby

import at.aau.se2.skyjo.model.LobbyPlayer
import kotlinx.serialization.Serializable

@Serializable
data class LobbySummaryResponse(
    val lobbyId: String,
    val joinCode: String,
    val players: List<LobbyPlayer>,
    val status: String,
    val maxPlayers: Int,
)
